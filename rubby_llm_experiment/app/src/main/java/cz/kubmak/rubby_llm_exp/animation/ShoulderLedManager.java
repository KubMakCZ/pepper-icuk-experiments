package cz.kubmak.rubby_llm_exp.animation;

import android.content.Context;

import com.aldebaran.qi.AnyObject;
import com.aldebaran.qi.Session;
import com.aldebaran.qi.UserTokenAuthenticator;
import com.aldebaran.qi.sdk.QiContext;
import com.aldebaran.qi.sdk.core.SessionManager;
import com.aldebaran.robotservice.IRobotService;

import java.lang.reflect.Field;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import cz.kubmak.rubby_llm_exp.conversation.ConversationState;
import cz.kubmak.rubby_llm_exp.util.FileLogger;

/**
 * Ovládání RGB LED diod na ramenech Peppera ("ChestLeds") a v očích ("FaceLeds")
 * přes nativní NAOqi službu ALLeds získanou z QiContext Session.
 *
 * Barvy ramen podle stavu:
 * - IDLE:       klidná azurově modrá (0.0, 0.55, 1.0)
 * - LISTENING:  jasně zelená (0.0, 1.0, 0.15)
 * - PROCESSING: pulzující oranžovo-žlutá (1.0, 0.45, 0.0)
 * - SPEAKING:   jasně fialovo-modrá (0.55, 0.15, 1.0)
 * - DISCO:      rychlé náhodné střídání neonových barev na ramenech i v očích (při tanci/discu)
 */
public class ShoulderLedManager {

    private static final String TAG = "ShoulderLedManager";

    // Syté neonové barvy pro diskotékový režim (R, G, B v rozsahu 0.0 - 1.0)
    private static final float[][] DISCO_COLORS = {
        {1.0f, 0.0f, 0.75f}, // Neonově růžová / magenta
        {0.0f, 0.95f, 1.0f}, // Elektrická azurová
        {0.0f, 1.0f, 0.1f},  // Laserově zelená
        {1.0f, 0.85f, 0.0f}, // Zlatě žlutá
        {1.0f, 0.05f, 0.0f}, // Ohnivě červená
        {0.65f, 0.0f, 1.0f}, // Ultrafialová
        {0.0f, 0.35f, 1.0f}  // Sytě modrá
    };

    private final FileLogger fileLogger;
    private final ExecutorService ledExecutor = Executors.newSingleThreadExecutor();
    private final Random random = new Random();

    private volatile AnyObject alLedsService;
    private Session privateSession;
    private SessionManager privateSessionManager;
    private Context appContext;
    private final AtomicBoolean discoActive = new AtomicBoolean(false);
    private final AtomicBoolean thinkingPulseActive = new AtomicBoolean(false);
    private Thread discoThread;
    private Thread thinkingThread;
    private volatile ConversationState lastState = ConversationState.IDLE;

    public ShoulderLedManager(FileLogger fileLogger) {
        this.fileLogger = fileLogger;
    }

