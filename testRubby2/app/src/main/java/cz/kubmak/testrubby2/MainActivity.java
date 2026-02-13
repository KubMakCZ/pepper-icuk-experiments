package cz.kubmak.testrubby2;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;

import com.aldebaran.qi.sdk.QiContext;
import com.aldebaran.qi.sdk.QiSDK;
import com.aldebaran.qi.sdk.RobotLifecycleCallbacks;
import com.aldebaran.qi.sdk.builder.AnimateBuilder;
import com.aldebaran.qi.sdk.builder.AnimationBuilder;
import com.aldebaran.qi.sdk.builder.ChatBuilder;
import com.aldebaran.qi.sdk.builder.QiChatbotBuilder;
import com.aldebaran.qi.sdk.builder.SayBuilder;
import com.aldebaran.qi.sdk.builder.TopicBuilder;
import com.aldebaran.qi.sdk.object.actuation.Animate;
import com.aldebaran.qi.sdk.object.actuation.Animation;
import com.aldebaran.qi.sdk.object.conversation.Bookmark;
import com.aldebaran.qi.sdk.object.conversation.Chat;
import com.aldebaran.qi.sdk.object.conversation.QiChatbot;
import com.aldebaran.qi.sdk.object.conversation.Say;
import com.aldebaran.qi.sdk.object.conversation.Topic;
import com.aldebaran.qi.sdk.object.locale.Language;
import com.aldebaran.qi.sdk.object.locale.Locale;
import com.aldebaran.qi.sdk.object.locale.Region;

import java.util.Map;

public class MainActivity extends AppCompatActivity implements RobotLifecycleCallbacks {

    private static final String TAG = "PepperHybrid";
    private QiContext qiContext;
    private Chat chat;
    private QiChatbot qiChatbot;

    // Detekce, zda běžíme v emulátoru
    private boolean isEmulator = Build.FINGERPRINT.contains("generic") 
            || Build.MODEL.contains("Emulator") 
            || Build.MODEL.contains("Android SDK built for x86");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Tlačítka fungují vždy
        findViewById(R.id.btn_say_hello).setOnClickListener(v -> {
            if (qiContext != null) {
                saySomething("Ahoj! Zdravím tě z " + (isEmulator ? "simulátoru" : "reálného robota") + ".");
            }
        });

        findViewById(R.id.btn_dance).setOnClickListener(v -> {
            if (qiContext != null) {
                runAnimation(R.raw.dance_b003);
            }
        });

        QiSDK.register(this, this);
    }

    @Override
    public void onRobotFocusGained(QiContext qiContext) {
        this.qiContext = qiContext;
        Log.i(TAG, "Robot focus gained.");

        if (isEmulator) {
            Log.w(TAG, "EMULÁTOR DETEKOVÁN: Chat (poslech) je vypnut pro stabilitu. Používej tlačítka.");
            saySomething("Ahoj! V simulátoru mě ovládej tlačítky.");
        } else {
            Log.i(TAG, "REÁLNÝ ROBOT: Inicializuji plnou hlasovou konverzaci.");
            initializeFullChat(qiContext);
        }
    }

    private void initializeFullChat(QiContext qiContext) {
        Locale locale = new Locale(Language.CZECH, Region.CZECH_REPUBLIC);

        TopicBuilder.with(qiContext)
                .withResource(R.raw.conversation)
                .buildAsync()
                .andThenCompose(topic -> {
                    qiChatbot = QiChatbotBuilder.with(qiContext)
                            .withTopic(topic)
                            .withLocale(locale)
                            .build();

                    Map<String, Bookmark> bookmarks = topic.getBookmarks();
                    final Bookmark danceBookmark = bookmarks.get("dance");
                    if (danceBookmark != null) {
                        qiChatbot.addOnBookmarkReachedListener(bookmark -> {
                            if (bookmark.getName().equals(danceBookmark.getName())) {
                                runAnimation(R.raw.dance_b003);
                            }
                        });
                    }

                    return ChatBuilder.with(qiContext)
                            .withChatbot(qiChatbot)
                            .withLocale(locale)
                            .buildAsync();
                })
                .andThenConsume(chatAction -> {
                    this.chat = chatAction;
                    chat.async().run();
                    Log.i(TAG, "Chat spuštěn.");
                });
    }

    @Override
    public void onRobotFocusLost() {
        this.qiContext = null;
        Log.i(TAG, "Robot focus lost.");
    }

    @Override
    public void onRobotFocusRefused(String reason) {
        Log.e(TAG, "Robot focus refused: " + reason);
    }

    @Override
    protected void onDestroy() {
        QiSDK.unregister(this, this);
        super.onDestroy();
    }

    private void saySomething(String text) {
        if (qiContext == null) return;
        SayBuilder.with(qiContext)
                .withText(text)
                .withLocale(new Locale(Language.CZECH, Region.CZECH_REPUBLIC))
                .buildAsync()
                .andThenConsume(say -> say.async().run());
    }

    private void runAnimation(int animationResId) {
        if (qiContext == null) return;
        AnimationBuilder.with(qiContext)
                .withResources(animationResId)
                .buildAsync()
                .andThenCompose(animation -> AnimateBuilder.with(qiContext)
                        .withAnimation(animation)
                        .buildAsync())
                .andThenConsume(animate -> animate.async().run());
    }
}
