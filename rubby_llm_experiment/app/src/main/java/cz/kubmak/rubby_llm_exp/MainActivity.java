package cz.kubmak.rubby_llm_exp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
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
import android.widget.TextView;

import androidx.annotation.NonNull;
import com.aldebaran.qi.sdk.design.activity.RobotActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.aldebaran.qi.sdk.QiContext;
import com.aldebaran.qi.sdk.QiSDK;
import com.aldebaran.qi.sdk.RobotLifecycleCallbacks;
import com.aldebaran.qi.sdk.builder.AnimateBuilder;
import com.aldebaran.qi.sdk.builder.AnimationBuilder;
import com.aldebaran.qi.sdk.builder.SayBuilder;
import com.aldebaran.qi.sdk.object.actuation.Animate;
import com.aldebaran.qi.sdk.object.actuation.Animation;
import com.aldebaran.qi.sdk.object.conversation.Say;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cz.kubmak.rubby_llm_exp.animation.AnimationManager;
import cz.kubmak.rubby_llm_exp.conversation.ChatHistoryManager;
import cz.kubmak.rubby_llm_exp.conversation.ConversationState;
import cz.kubmak.rubby_llm_exp.llm.GeminiService;
import cz.kubmak.rubby_llm_exp.llm.ILlmService;
import cz.kubmak.rubby_llm_exp.util.TextCleaner;

public class MainActivity extends RobotActivity implements RobotLifecycleCallbacks {

    private static final String TAG = "RubbyLLM";
    private static final int PERMISSION_REQUEST_AUDIO = 1001;

    private final Random random = new Random();

    // UI
    private TextView tvStatus;
    private TextView tvChatLog;
    private EditText etInput;
    private Button btnSend;
    private Button btnMic;
    private Button btnClear;
    private ScrollView scrollView;

    // LLM
    private ILlmService llmService;
    private ChatHistoryManager chatHistory;
    private ConversationState currentState = ConversationState.IDLE;

    // QiSDK - muze byt null pokud neni focus (virtualni robot, telefon)
    private QiContext qiContext;

    // Android TTS fallback (kdyz neni QiSDK nebo neni focus)
    private TextToSpeech androidTts;
    private boolean androidTtsReady = false;

    // Speech recognition
    private SpeechRecognizer speechRecognizer;

    // Threading
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // Chat log text
    private final StringBuilder chatLogText = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Fullscreen immersive mode zakazan pro lepsi kompatibilitu s emulatorem
        // hideSystemUI();

        setContentView(R.layout.activity_main);

        // Registrace QiSDK lifecycle - na ne-Pepper zarizeni se nic nestane
        try {
            QiSDK.register(this, this);
            Log.i(TAG, "QiSDK registrace uspesna");
        } catch (Exception e) {
            Log.w(TAG, "QiSDK neni k dispozici (telefon/emulator): " + e.getMessage());
        }

        // Inicializace UI
        initViews();

        // Inicializace LLM
        String apiKey = BuildConfig.GEMINI_API_KEY;
        if (apiKey == null || apiKey.isEmpty()) {
            appendToChat("SYSTEM", "CHYBA: GEMINI_API_KEY neni nastaven v local.properties!");
            updateStatus("Chyba: chybi API klic", false);
        } else {
            llmService = new GeminiService(apiKey);
            appendToChat("SYSTEM", "Gemini API pripojeno. Muzete psat nebo mluvit.");
            updateStatus("Rubby - Pripravena", true);
        }

        chatHistory = new ChatHistoryManager();

