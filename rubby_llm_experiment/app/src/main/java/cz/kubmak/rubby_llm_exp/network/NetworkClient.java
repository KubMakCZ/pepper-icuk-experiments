package cz.kubmak.rubby_llm_exp.network;

import android.os.Build;
import android.util.Log;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

/**
 * Singleton poskytujici nakonfigurovany OkHttpClient s TLS 1.2 patchem
 * pro Android API 23.
 */
public class NetworkClient {

    private static final String TAG = "NetworkClient";
    private static OkHttpClient instance;

    private NetworkClient() {}

    public static synchronized OkHttpClient getInstance() {
        if (instance == null) {
            instance = createClient();
        }
        return instance;
    }

    private static OkHttpClient createClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS);

        // TLS 1.2 patch pro Android < 7.0 (API < 24)
        if (Build.VERSION.SDK_INT < 24) {
            try {
                SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
                sslContext.init(null, null, null);
                Tls12SocketFactory tlsFactory = new Tls12SocketFactory(sslContext.getSocketFactory());
                builder.sslSocketFactory(tlsFactory);
                Log.i(TAG, "TLS 1.2 patch aplikovan pro API " + Build.VERSION.SDK_INT);
            } catch (NoSuchAlgorithmException | KeyManagementException e) {
                Log.e(TAG, "Chyba pri nastaveni TLS 1.2: " + e.getMessage());
            }
        }

        // Logging pro debug (v produkci prepnout na NONE)
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);
        builder.addInterceptor(logging);

        return builder.build();
    }
}