    /**
     * Inicializuje přístup k NAOqi službě ALLeds přes privátní NAOqi Session (usePrivateDirectory = true).
     */
    public void init(final QiContext qiContext) {
        if (qiContext == null) return;
        this.appContext = qiContext.getApplicationContext();

        ledExecutor.execute(() -> {
            try {
                // 1) Nejrychlejší cesta: získat IRobotService ze stávajícího SessionManageru v QiContextu
                // a připojit se na PrivateEndpoint (kde běží plný NAOqi ServiceDirectory včetně ALLeds)
                IRobotService robotService = extractRobotService(qiContext);
                if (robotService != null) {
                    String privateEndpoint = robotService.getPrivateEndpoint();
                    String privateToken = robotService.getPrivateToken();
                    fileLogger.i(TAG, "Pripojuji ALLeds pres NAOqi PrivateEndpoint: " + privateEndpoint);
                    if (privateSession != null && privateSession.isConnected()) {
                        privateSession.close();
                    }
                    Session s = new Session();
                    if (privateToken != null && !privateToken.isEmpty()) {
                        s.setClientAuthenticator(new UserTokenAuthenticator("tablet", privateToken));
                    }
                    s.connect(privateEndpoint).get();
                    privateSession = s;
                    alLedsService = s.service("ALLeds").get();
                    fileLogger.i(TAG, "ALLeds sluzba uspesne pripojena! Ovladani RGB ramen je aktivni.");
                    applyStateColor(lastState);
                    return;
                }

                // 2) Záložní cesta přes SessionManager(true)
                if (privateSessionManager == null && appContext != null) {
                    privateSessionManager = new SessionManager(true);
                    privateSessionManager.register(appContext, new SessionManager.Callback() {
                        @Override
                        public void onRobotReady(Session session) {
                            ledExecutor.execute(() -> {
                                try {
                                    alLedsService = session.service("ALLeds").get();
                                    fileLogger.i(TAG, "ALLeds sluzba pripojena pres SessionManager(true)!");
                                    applyStateColor(lastState);
                                } catch (Exception e) {
                                    fileLogger.w(TAG, "ALLeds pres SessionManager(true) selhalo: " + e.getMessage());
                                }
                            });
                        }

                        @Override
                        public void onRobotLost() {
                            alLedsService = null;
                        }

                        @Override
                        public void onRobotAbsent() {
                            alLedsService = null;
                        }
                    });
                }
            } catch (Exception e) {
                fileLogger.w(TAG, "Inicializace ALLeds selhala: " + e.getMessage());
            }
        });
    }

    /**
     * Uvolní referenci při ztrátě Robot Focusu.
     */
    public void release() {
        stopDisco();
        stopThinkingPulse();
        alLedsService = null;
    }

    public void shutdown() {
        release();
        ledExecutor.shutdownNow();
    }

    /**
     * Zjistí, zda daná kategorie animace představuje tanec / disco / hudební show.
     */
    public static boolean isDiscoCategory(String category) {
        if (category == null) return false;
        String norm = category.trim().toLowerCase();
        return "disco".equals(norm)
                || "dance".equals(norm)
                || "guitar".equals(norm)
                || "saxophone".equals(norm)
                || "celebration".equals(norm);
    }

    /**
     * Nastaví barvu ramen podle aktuálního stavu konverzace.
     */
    public void onStateChanged(final ConversationState state) {
        this.lastState = state;
        if (state != ConversationState.PROCESSING) {
            stopThinkingPulse();
        }
        // Pokud zrovna běží disco efekt během SPEAKING, nepřepisujeme ho statickou barvou
        if (discoActive.get() && state == ConversationState.SPEAKING) {
            return;
        }
        if (state != ConversationState.SPEAKING && discoActive.get()) {
            stopDisco();
        }
        applyStateColor(state);
    }

    private void applyStateColor(final ConversationState state) {
        if (alLedsService == null) return;

        if (state == ConversationState.PROCESSING) {
            startThinkingPulse();
            return;
        }

        ledExecutor.execute(() -> {
            try {
                AnyObject leds = alLedsService;
                if (leds == null) return;
                switch (state) {
                    case IDLE:
                        // Klidná azurově modrá na ramenech
                        fadeChestRgb(leds, 0.0f, 0.55f, 1.0f, 0.25f);
                        break;
                    case LISTENING:
                        // Jasně zelená (poslouchám)
                        fadeChestRgb(leds, 0.0f, 1.0f, 0.15f, 0.12f);
                        break;
                    case SPEAKING:
                        // Jasně fialovo-modrá (mluvím / předvádím)
                        fadeChestRgb(leds, 0.55f, 0.15f, 1.0f, 0.15f);
                        break;
                    default:
                        break;
                }
            } catch (Exception e) {
                fileLogger.w(TAG, "Chyba pri zmene LED stavu: " + e.getMessage());
            }
        });
    }

