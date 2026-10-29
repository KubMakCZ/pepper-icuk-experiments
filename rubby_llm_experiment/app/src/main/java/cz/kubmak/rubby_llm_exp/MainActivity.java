package cz.kubmak.rubby_llm_exp;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.RecognitionListener;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;

import androidx.annotation.NonNull;
import com.aldebaran.qi.sdk.design.activity.RobotActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.aldebaran.qi.sdk.QiContext;
import com.aldebaran.qi.sdk.QiSDK;
import com.aldebaran.qi.Future;
import com.aldebaran.qi.sdk.RobotLifecycleCallbacks;
import com.aldebaran.qi.sdk.builder.AnimateBuilder;
import com.aldebaran.qi.sdk.builder.AnimationBuilder;
import com.aldebaran.qi.sdk.builder.GoToBuilder;
import com.aldebaran.qi.sdk.builder.HolderBuilder;
import com.aldebaran.qi.sdk.builder.SayBuilder;
import com.aldebaran.qi.sdk.builder.TransformBuilder;
import com.aldebaran.qi.sdk.object.actuation.Actuation;
import com.aldebaran.qi.sdk.object.actuation.Animate;
import com.aldebaran.qi.sdk.object.actuation.Animation;
import com.aldebaran.qi.sdk.object.actuation.Frame;
import com.aldebaran.qi.sdk.object.actuation.FreeFrame;
import com.aldebaran.qi.sdk.object.actuation.GoTo;
import com.aldebaran.qi.sdk.object.actuation.Mapping;
import com.aldebaran.qi.sdk.object.actuation.OrientationPolicy;
import com.aldebaran.qi.sdk.object.actuation.PathPlanningPolicy;
import com.aldebaran.qi.sdk.object.conversation.Say;
import com.aldebaran.qi.sdk.object.geometry.Transform;
import com.aldebaran.qi.sdk.object.holder.AutonomousAbilitiesType;
import com.aldebaran.qi.sdk.object.holder.Holder;
import com.aldebaran.qi.sdk.object.power.FlapSensor;
import com.aldebaran.qi.sdk.object.touch.Touch;
import com.aldebaran.qi.sdk.object.touch.TouchSensor;

import com.aldebaran.qi.sdk.object.conversation.Topic;
import com.aldebaran.qi.sdk.builder.TopicBuilder;
import com.aldebaran.qi.sdk.object.conversation.QiChatbot;
import com.aldebaran.qi.sdk.builder.QiChatbotBuilder;
import com.aldebaran.qi.sdk.object.conversation.Chat;
import com.aldebaran.qi.sdk.builder.ChatBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cz.kubmak.rubby_llm_exp.animation.AnimationManager;
import cz.kubmak.rubby_llm_exp.animation.ShoulderLedManager;
import cz.kubmak.rubby_llm_exp.conversation.ChatHistoryManager;
import cz.kubmak.rubby_llm_exp.remote.RemoteControlServer;
import cz.kubmak.rubby_llm_exp.util.FileLogger;
import cz.kubmak.rubby_llm_exp.conversation.ConversationState;
import cz.kubmak.rubby_llm_exp.llm.GeminiService;
import cz.kubmak.rubby_llm_exp.llm.OpenRouterService;
import cz.kubmak.rubby_llm_exp.llm.ILlmService;
import cz.kubmak.rubby_llm_exp.util.TextCleaner;

public class MainActivity extends RobotActivity implements RobotLifecycleCallbacks, RemoteControlServer.RemoteCommandHandler {

    private static final String TAG = "RubbyLLM";
    private static final int PERMISSION_REQUEST_AUDIO = 1001;

    private final Random random = new Random();
    private FileLogger fileLogger;

    // UI - Admin / Testovaci obrazovka
    private View adminContainer;
    private TextView tvStatus;
    private TextView tvChatLog;
    private EditText etInput;
    private Button btnSend;
    private Button btnMic;
    private Button btnClear;
    private Button btnAnimations;
    private Button btnSwitchToVisitor;
    private ScrollView scrollView;
    private Spinner spinnerLlm;
    private SeekBar seekBarVolume;
    private TextView tvVolumePercent;
    private TextView tvRemoteUrlAdmin;

    // UI - Navstevnicka / Prezentacni obrazovka
    private View visitorContainer;
    private Button btnSwitchToAdmin;
    private Button btnVisitorMic;
    private Button btnVisitorAnimations;
    private TextView tvVisitorStateTitle;
    private TextView tvVisitorStateSubtitle;

    // LLM
    private ILlmService llmService;
    private ILlmService geminiService;
    private ILlmService openRouterService;
    private ChatHistoryManager chatHistory;
    private ConversationState currentState = ConversationState.IDLE;

    // QiSDK - muze byt null pokud neni focus (virtualni robot, telefon)
    private QiContext qiContext;

    // QiSDK komponenty
    private TouchSensor headTouchSensor;
    private FlapSensor chargingFlapSensor;
    private volatile boolean chargingFlapOpen = false;
    private QiChatbot qiChatbot;
    private Chat wakeWordChat;
    private Future<Void> currentAnimateFuture;
    private Future<Void> currentGoToFuture;
    private Future<Void> currentSayFuture;
    private FreeFrame joystickTargetFrame;
    private GoTo joystickGoTo;
    private ShoulderLedManager shoulderLedManager;
    private Holder awarenessHolder;
    private volatile boolean faceTrackingEnabled = true;
    private volatile boolean narrowSpaceMode = false;

    // Vzdalene ovladani (Web + REST API na portu 8080)
    private RemoteControlServer remoteControlServer;
    private volatile String lastRubbyMessage = "";

    // Android TTS fallback (kdyz neni QiSDK nebo neni focus)
    private TextToSpeech androidTts;
    private boolean androidTtsReady = false;

    // Speech recognition
    private SpeechRecognizer speechRecognizer;

    // Threading (executor pro LLM a rec, moveExecutor pro nezavisly realtime pojezd)
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final ExecutorService moveExecutor = Executors.newSingleThreadExecutor();

    // Chat log text
    private final StringBuilder chatLogText = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Fullscreen immersive mode zakazan pro lepsi kompatibilitu s emulatorem
        // hideSystemUI();

        setContentView(R.layout.activity_main);
        fileLogger = FileLogger.getInstance(this);
        shoulderLedManager = new ShoulderLedManager(fileLogger);

        // Registrace QiSDK lifecycle - na ne-Pepper zarizeni se nic nestane
        try {
            QiSDK.register(this, this);
            fileLogger.i(TAG, "QiSDK registrace uspesna");
        } catch (Exception e) {
            fileLogger.w(TAG, "QiSDK neni k dispozici (telefon/emulator): " + e.getMessage());
        }

        // Inicializace UI
        initViews();

        // Inicializace LLM a Spinneru
        initLlmAndSpinner();

        chatHistory = new ChatHistoryManager();