        // Android TTS jako fallback pro ne-Pepper zarizeni
        androidTts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                Locale czLocale = new Locale("cs", "CZ");
                int result = androidTts.setLanguage(czLocale);
                
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(TAG, "Cestina pro Android TTS neni v tomto zarizeni/emulatoru podporovana.");
                    androidTts.setLanguage(Locale.US); // Zkus aspon anglictinu pro test
                }
                
                androidTtsReady = true;
                Log.i(TAG, "Android TTS pripraven (Fallback)");
            } else {
                Log.e(TAG, "Inicializace Android TTS selhala!");
            }
        });

        // Povoleni mikrofonu
        checkAudioPermission();
    }

    private void initViews() {
        tvStatus = findViewById(R.id.tvStatus);
        tvChatLog = findViewById(R.id.tvChatLog);
        etInput = findViewById(R.id.etInput);
        btnSend = findViewById(R.id.btnSend);
        btnMic = findViewById(R.id.btnMic);
        btnClear = findViewById(R.id.btnClear);
        scrollView = findViewById(R.id.scrollView);

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

        // Mikrofon
        btnMic.setOnClickListener(v -> startSpeechRecognition());

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

    // ==================== SPEECH RECOGNITION ====================

    private void checkAudioPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    PERMISSION_REQUEST_AUDIO);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "Povoleni mikrofonu udeleno");
            } else {
                appendToChat("SYSTEM", "Mikrofon neni povolen - hlasove ovladani nebude fungovat.");
            }
        }
    }

    private void startSpeechRecognition() {
        if (currentState == ConversationState.PROCESSING || currentState == ConversationState.SPEAKING) {
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            appendToChat("SYSTEM", "Mikrofon neni povolen. Povolte ho v nastaveni.");
            return;
        }

        setState(ConversationState.LISTENING);
        updateStatus("Posloucham...", true);

        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                Log.d(TAG, "SpeechRecognizer pripraven");
            }

            @Override
            public void onBeginningOfSpeech() {
                Log.d(TAG, "Zaznamenavam rec...");
            }

            @Override
            public void onRmsChanged(float rmsdB) {}

            @Override
            public void onBufferReceived(byte[] buffer) {}

            @Override
            public void onEndOfSpeech() {
                Log.d(TAG, "Konec reci");
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
                    default:
                        msg = "Chyba rozpoznavani (kod " + error + ").";
                        break;
                }
                Log.e(TAG, "SpeechRecognizer chyba: " + error);
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
                    Log.i(TAG, "Rozpoznano: " + recognizedText);
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

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "cs-CZ");
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);

        speechRecognizer.startListening(intent);
    }

    // ==================== HLAVNI LOGIKA ====================

    private void processUserInput(String userText) {
        setState(ConversationState.PROCESSING);
        appendToChat("Vy", userText);
        updateStatus("Rubby premysli...", true);

        chatHistory.addUserMessage(userText);

        if (llmService == null) {
            appendToChat("SYSTEM", "LLM service neni inicializovana. Zkontrolujte API klic.");
            setState(ConversationState.IDLE);
            updateStatus("Chyba", false);
            return;
        }

        // Odeslani do LLM na background vlakne
        executor.execute(() -> {
            try {
                String response = llmService.generateResponse(chatHistory.getHistory());

                // Extrakce animacniho tagu pred cistenim textu
                String animationCategory = TextCleaner.extractAnimationTag(response);
                String cleanedResponse = TextCleaner.clean(response);

                chatHistory.addModelMessage(cleanedResponse);

                runOnUiThread(() -> {
                    appendToChat("Rubby", cleanedResponse);
                    speakResponse(cleanedResponse, animationCategory);
                });

            } catch (OutOfMemoryError e) {
                Log.e(TAG, "OutOfMemoryError! Cistim historii.", e);
                chatHistory.clear();
                runOnUiThread(() -> {
                    appendToChat("SYSTEM", "Dosla pamet. Historie smazana.");
                    setState(ConversationState.IDLE);
                    updateStatus("Rubby - Pripravena", true);
                });
            } catch (Exception e) {
                Log.e(TAG, "Chyba LLM: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    appendToChat("Rubby", "Omlouvam se, doslo k chybe spojeni.");
                    setState(ConversationState.IDLE);
                    updateStatus("Chyba spojeni", false);
                });
            }
        });
    }

    // ==================== TEXT-TO-SPEECH ====================

    private void speakResponse(String text, String animationCategory) {
        setState(ConversationState.SPEAKING);
        updateStatus("Rubby mluvi...", true);

        if (qiContext != null) {
            // Na Pepperovi: QiSDK TTS + animace
            executor.execute(() -> {
                try {
                    // Vyber animaci - specificka kategorie od LLM, nebo nahodna idle
                    int animRes;
                    if (animationCategory != null && AnimationManager.hasCategory(animationCategory)) {
                        animRes = AnimationManager.getAnimation(animationCategory);
                        Log.i(TAG, "LLM pozadala animaci: " + animationCategory);
                    } else {
                        animRes = AnimationManager.getIdleAnimation();
                    }

                    // Spust animaci paralelne (async)
                    runAnimationAsync(animRes);

                    // Say synchronne - pocka az robot domluvi
                    Say say = SayBuilder.with(qiContext)
                            .withText(text)
                            .build();
                    say.run();

                    // Say uspesne dobehl
                    runOnUiThread(() -> {
                        setState(ConversationState.IDLE);
                        updateStatus("Rubby - Pripravena", true);
                    });
                } catch (Exception e) {
                    Log.e(TAG, "QiSDK Say chyba: " + e.getMessage());
                    // Say selhalo - rovnou odemkni UI (nevolej Android TTS z executoru)
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

    private void runAnimationAsync(int animRes) {
        if (qiContext == null) return;
        try {
            Animation animation = AnimationBuilder.with(qiContext)
                    .withResources(animRes)
                    .build();
            Animate animate = AnimateBuilder.with(qiContext)
                    .withAnimation(animation)
                    .build();
            animate.async().run();
        } catch (Exception e) {
            Log.w(TAG, "Animace se nezdarila (neni kriticke): " + e.getMessage());
        }
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
            Log.w(TAG, "Zadny TTS neni k dispozici - jen text na obrazovce");
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

    private void setState(ConversationState state) {
        this.currentState = state;
        boolean inputEnabled = (state == ConversationState.IDLE);
        etInput.setEnabled(inputEnabled);
        btnSend.setEnabled(inputEnabled);
        btnMic.setEnabled(inputEnabled);
        // Smazat chat je VZDY klikatelne - slouzi i jako nouzovy reset
        btnClear.setEnabled(true);
    }

    // ==================== QISDK LIFECYCLE ====================

    @Override
    public void onRobotFocusGained(QiContext qiContext) {
        this.qiContext = qiContext;
        Log.i(TAG, "Robot Focus GAINED");
        runOnUiThread(() -> {
            appendToChat("SYSTEM", "Robot Focus ziskan! Rubby pouziva svuj vlastni hlas a animace.");
            updateStatus("Rubby - Pripravena (QiSDK)", true);
        });
    }

    @Override
    public void onRobotFocusLost() {
        this.qiContext = null;
        Log.w(TAG, "Robot Focus LOST");
        runOnUiThread(() -> {
            appendToChat("SYSTEM", "Robot Focus ztracen. Pouzivam Android TTS.");
            updateStatus("Rubby - Pripravena (bez QiSDK)", true);
        });
    }

    @Override
    public void onRobotFocusRefused(String reason) {
        Log.w(TAG, "Robot Focus REFUSED: " + reason);
        runOnUiThread(() -> {
            appendToChat("SYSTEM", "Robot Focus odmitnut: " + reason + ". Funguju i bez nej.");
            updateStatus("Rubby - Pripravena (bez QiSDK)", true);
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

    // ==================== LIFECYCLE ====================

    @Override
    protected void onDestroy() {
        try {
            QiSDK.unregister(this, this);
        } catch (Exception e) {
            Log.w(TAG, "QiSDK unregister: " + e.getMessage());
        }

        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        if (androidTts != null) {
            androidTts.shutdown();
        }
        executor.shutdown();

        super.onDestroy();
    }
}