    /**
     * Spustí náhodné diskotékové blikání ramen (a očí) během tance nebo disco animace.
     */
    public void startDisco() {
        stopThinkingPulse();
        if (discoActive.getAndSet(true)) {
            return; // Už běží
        }
        fileLogger.i(TAG, "Spoustim DISCO svetelny rezim na ramenech!");
        discoThread = new Thread(() -> {
            int lastColorIdx = -1;
            while (discoActive.get() && !Thread.currentThread().isInterrupted()) {
                try {
                    AnyObject leds = alLedsService;
                    if (leds != null) {
                        int idx = random.nextInt(DISCO_COLORS.length);
                        if (idx == lastColorIdx) {
                            idx = (idx + 1) % DISCO_COLORS.length;
                        }
                        lastColorIdx = idx;
                        float[] c = DISCO_COLORS[idx];
                        // Ramena ("ChestLeds") blikají sytou náhodnou barvou
                        fadeChestRgb(leds, c[0], c[1], c[2], 0.08f);

                        // Druhá náhodná barva pro oči ("FaceLeds") pro pravý disco efekt
                        float[] eyeColor = DISCO_COLORS[(idx + 2) % DISCO_COLORS.length];
                        leds.call(Void.class, "fadeRGB", "FaceLeds", eyeColor[0], eyeColor[1], eyeColor[2], 0.08f).get();
                    }
                    Thread.sleep(170);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    // Ignorujeme pripadne kratkodobe chyby komunikace
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }, "Rubby-DiscoLeds");
        discoThread.setDaemon(true);
        discoThread.start();
    }

    /**
     * Zastaví diskotékové blikání a vrátí oči i ramena do barvy aktuálního stavu.
     */
    public void stopDisco() {
        if (!discoActive.getAndSet(false)) {
            return;
        }
        if (discoThread != null) {
            discoThread.interrupt();
            discoThread = null;
        }
        ledExecutor.execute(() -> {
            try {
                AnyObject leds = alLedsService;
                if (leds != null) {
                    // Vrátíme oči do přirozené bílé barvy
                    leds.call(Void.class, "fadeRGB", "FaceLeds", 1.0f, 1.0f, 1.0f, 0.2f).get();
                }
            } catch (Exception ignored) {}
        });
        applyStateColor(lastState);
    }

    /**
     * Jemné pulzování oranžové a jantarové na ramenech, když Rubby přemýšlí (PROCESSING).
     */
    private void startThinkingPulse() {
        if (thinkingPulseActive.getAndSet(true)) {
            return;
        }
        thinkingThread = new Thread(() -> {
            boolean bright = true;
            while (thinkingPulseActive.get() && !Thread.currentThread().isInterrupted()) {
                try {
                    AnyObject leds = alLedsService;
                    if (leds != null) {
                        if (bright) {
                            fadeChestRgb(leds, 1.0f, 0.50f, 0.0f, 0.35f);
                        } else {
                            fadeChestRgb(leds, 0.35f, 0.12f, 0.0f, 0.35f);
                        }
                        bright = !bright;
                    }
                    Thread.sleep(400);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    break;
                }
            }
        }, "Rubby-ThinkingLeds");
        thinkingThread.setDaemon(true);
        thinkingThread.start();
    }

    private void stopThinkingPulse() {
        if (!thinkingPulseActive.getAndSet(false)) {
            return;
        }
        if (thinkingThread != null) {
            thinkingThread.interrupt();
            thinkingThread = null;
        }
    }

    private void fadeChestRgb(AnyObject leds, float r, float g, float b, float durationSec) throws Exception {
        leds.call(Void.class, "fadeRGB", "ChestLeds", r, g, b, durationSec).get();
    }

    /**
     * Pomocí reflexe získá IRobotService ze SessionManageru uvnitř QiContextu.
     */
    private IRobotService extractRobotService(QiContext qiContext) {
        if (qiContext == null) return null;
        try {
            Field focusManagerField = QiContext.class.getDeclaredField("focusManager");
            focusManagerField.setAccessible(true);
            Object focusManager = focusManagerField.get(qiContext);
            if (focusManager == null) return null;

            Field sessionManagerField = focusManager.getClass().getDeclaredField("sessionManager");
            sessionManagerField.setAccessible(true);
            SessionManager sessionManager = (SessionManager) sessionManagerField.get(focusManager);
            if (sessionManager == null) return null;

            return sessionManager.getRobotService();
        } catch (Exception e) {
            fileLogger.w(TAG, "Nepodarilo se extrahovat IRobotService z QiContextu: " + e.getMessage());
            return null;
        }
    }
}