        // Spusteni vestaveneho HTTP serveru pro vzdalene ovladani (port 8080)
        remoteControlServer = new RemoteControlServer(RemoteControlServer.DEFAULT_PORT, this);
        remoteControlServer.start();
        updateRemoteUrlLabels();
        String ip = RemoteControlServer.getLocalIpAddress();
        appendToChat("SYSTEM", "Dalkove ovladani (Web + API): http://" + ip + ":" + RemoteControlServer.DEFAULT_PORT);

        // Android TTS jako fallback pro ne-Pepper zarizeni
        androidTts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                Locale czLocale = new Locale("cs", "CZ");
                int result = androidTts.setLanguage(czLocale);
                
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    fileLogger.e(TAG, "Cestina pro Android TTS neni v tomto zarizeni/emulatoru podporovana.");
                    androidTts.setLanguage(Locale.US); // Zkus aspon anglictinu pro test
                }
                
                androidTtsReady = true;
                fileLogger.i(TAG, "Android TTS pripraven (Fallback)");
            } else {
                fileLogger.e(TAG, "Inicializace Android TTS selhala!");
            }
        });

        // Povoleni mikrofonu a inicializace
        checkPermissions();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            initSpeechRecognizer();
        }
    }

    private void initViews() {
        adminContainer = findViewById(R.id.adminContainer);
        tvStatus = findViewById(R.id.tvStatus);
        tvChatLog = findViewById(R.id.tvChatLog);
        etInput = findViewById(R.id.etInput);
        btnSend = findViewById(R.id.btnSend);
        btnMic = findViewById(R.id.btnMic);
        btnClear = findViewById(R.id.btnClear);
        btnAnimations = findViewById(R.id.btnAnimations);
        btnSwitchToVisitor = findViewById(R.id.btnSwitchToVisitor);
        scrollView = findViewById(R.id.scrollView);
        spinnerLlm = findViewById(R.id.spinnerLlm);
        seekBarVolume = findViewById(R.id.seekBarVolume);
        tvVolumePercent = findViewById(R.id.tvVolumePercent);
        tvRemoteUrlAdmin = findViewById(R.id.tvRemoteUrlAdmin);

        visitorContainer = findViewById(R.id.visitorContainer);
        btnSwitchToAdmin = findViewById(R.id.btnSwitchToAdmin);
        btnVisitorMic = findViewById(R.id.btnVisitorMic);
        btnVisitorAnimations = findViewById(R.id.btnVisitorAnimations);
        tvVisitorStateTitle = findViewById(R.id.tvVisitorStateTitle);
        tvVisitorStateSubtitle = findViewById(R.id.tvVisitorStateSubtitle);

        // Odeslani zpravy tlacitkem
        btnSend.setOnClickListener(v -> {
            String text = etInput.getText().toString().trim();
            if (!text.isEmpty()) {
                etInput.setText("");
                processUserInput(text);
            }
        });

        // Odeslani Enterem
        etInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                btnSend.performClick();
                return true;
            }
            return false;
        });

        // Mikrofon (v Admin i Navstevnickem rezimu)
        btnMic.setOnClickListener(v -> startSpeechRecognition());
        btnVisitorMic.setOnClickListener(v -> startSpeechRecognition());

        // Prepinani mezi Admin (Test) a Navstevnickou (Prezentacni) obrazovkou
        btnSwitchToVisitor.setOnClickListener(v -> {
            adminContainer.setVisibility(View.GONE);
            visitorContainer.setVisibility(View.VISIBLE);
        });
        btnSwitchToAdmin.setOnClickListener(v -> {
            updateRemoteUrlLabels();
            visitorContainer.setVisibility(View.GONE);
            adminContainer.setVisibility(View.VISIBLE);
        });

        // Popup s prehledem a primym spoustenim 44 kategorii animaci
        btnAnimations.setOnClickListener(v -> showAnimationsPopup());
        btnVisitorAnimations.setOnClickListener(v -> showAnimationsPopup());

        // Slider hlasitosti (AudioManager STREAM_MUSIC)
        initVolumeControl();

        // Smazani chatu - VZDY klikatelne, resetuje i zasekly stav
        btnClear.setOnClickListener(v -> {
            chatHistory.clear();
            chatLogText.setLength(0);
            tvChatLog.setText("");
            setState(ConversationState.IDLE);
            appendToChat("SYSTEM", "Chat smazan. Muzete zacit novou konverzaci.");
            updateStatus("Rubby - Pripravena", true);
        });
    }

    private void updateRemoteUrlLabels() {
        String ip = RemoteControlServer.getLocalIpAddress();
        String urlText = "\uD83C\uDF10 Web ovlada\u010d: http://" + ip + ":" + RemoteControlServer.DEFAULT_PORT;
        if (tvRemoteUrlAdmin != null) {
            tvRemoteUrlAdmin.setText(urlText);
        }
    }

    private void initVolumeControl() {
        final AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (audioManager == null) return;

        final int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        final int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

        seekBarVolume.setMax(maxVolume);
        seekBarVolume.setProgress(currentVolume);
        updateVolumeLabel(currentVolume, maxVolume);

        seekBarVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0);
                }
                updateVolumeLabel(progress, maxVolume);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void updateVolumeLabel(int current, int max) {
        if (max <= 0) return;
        int percent = Math.round((current * 100f) / max);
        tvVolumePercent.setText(percent + " %");
    }

    private void showAnimationsPopup() {
        final String[] keys = AnimationManager.getCategoryKeys();
        final String[] labels = AnimationManager.getPopupLabels();

        new AlertDialog.Builder(this)
                .setTitle("\uD83C\uDFAD Co v\u0161echno Rubby um\u00ed p\u0159edv\u00e9st (44 kategori\u00ed / 93 animac\u00ed)")
                .setItems(labels, (dialog, which) -> {
                    if (which >= 0 && which < keys.length) {
                        String selectedKey = keys[which];
                        playStandaloneAnimation(selectedKey, false);
                    }
                })
                .setNegativeButton("Zav\u0159\u00edt", null)
                .show();
    }

    private void initLlmAndSpinner() {
        String geminiKey = BuildConfig.GEMINI_API_KEY;
        String openRouterKey = BuildConfig.OPENROUTER_API_KEY;

        if (geminiKey != null && !geminiKey.isEmpty()) {
            geminiService = new GeminiService(geminiKey);
        }
        if (openRouterKey != null && !openRouterKey.isEmpty()) {
            openRouterService = new OpenRouterService(openRouterKey, "openrouter/free");
        }

        String[] options = {
            "Gemini Flash (Google)",
            "Auto Free (OpenRouter)",
            "Gemma 4 31B (OpenRouter Free)",
            "Nemotron 3.5 Lightning (OpenRouter Free)",
            "Nemotron 3 Ultra (OpenRouter Free)"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item, options);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerLlm.setAdapter(adapter);

        spinnerLlm.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                if (position == 0) {
                    llmService = geminiService;
                    if (geminiService == null) {
                        appendToChat("SYSTEM", "Upozorneni: Gemini klic chybi.");
                    } else {
                        appendToChat("SYSTEM", "Prepina se na: Google Gemini API.");
                    }
                } else {
                    llmService = openRouterService;
                    if (openRouterService == null) {
                        appendToChat("SYSTEM", "Upozorneni: OpenRouter klic chybi.");
                    } else {
                        if (position == 1) ((OpenRouterService)openRouterService).setModelName("openrouter/free");
                        else if (position == 2) ((OpenRouterService)openRouterService).setModelName("google/gemma-4-31b-it:free");
                        else if (position == 3) ((OpenRouterService)openRouterService).setModelName("nvidia/nemotron-3.5-lightning:free");
                        else if (position == 4) ((OpenRouterService)openRouterService).setModelName("nvidia/nemotron-3-ultra-550b-a55b:free");

                        appendToChat("SYSTEM", "Prepina se na: OpenRouter (" + options[position] + ").");
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Set default based on availability
        if (geminiService != null) {
            spinnerLlm.setSelection(0);
            llmService = geminiService;
        } else if (openRouterService != null) {
            spinnerLlm.setSelection(1);
            llmService = openRouterService;
        }

        if (llmService == null) {
            updateStatus("Chyba: chybi API klice", false);
        } else {
            updateStatus("Rubby - Pripravena", true);
        }
    }

    // ==================== SPEECH RECOGNITION ====================

    private void checkPermissions() {
        List<String> neededPermissions = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            neededPermissions.add(Manifest.permission.RECORD_AUDIO);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            neededPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        
        if (!neededPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    neededPermissions.toArray(new String[0]),
                    PERMISSION_REQUEST_AUDIO);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_AUDIO) {
            for (int i = 0; i < permissions.length && i < grantResults.length; i++) {
                if (Manifest.permission.RECORD_AUDIO.equals(permissions[i])) {
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        fileLogger.i(TAG, "Povoleni mikrofonu udeleno");
                        initSpeechRecognizer();
                    } else {
                        appendToChat("SYSTEM", "Mikrofon neni povolen - hlasove ovladani nebude fungovat.");
                    }
                } else if (Manifest.permission.WRITE_EXTERNAL_STORAGE.equals(permissions[i])) {
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        fileLogger.i(TAG, "Povoleni pro zapis logu udeleno");
                    }
                }
            }
        }
    }

    private void initSpeechRecognizer() {
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                fileLogger.d(TAG, "SpeechRecognizer pripraven");
            }

            @Override
            public void onBeginningOfSpeech() {
                fileLogger.d(TAG, "Zaznamenavam rec...");
            }

            @Override
            public void onRmsChanged(float rmsdB) {}

            @Override
            public void onBufferReceived(byte[] buffer) {}

            @Override
            public void onEndOfSpeech() {
                fileLogger.d(TAG, "Konec reci");
                runOnUiThread(() -> updateStatus("Zpracovavam...", true));
            }

            @Override
            public void onError(int error) {
                String msg;
                switch (error) {
                    case SpeechRecognizer.ERROR_NO_MATCH:
                        msg = "Nerozumel jsem. Zkuste to znovu.";
                        break;
                    case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                        msg = "Nic jsem nezachytil. Zkuste to znovu.";
                        break;
                    case SpeechRecognizer.ERROR_NETWORK:
                    case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                        msg = "Chyba site. Zkontrolujte pripojeni.";
                        break;
                    case SpeechRecognizer.ERROR_CLIENT:
                        msg = "Rozpoznavani bylo preruseno.";
                        break;
                    default:
                        msg = "Chyba rozpoznavani (kod " + error + ").";
                        break;
                }
                fileLogger.e(TAG, "SpeechRecognizer chyba: " + error);
                
                // Restart pro chyby zpusobene zaseknutim
                if (error == SpeechRecognizer.ERROR_CLIENT || error == SpeechRecognizer.ERROR_SERVER) {
                    runOnUiThread(MainActivity.this::initSpeechRecognizer);
                }
                
                runOnUiThread(() -> {
                    appendToChat("SYSTEM", msg);
                    setState(ConversationState.IDLE);
                    updateStatus("Rubby - Pripravena", true);
                });
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String recognizedText = matches.get(0);
                    fileLogger.i(TAG, "Rozpoznano: " + recognizedText);
                    runOnUiThread(() -> processUserInput(recognizedText));
                } else {
                    runOnUiThread(() -> {
                        appendToChat("SYSTEM", "Nerozumel jsem. Zkuste to znovu.");
                        setState(ConversationState.IDLE);
                        updateStatus("Rubby - Pripravena", true);
                    });
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {}

            @Override
            public void onEvent(int eventType, Bundle params) {}
        });
    }

    private void startSpeechRecognition() {
        if (currentState != ConversationState.IDLE) {
            return;
        }
        startListeningInternal(wakeWordChatFuture != null);
    }

    private void startListeningInternal(boolean waitForWakeWordRelease) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            setState(ConversationState.IDLE);
            appendToChat("SYSTEM", "Mikrofon neni povolen. Povolte ho v nastaveni.");
            return;
        }

        setState(ConversationState.LISTENING);
        updateStatus("Posloucham...", true);

        if (speechRecognizer == null) {
            initSpeechRecognizer();
        }

        final Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "cs-CZ");
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);

        if (waitForWakeWordRelease) {
            // Dej QiSDK ~200 ms na uvolneni mikrofonu po stopWakeWordListening()
            tvStatus.postDelayed(() -> {
                if (currentState == ConversationState.LISTENING && speechRecognizer != null) {
                    speechRecognizer.startListening(intent);
                }
            }, 200);
        } else {
            speechRecognizer.startListening(intent);
        }
    }

    // ==================== HLAVNI LOGIKA ====================

    private void processUserInput(String userText) {
        setState(ConversationState.PROCESSING);
        appendToChat("Vy", userText);
        updateStatus("Rubby premysli...", true);

        if (llmService == null) {
            appendToChat("SYSTEM", "LLM service neni inicializovana. Zkontrolujte API klic.");
            setState(ConversationState.IDLE);
            updateStatus("Chyba", false);
            return;
        }

        chatHistory.addUserMessage(userText);

        // Odeslani do LLM na background vlakne
        executor.execute(() -> {
            try {
                cz.kubmak.rubby_llm_exp.llm.models.LlmResult result = llmService.generateResponse(chatHistory.getHistory());
                // Získáme syrovou odpověď z LLM
                String response = result.text;
                int tokens = result.tokensUsed;

                // Extrakce animačního tagu a vyčištění textu pro TTS
                String animationCategory = cz.kubmak.rubby_llm_exp.util.TextCleaner.extractAnimationTag(response);
                String cleanedResponse = cz.kubmak.rubby_llm_exp.util.TextCleaner.clean(response);

                chatHistory.addModelMessage(cleanedResponse);

                runOnUiThread(() -> {
                    appendToChat("Rubby", cleanedResponse);
                    if (tokens > 0) {
                        appendToChat("SYSTEM", "[TOKEN USAGE] Tento dotaz spotřeboval " + tokens + " tokenů.");
                    }
                    speakResponse(cleanedResponse, animationCategory);
                });

            } catch (Exception e) {
                fileLogger.e(TAG, "Chyba LLM: " + e.getMessage(), e);
                chatHistory.removeLastUserMessage();
                runOnUiThread(() -> {
                    appendToChat("Rubby", "Omlouvam se, doslo k chybe spojeni.");
                    setState(ConversationState.IDLE);
                    updateStatus("Chyba spojeni", false);
                });
            } catch (OutOfMemoryError e) {
                fileLogger.e(TAG, "OutOfMemoryError! Cistim historii.", e);
                chatHistory.clear();
                runOnUiThread(() -> {
                    appendToChat("SYSTEM", "Dosla pamet. Historie smazana.");
                    setState(ConversationState.IDLE);
                    updateStatus("Rubby - Pripravena", true);
                });
            }
        });
    }

    // ==================== TEXT-TO-SPEECH ====================

    private void speakResponse(String text, String animationCategory) {
        final boolean isDisco = ShoulderLedManager.isDiscoCategory(animationCategory);
        final boolean hasSpecificAnim = (animationCategory != null && AnimationManager.hasCategory(animationCategory));
        setState(ConversationState.SPEAKING);
        if (isDisco && shoulderLedManager != null) {
            shoulderLedManager.startDisco();
        }
        updateStatus("Rubby mluvi...", true);

        if (qiContext != null) {
            // Na Pepperovi: QiSDK TTS + animace
            executor.execute(() -> {
                try {
                    // Vyber animaci - specificka kategorie od LLM, nebo nahodna idle
                    int animRes;
                    if (hasSpecificAnim) {
                        animRes = AnimationManager.getAnimation(animationCategory);
                        fileLogger.i(TAG, "LLM pozadala animaci: " + animationCategory);
                    } else {
                        animRes = AnimationManager.getIdleAnimation();
                    }

                    // Spust animaci paralelne (async)
                    runAnimationAsync(animRes);

                    // Say async s cekanim, abychom ho mohli pripadne zastavit pres /api/stop
                    Say say = SayBuilder.with(qiContext)
                            .withText(text)
                            .build();
                    currentSayFuture = say.async().run();
                    currentSayFuture.get();

                    // Pokud sla specificka (napr. tanecni/disco) animace, pockame az dobehne cela
                    if (hasSpecificAnim && currentAnimateFuture != null && !currentAnimateFuture.isDone()) {
                        currentAnimateFuture.get();
                    }

                    if (isDisco && shoulderLedManager != null) {
                        shoulderLedManager.stopDisco();
                    }

                    // Say (i pripadna tanecni animace) uspesne dobehly
                    runOnUiThread(() -> {
                        setState(ConversationState.IDLE);
                        updateStatus("Rubby - Pripravena", true);
                    });
                } catch (Exception e) {
                    fileLogger.e(TAG, "QiSDK Say chyba/zastaveno: " + e.getMessage());
                    if (isDisco && shoulderLedManager != null) {
                        shoulderLedManager.stopDisco();
                    }
                    // Say selhalo nebo bylo zastaveno - rovnou odemkni UI
                    runOnUiThread(() -> {
                        setState(ConversationState.IDLE);
                        updateStatus("Rubby - Pripravena", true);
                    });
                }
            });
        } else {
            // Bez QiSDK: Android TTS
            speakWithAndroidTts(text);
        }
    }

    private void runAnimationAsync(final int animRes) {
        if (qiContext == null) return;
        // QiSDK .build() nesmi byt nikdy volano na hlavnim UI vlakne!
        if (Looper.myLooper() == Looper.getMainLooper()) {
            moveExecutor.execute(() -> runAnimationAsync(animRes));
            return;
        }
        try {
            if (currentAnimateFuture != null && !currentAnimateFuture.isDone()) {
                currentAnimateFuture.requestCancellation();
            }
            Animation animation = AnimationBuilder.with(qiContext)
                    .withResources(animRes)
                    .build();
            Animate animate = AnimateBuilder.with(qiContext)
                    .withAnimation(animation)
                    .build();
            currentAnimateFuture = animate.async().run();
        } catch (Exception e) {
            fileLogger.w(TAG, "Animace se nezdarila (neni kriticke): " + e.getMessage());
        }
    }

    /**
     * Samostatne prehrani animace (z weboveho ovladace nebo z popup menu na tabletu).
     * Vzdy bezi na background vlakne (QiSDK zakazuje .build() na UI vlakne) a docasne
     * pozastavi QiChat wake-word naslouchani, aby mel robot volne klouby pro animaci.
     */
    private void playStandaloneAnimation(final String category, final boolean fromRemote) {
        runOnUiThread(() -> {
            String prefix = fromRemote ? "[DALKOVE OVLADANI] " : "";
            appendToChat("SYSTEM", prefix + "Spou\u0161t\u00edm animaci: [" + category + "]");
            if (qiContext == null) {
                appendToChat("SYSTEM", "Nelze p\u0159ehr\u00e1t animaci \u2013 Rubby nem\u00e1 Robot Focus.");
                return;
            }
            final boolean isDisco = ShoulderLedManager.isDiscoCategory(category);
            final boolean wasIdle = (currentState == ConversationState.IDLE);
            if (wasIdle) {
                setState(ConversationState.SPEAKING);
                updateStatus("Rubby p\u0159edv\u00e1d\u00ed animaci [" + category + "]...", true);
            }
            if (isDisco && shoulderLedManager != null) {
                shoulderLedManager.startDisco();
            }

            moveExecutor.execute(() -> {
                try {
                    if (currentAnimateFuture != null && !currentAnimateFuture.isDone()) {
                        currentAnimateFuture.requestCancellation();
                    }
                    if (wasIdle) {
                        // Kratka pauza, aby QiSDK uvolnilo QiChat wake-word pred zacatkem animace
                        Thread.sleep(150);
                    }
                    if (qiContext == null) return;

                    int animRes = AnimationManager.getAnimation(category);
                    Animation animation = AnimationBuilder.with(qiContext)
                            .withResources(animRes)
                            .build();
                    Animate animate = AnimateBuilder.with(qiContext)
                            .withAnimation(animation)
                            .build();
                    final Future<Void> animFuture = animate.async().run();
                    currentAnimateFuture = animFuture;

                    animFuture.thenConsume(future -> {
                        if (future.hasError()) {
                            fileLogger.w(TAG, "Samostatna animace [" + category + "] chyba: " + future.getErrorMessage());
                        } else {
                            fileLogger.i(TAG, "Samostatna animace [" + category + "] dokoncena.");
                        }
                        if (isDisco && shoulderLedManager != null && currentAnimateFuture == animFuture) {
                            shoulderLedManager.stopDisco();
                        }
                        runOnUiThread(() -> {
                            // Vrat stav do IDLE pouze pokud tato animace nebyla nahrazena jinou a nebezi rec
                            if (currentAnimateFuture == animFuture
                                    && currentState == ConversationState.SPEAKING
                                    && (currentSayFuture == null || currentSayFuture.isDone())) {
                                setState(ConversationState.IDLE);
                                updateStatus("Rubby - Pripravena", true);
                            }
                        });
                    });
                } catch (Exception e) {
                    fileLogger.e(TAG, "Chyba pri spousteni animace [" + category + "]: " + e.getMessage());
                    if (isDisco && shoulderLedManager != null) {
                        shoulderLedManager.stopDisco();
                    }
                    runOnUiThread(() -> {
                        appendToChat("SYSTEM", "Chyba animace [" + category + "]: " + e.getMessage());
                        if (currentState == ConversationState.SPEAKING
                                && (currentSayFuture == null || currentSayFuture.isDone())) {
                            setState(ConversationState.IDLE);
                            updateStatus("Rubby - Pripravena", true);
                        }
                    });
                }
            });
        });
    }

    private void speakWithAndroidTts(String text) {
        if (androidTtsReady) {
            androidTts.setOnUtteranceProgressListener(new android.speech.tts.UtteranceProgressListener() {
                @Override
                public void onStart(String utteranceId) {}

                @Override
                public void onDone(String utteranceId) {
                    runOnUiThread(() -> {
                        setState(ConversationState.IDLE);
                        updateStatus("Rubby - Pripravena", true);
                    });
                }

                @Override
                public void onError(String utteranceId) {
                    runOnUiThread(() -> {
                        setState(ConversationState.IDLE);
                        updateStatus("Rubby - Pripravena", true);
                    });
                }
            });
            androidTts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "rubby_response");
        } else {
            fileLogger.w(TAG, "Zadny TTS neni k dispozici - jen text na obrazovce");
            setState(ConversationState.IDLE);
            updateStatus("Rubby - Pripravena", true);
        }
    }

    // ==================== UI HELPERS ====================

    private void appendToChat(String sender, String message) {
        String prefix;
        switch (sender) {
            case "Vy":
                prefix = ">> Vy: ";
                break;
            case "Rubby":
                prefix = "<< Rubby: ";
                lastRubbyMessage = message;
                break;
            default:
                prefix = "[" + sender + "] ";
                break;
        }

        chatLogText.append(prefix).append(message).append("\n\n");
        tvChatLog.setText(chatLogText.toString());

        // Scroll dolu
        scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
    }

    private void updateStatus(String status, boolean ok) {
        tvStatus.setText(status);
        findViewById(R.id.statusIndicator).setBackgroundColor(
                ok ? 0xFF4CAF50 : 0xFFf44336
        );
    }

    private Future<Void> wakeWordChatFuture;

    private void startWakeWordListening() {
        if (qiContext == null || qiChatbot == null) return;
        
        executor.execute(() -> {
            try {
                if (wakeWordChatFuture != null) {
                    wakeWordChatFuture.requestCancellation();
                }
                wakeWordChat = ChatBuilder.with(qiContext)
                        .withChatbot(qiChatbot)
                        .build();
                wakeWordChatFuture = wakeWordChat.async().run();
                fileLogger.i(TAG, "Wake word listening started");
            } catch (Exception e) {
                fileLogger.w(TAG, "Failed to start wake word chat: " + e.getMessage());
            }
        });
    }

    private void stopWakeWordListening() {
        if (wakeWordChatFuture != null) {
            wakeWordChatFuture.requestCancellation();
            wakeWordChatFuture = null;
            fileLogger.i(TAG, "Wake word listening stopped");
        }
    }

    private void setState(ConversationState state) {
        this.currentState = state;
        boolean inputEnabled = (state == ConversationState.IDLE);
        etInput.setEnabled(inputEnabled);
        btnSend.setEnabled(inputEnabled);
        btnMic.setEnabled(inputEnabled);
        btnVisitorMic.setEnabled(inputEnabled);
        // Smazat chat je VZDY klikatelne - slouzi i jako nouzovy reset
        btnClear.setEnabled(true);

        // Aktualizace barvy a popisku podle stavu (v Admin i Navstevnicke obrazovce)
        // Pouzivame jen znaky podporovane na Androidu 6.0 (\u25CF, \uD83C\uDFA4, \u23F3, \uD83D\uDD0A)
        switch (state) {
            case IDLE:
                btnMic.setText("\uD83C\uDFA4 MIC");
                btnMic.setBackgroundColor(0xFF1565C0);
                visitorContainer.setBackgroundColor(0xFF102A43);
                tvVisitorStateTitle.setText("\u25CF  P\u0158IPRAVENA K ROZHOVORU");
                tvVisitorStateSubtitle.setText("Oslov m\u011b jm\u00e9nem \u201eRubby\u201c, pohla\u010f m\u011b po hlav\u011b nebo klepni na tla\u010d\u00edtko mikrofonu dole!");
                btnVisitorMic.setText("\uD83C\uDFA4  MLUVIT S RUBBY");
                btnVisitorMic.setBackgroundColor(0xFF1565C0);
                break;
            case LISTENING:
                btnMic.setText("\uD83C\uDFA4 POSLOUCH\u00c1M");
                btnMic.setBackgroundColor(0xFF2E7D32);
                visitorContainer.setBackgroundColor(0xFF1B5E20);
                tvVisitorStateTitle.setText("\uD83C\uDFA4  POSLOUCH\u00c1M T\u011a...");
                tvVisitorStateSubtitle.setText("Mluv pros\u00edm z\u0159eteln\u011b \u010desky \u2013 zeptej se na cokoliv nebo mi \u0159ekni, co m\u00e1m p\u0159edv\u00e9st!");
                btnVisitorMic.setText("\uD83C\uDFA4  POSLOUCH\u00c1M...");
                btnVisitorMic.setBackgroundColor(0xFF2E7D32);
                break;
            case PROCESSING:
                btnMic.setText("\u23F3 P\u0158EM\u00dd\u0160L\u00cdM");
                btnMic.setBackgroundColor(0xFFE65100);
                visitorContainer.setBackgroundColor(0xFFB45309);
                tvVisitorStateTitle.setText("\u23F3  P\u0158EM\u00dd\u0160L\u00cdM NAD ODPOV\u011aD\u00cd...");
                tvVisitorStateSubtitle.setText("Chvili\u010dku strpen\u00ed, pr\u00e1v\u011b zpracov\u00e1v\u00e1m tv\u016fj dotaz...");
                btnVisitorMic.setText("\u23F3  P\u0158EM\u00dd\u0160L\u00cdM...");
                btnVisitorMic.setBackgroundColor(0xFFE65100);
                break;
            case SPEAKING:
                btnMic.setText("\uD83D\uDD0A MLUV\u00cdM");
                btnMic.setBackgroundColor(0xFF283593);
                visitorContainer.setBackgroundColor(0xFF1E3A8A);
                tvVisitorStateTitle.setText("\uD83D\uDD0A  RUBBY PR\u00c1V\u011a MLUV\u00cd / P\u0158EDV\u00c1D\u00cd");
                tvVisitorStateSubtitle.setText("Poslouchej a d\u00edvej se, co pr\u00e1v\u011b \u0159\u00edk\u00e1m a p\u0159edv\u00e1d\u00edm!");
                btnVisitorMic.setText("\uD83D\uDD0A  RUBBY MLUV\u00cd...");
                btnVisitorMic.setBackgroundColor(0xFF283593);
                break;
        }

        if (shoulderLedManager != null) {
            shoulderLedManager.onStateChanged(state);
        }

        if (state == ConversationState.IDLE) {
            startWakeWordListening();
        } else {
            stopWakeWordListening();
        }
    }

    // ==================== QISDK LIFECYCLE ====================

    @Override
    public void onRobotFocusGained(QiContext qiContext) {
        this.qiContext = qiContext;
        fileLogger.i(TAG, "Robot Focus GAINED");

        if (shoulderLedManager != null) {
            shoulderLedManager.init(qiContext);
        }

        // Initialize Head Touch Sensor
        Touch touch = qiContext.getTouch();
        headTouchSensor = touch.getSensor("Head/Touch");
        headTouchSensor.addOnStateChangedListener(touchState -> {
            if (touchState.getTouched()) {
                fileLogger.i(TAG, "Head sensor touched!");
                runOnUiThread(this::onHeadTouched);
            }
        });

        // Inicializace senzoru dvířek napájecího portu (FlapSensor)
        try {
            chargingFlapSensor = qiContext.getPower().getChargingFlap();
            chargingFlapOpen = chargingFlapSensor.getState().getOpen();
            fileLogger.i(TAG, "Charging flap initial state (open): " + chargingFlapOpen);
            if (chargingFlapOpen) {
                runOnUiThread(() -> appendToChat("SYSTEM",
                        "POZOR: Napájecí port (dvířka vzadu dole) je OTEVŘENÝ – Rubby má zablokovaný pojezd!"));
            }
            chargingFlapSensor.addOnStateChangedListener(flapState -> {
                chargingFlapOpen = flapState.getOpen();
                fileLogger.i(TAG, "Charging flap state changed (open): " + chargingFlapOpen);
                runOnUiThread(() -> {
                    if (chargingFlapOpen) {
                        appendToChat("SYSTEM", "POZOR: Napájecí port byl otevřen – pojezd Rubby je zablokován!");
                    } else {
                        appendToChat("SYSTEM", "Napájecí port uzavřen – pojezd Rubby je opět povolen.");
                    }
                });
            });
        } catch (Exception e) {
            fileLogger.w(TAG, "Nelze inicializovat FlapSensor: " + e.getMessage());
        }

        // Inicializace Wake word (QiChat)
        executor.execute(() -> {
            try {
                Topic topic = TopicBuilder.with(qiContext)
                        .withResource(R.raw.wakeword)
                        .build();

                qiChatbot = QiChatbotBuilder.with(qiContext)
                        .withTopic(topic)
                        .build();

                qiChatbot.addOnBookmarkReachedListener((bookmark) -> {
                    if ("wake_word_heard".equals(bookmark.getName())) {
                        fileLogger.i(TAG, "Wake word (Rubby) detected!");
                        runOnUiThread(this::onWakeWordTriggered);
                    }
                });

                // Zkusit rovnou nastartovat pokud jsme IDLE
                if (currentState == ConversationState.IDLE) {
                    startWakeWordListening();
                }

            } catch (Exception e) {
                fileLogger.e(TAG, "Nelze inicializovat QiChat wakeword: " + e.getMessage());
            }
        });

        runOnUiThread(() -> {
            appendToChat("SYSTEM", "Robot Focus ziskan! Rubby pouziva svuj vlastni hlas, animace a slysi na sve jmeno.");
            updateStatus("Rubby - Pripravena (QiSDK)", true);
        });
    }

    @Override
    public void onRobotFocusLost() {
        if (headTouchSensor != null) {
            headTouchSensor.removeAllOnStateChangedListeners();
            headTouchSensor = null;
        }
        if (chargingFlapSensor != null) {
            chargingFlapSensor.removeAllOnStateChangedListeners();
            chargingFlapSensor = null;
        }
        chargingFlapOpen = false;
        stopWakeWordListening();
        if (qiChatbot != null) {
            qiChatbot.removeAllOnBookmarkReachedListeners();
            qiChatbot = null;
        }
        if (shoulderLedManager != null) {
            shoulderLedManager.release();
        }
        awarenessHolder = null;
        joystickTargetFrame = null;
        joystickGoTo = null;
        this.qiContext = null;
        fileLogger.w(TAG, "Robot Focus LOST");
        runOnUiThread(() -> {
            appendToChat("SYSTEM", "Robot Focus ztracen. Pouzivam Android TTS.");
            updateStatus("Rubby - Pripravena (bez QiSDK)", true);
        });
    }

    @Override
    public void onRobotFocusRefused(String reason) {
        fileLogger.w(TAG, "Robot Focus REFUSED: " + reason);
        runOnUiThread(() -> {
            appendToChat("SYSTEM", "Robot Focus odmitnut: " + reason + ". Funguju i bez nej.");
            updateStatus("Rubby - Pripravena (bez QiSDK)", true);
        });
    }

    private String getRandomHeadTouchResponse() {
        // Reakce specificky na fyzicke pohlazeni po hlave ("pat pat")
        String[] responses = {
            "Hi hi, to lecht\u00e1! Copak bys r\u00e1d?",
            "J\u00e9\u00e9, pat pat, poslouch\u00e1m t\u011b.",
            "J\u016f\u016f\u016f, to je hezk\u00e9 pohlazen\u00ed! Copak si p\u0159eje\u0161?",
            "Hele, to je p\u0159\u00edjemn\u00e9! Co pot\u0159ebuje\u0161?",
            "\u00dat\u011bk, n\u011bkdo m\u011b pohladil! Co t\u011b tr\u00e1p\u00ed?",
            "Haha, hlazen\u00ed po hlavi\u010dce m\u00e1m moc r\u00e1da! Pov\u00eddej.",
        };
        return responses[random.nextInt(responses.length)];
    }

    private String getRandomWakeWordResponse() {
        // Reakce na hlasove osloveni jmenem ("Rubby", "Ahoj Rubby")
        String[] responses = {
            "Ano? Poslouch\u00e1m t\u011b!",
            "Tady jsem! Copak pot\u0159ebuje\u0161?",
            "Ahoj! S \u010d\u00edm ti m\u016f\u017eu pomoct?",
            "Sly\u0161\u00edm t\u011b, pov\u00eddej!",
            "Ano, to jsem j\u00e1, Rubby! Copak t\u011b zaj\u00edm\u00e1?",
            "Hal\u00f3, hal\u00f3! Poslouch\u00e1m.",
            "Jsem p\u0159ipraven\u00e1! Na co se chce\u0161 zeptat?",
            "Copak? Pov\u00eddej, poslouch\u00e1m.",
        };
        return responses[random.nextInt(responses.length)];
    }

    private void onHeadTouched() {
        triggerGreetingAndListen(true);
    }

    private void onWakeWordTriggered() {
        triggerGreetingAndListen(false);
    }

    private void triggerGreetingAndListen(final boolean isHeadTouch) {
        if (currentState != ConversationState.IDLE) {
            return;
        }
        // Okamzite uzamkni stav na SPEAKING (tim se i zastavi wakeWordChatFuture
        // a zabrani se vicenasobnemu spusteni pri rychlem hlazeni po hlave)
        setState(ConversationState.SPEAKING);
        updateStatus("Rubby mluvi...", true);

        executor.execute(() -> {
            try {
                // Kratka pauza, aby QiSDK stihlo ukoncit Chat akci z wake-wordu
                Thread.sleep(250);

                if (qiContext != null) {
                    String greeting = isHeadTouch
                            ? getRandomHeadTouchResponse()
                            : getRandomWakeWordResponse();
                    // Spust doprovodnou animaci (lechtani/radost pri pohlazeni, pozdrav pri zavolani)
                    runAnimationAsync(AnimationManager.getAnimation(isHeadTouch ? "laugh" : "hello"));
                    SayBuilder.with(qiContext)
                            .withText(greeting)
                            .build()
                            .run();
                }
            } catch (Exception e) {
                fileLogger.e(TAG, "Error in greeting response: " + e.getMessage());
            } finally {
                // Prejdi do poslouchani (mikrofon uz neni blokovan wake-wordem)
                runOnUiThread(() -> {
                    if (currentState == ConversationState.SPEAKING) {
                        startListeningInternal(false);
                    }
                });
            }
        });
    }

    // ==================== FULLSCREEN ====================

    private void hideSystemUI() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        );
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            // hideSystemUI(); // Zakazano pro testovani v emulatoru
        }
    }

    // ==================== VZDALENE OVLADANI (HTTP SERVER + API) ====================

    @Override
    public String getCurrentStateName() {
        return currentState != null ? currentState.name() : "IDLE";
    }

    @Override
    public boolean hasRobotFocus() {
        return qiContext != null;
    }

    @Override
    public boolean isChargingFlapOpen() {
        return chargingFlapOpen;
    }

    @Override
    public boolean isFaceTrackingEnabled() {
        return faceTrackingEnabled;
    }

    @Override
    public boolean isNarrowSpaceMode() {
        return narrowSpaceMode;
    }

    @Override
    public int getVolumePercent() {
        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (am == null) return 80;
        int max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int cur = am.getStreamVolume(AudioManager.STREAM_MUSIC);
        return max > 0 ? Math.round((cur * 100f) / max) : 80;
    }

    @Override
    public boolean isVisitorScreenActive() {
        return visitorContainer != null && visitorContainer.getVisibility() == View.VISIBLE;
    }

    @Override
    public String getLastRubbyMessage() {
        return lastRubbyMessage != null ? lastRubbyMessage : "";
    }

    @Override
    public void onRemotePrompt(final String prompt) {
        runOnUiThread(() -> {
            if (speechRecognizer != null && currentState == ConversationState.LISTENING) {
                try {
                    speechRecognizer.cancel();
                } catch (Exception ignored) {}
            }
            appendToChat("SYSTEM", "[DALKOVE OVLADANI] Prijat AI prompt");
            processUserInput(prompt);
        });
    }

    @Override
    public void onRemoteSay(final String text, final String animationCategory) {
        runOnUiThread(() -> {
            if (speechRecognizer != null && currentState == ConversationState.LISTENING) {
                try {
                    speechRecognizer.cancel();
                } catch (Exception ignored) {}
            }
            String cleaned = TextCleaner.clean(text);
            appendToChat("Rubby", cleaned);
            speakResponse(cleaned, animationCategory);
        });
    }

    @Override
    public void onRemoteAnimate(final String category) {
        playStandaloneAnimation(category, true);
    }

    @Override
    public void onRemoteMove(final double xMeters, final double yMeters, final double thetaRadians) {
        if (chargingFlapOpen) {
            runOnUiThread(() -> appendToChat("SYSTEM",
                    "[DALKOVE OVLADANI] Nelze jet – napájecí port (dvířka vzadu dole) je otevřený!"));
            return;
        }
        runOnUiThread(() -> appendToChat("SYSTEM",
                String.format(Locale.US, "[DALKOVE OVLADANI] Pojezd: x=%.2fm, y=%.2fm, rot=%.2frad", xMeters, yMeters, thetaRadians)));
        if (qiContext == null) {
            fileLogger.w(TAG, "Nelze provest pojezd - neni QiContext");
            return;
        }
        moveExecutor.execute(() -> {
            try {
                if (currentGoToFuture != null && !currentGoToFuture.isDone()) {
                    currentGoToFuture.requestCancellation();
                }
                // Pokud jde o ciste otaceni na miste (x=0, y=0), pridame maly posun (12 cm) ve smeru otoceni,
                // protoze GoTo v QiSDK jinak povazuje vzdalenost 0.0m za jiz splnenou a neotoci se!
                double effX = xMeters;
                double effY = yMeters;
                if (Math.abs(effX) < 0.05 && Math.abs(effY) < 0.05 && Math.abs(thetaRadians) > 0.05) {
                    effX = 0.12 * Math.cos(thetaRadians);
                    effY = 0.12 * Math.sin(thetaRadians);
                }

                Actuation actuation = qiContext.getActuation();
                Mapping mapping = qiContext.getMapping();
                Frame robotFrame = actuation.robotFrame();
                Transform transform = TransformBuilder.create().from2DTransform(effX, effY, thetaRadians);
                FreeFrame targetFrame = mapping.makeFreeFrame();
                targetFrame.update(robotFrame, transform, 0L);

                GoTo goTo = GoToBuilder.with(qiContext)
                        .withFrame(targetFrame.frame())
                        .withPathPlanningPolicy(PathPlanningPolicy.STRAIGHT_LINES_ONLY)
                        .withFinalOrientationPolicy(OrientationPolicy.ALIGN_X)
                        .withMaxSpeed(0.55f)
                        .build();
                currentGoToFuture = goTo.async().run();
            } catch (Exception e) {
                fileLogger.e(TAG, "Chyba pri pojezdu GoTo: " + e.getMessage());
            }
        });
    }

    @Override
    public void onRemoteJoystick(final double normX, final double normY, final double normTheta) {
        if (qiContext == null || chargingFlapOpen) return;
        moveExecutor.execute(() -> {
            try {
                // 1) Primarni prime rizeni rychlosti kol pres NAOqi ALMotion.moveToward(vx, vy, vtheta).
                // Umoznuje ciste otaceni na miste (pravym joystickem) i okamzite pojezdy a ukroky (levym joystickem).
                if (shoulderLedManager != null) {
                    float vx = (float) Math.max(-1.0, Math.min(1.0, normX * 0.75));
                    float vy = (float) Math.max(-1.0, Math.min(1.0, normY * 0.65));
                    float vth = (float) Math.max(-1.0, Math.min(1.0, normTheta * 0.75));
                    if (Math.abs(vx) < 0.04f && Math.abs(vy) < 0.04f && Math.abs(vth) < 0.04f) {
                        shoulderLedManager.stopMove();
                        return;
                    }
                    if (shoulderLedManager.moveToward(vx, vy, vth)) {
                        return;
                    }
                }

                // 2) Zalozni cesta pres QiSDK GoTo (pokud neni dostupny privatni ALMotion)
                Actuation actuation = qiContext.getActuation();
                Mapping mapping = qiContext.getMapping();
                Frame robotFrame = actuation.robotFrame();

                double targetX = normX * 1.0;
                double targetY = normY * 0.8;
                double targetTheta = normTheta * 1.2;

                if (Math.abs(targetX) < 0.05 && Math.abs(targetY) < 0.05 && Math.abs(targetTheta) < 0.05) {
                    if (currentGoToFuture != null && !currentGoToFuture.isDone()) {
                        currentGoToFuture.requestCancellation();
                    }
                    return;
                }

                if (Math.abs(targetX) < 0.08 && Math.abs(targetY) < 0.08 && Math.abs(targetTheta) >= 0.05) {
                    targetX = 0.15 * Math.cos(targetTheta);
                    targetY = 0.15 * Math.sin(targetTheta);
                }

                Transform transform = TransformBuilder.create().from2DTransform(targetX, targetY, targetTheta);
                if (joystickTargetFrame == null) {
                    joystickTargetFrame = mapping.makeFreeFrame();
                }
                joystickTargetFrame.update(robotFrame, transform, 0L);

                if (joystickGoTo == null) {
                    joystickGoTo = GoToBuilder.with(qiContext)
                            .withFrame(joystickTargetFrame.frame())
                            .withPathPlanningPolicy(PathPlanningPolicy.STRAIGHT_LINES_ONLY)
                            .withFinalOrientationPolicy(OrientationPolicy.ALIGN_X)
                            .withMaxSpeed(0.55f)
                            .build();
                }
                if (currentGoToFuture == null || currentGoToFuture.isDone()) {
                    currentGoToFuture = joystickGoTo.async().run();
                }
            } catch (Exception e) {
                fileLogger.w(TAG, "Chyba pri realtime joysticku: " + e.getMessage());
                joystickTargetFrame = null;
                joystickGoTo = null;
            }
        });
    }

    @Override
    public void onRemoteStopMove() {
        moveExecutor.execute(() -> {
            if (shoulderLedManager != null) {
                shoulderLedManager.stopMove();
            }
            try {
                if (currentGoToFuture != null && !currentGoToFuture.isDone()) {
                    currentGoToFuture.requestCancellation();
                }
            } catch (Exception ignored) {}
        });
    }

    @Override
    public void onRemoteStop() {
        moveExecutor.execute(() -> {
            if (shoulderLedManager != null) {
                shoulderLedManager.stopMove();
            }
        });
        try {
            if (currentGoToFuture != null && !currentGoToFuture.isDone()) {
                currentGoToFuture.requestCancellation();
            }
            if (currentAnimateFuture != null && !currentAnimateFuture.isDone()) {
                currentAnimateFuture.requestCancellation();
            }
            if (currentSayFuture != null && !currentSayFuture.isDone()) {
                currentSayFuture.requestCancellation();
            }
            if (shoulderLedManager != null) {
                shoulderLedManager.stopDisco();
            }
        } catch (Exception ignored) {}
        runOnUiThread(() -> {
            if (speechRecognizer != null && currentState == ConversationState.LISTENING) {
                try {
                    speechRecognizer.cancel();
                } catch (Exception ignored) {}
            }
            if (androidTts != null && androidTtsReady) {
                androidTts.stop();
            }
            setState(ConversationState.IDLE);
            updateStatus("Rubby - Pripravena (Zastaveno)", true);
            appendToChat("SYSTEM", "[DALKOVE OVLADANI] Pohyb a rec zastaveny.");
        });
    }

    @Override
    public void onRemoteSetFaceTracking(final boolean enabled) {
        this.faceTrackingEnabled = enabled;
        if (shoulderLedManager != null) {
            shoulderLedManager.setFaceTracking(enabled);
        }
        if (qiContext == null) return;
        moveExecutor.execute(() -> {
            try {
                if (awarenessHolder == null) {
                    awarenessHolder = HolderBuilder.with(qiContext)
                            .withAutonomousAbilities(
                                    AutonomousAbilitiesType.BASIC_AWARENESS,
                                    AutonomousAbilitiesType.BACKGROUND_MOVEMENT
                            )
                            .build();
                }
                if (enabled) {
                    awarenessHolder.async().release().get();
                    runOnUiThread(() -> appendToChat("SYSTEM",
                            "[DALKOVE OVLADANI] Sledování obličejů (Focus mód) ZAPNUTO – Rubby opět otáčí hlavu za lidmi."));
                } else {
                    awarenessHolder.async().hold().get();
                    runOnUiThread(() -> appendToChat("SYSTEM",
                            "[DALKOVE OVLADANI] Sledování obličejů VYPNUTO – hlava Rubby je uzamčena rovně pro jízdu."));
                }
            } catch (Exception e) {
                fileLogger.w(TAG, "Chyba pri prepinani BasicAwareness Holderu: " + e.getMessage());
            }
        });
    }

    @Override
    public void onRemoteSetNarrowSpace(final boolean narrow) {
        this.narrowSpaceMode = narrow;
        if (shoulderLedManager != null) {
            shoulderLedManager.setNarrowSpaceMode(narrow);
        }
        runOnUiThread(() -> {
            if (narrow) {
                appendToChat("SYSTEM",
                        "[DALKOVE OVLADANI] Režim ÚZKÝ PROSTOR zapnut (odstup snížen z 35 cm na 8 cm pro jízdu mezi židlemi a stolem).");
            } else {
                appendToChat("SYSTEM",
                        "[DALKOVE OVLADANI] Režim ÚZKÝ PROSTOR vypnut (standardní bezpečnostní odstup 35 cm).");
            }
        });
    }

    @Override
    public void onRemoteStartListening() {
        runOnUiThread(this::startSpeechRecognition);
    }

    @Override
    public void onRemoteSetScreen(final boolean visitorMode) {
        runOnUiThread(() -> {
            if (visitorMode) {
                adminContainer.setVisibility(View.GONE);
                visitorContainer.setVisibility(View.VISIBLE);
            } else {
                visitorContainer.setVisibility(View.GONE);
                adminContainer.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onRemoteSetVolume(final int percent) {
        runOnUiThread(() -> {
            AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            if (am == null) return;
            int max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            int clamped = Math.max(0, Math.min(100, percent));
            int targetVol = Math.round((clamped * max) / 100f);
            am.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0);
            if (seekBarVolume != null) {
                seekBarVolume.setProgress(targetVol);
            }
            updateVolumeLabel(targetVol, max);
        });
    }

    // ==================== LIFECYCLE ====================

    @Override
    protected void onDestroy() {
        if (remoteControlServer != null) {
            remoteControlServer.stop();
        }
        try {
            QiSDK.unregister(this, this);
        } catch (Exception e) {
            fileLogger.w(TAG, "QiSDK unregister: " + e.getMessage());
        }

        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        if (androidTts != null) {
            androidTts.shutdown();
        }
        if (shoulderLedManager != null) {
            shoulderLedManager.shutdown();
        }
        executor.shutdown();
        moveExecutor.shutdown();

        super.onDestroy();
    }
}
