package cz.kubmak.rubby_llm_exp.util;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * File logger pro Rubby — zapisuje logy do souboru pro budoucí debugování.
 *
 * Vlastnosti:
 * - Automaticky se roztaví (max 3 soubory po 500 KB každý)
 * - Zapisuje na background threadu (neblokuje UI)
 * - Ukládá do /sdcard/RubbyLogs/ (přístupný přes ADB nebo Android Studio Device File Explorer)
 * - Prefixy: V=VERBOSE, D=DEBUG, I=INFO, W=WARN, E=ERROR
 */
public class FileLogger {

    private static final String TAG = "FileLogger";
    private static final String LOG_DIR_NAME = "RubbyLogs";
    private static final long MAX_FILE_SIZE_BYTES = 512 * 1024; // 500 KB
    private static final int MAX_LOG_FILES = 3;

    private final Context context;
    private final ExecutorService writeExecutor = Executors.newSingleThreadExecutor();
    private File currentLogFile;
    private long currentFileSize = 0;

    private static volatile FileLogger INSTANCE;

    public static FileLogger getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (FileLogger.class) {
                if (INSTANCE == null) {
                    INSTANCE = new FileLogger(context.getApplicationContext());
                }
            }
        }
        return INSTANCE;
    }

    private FileLogger(Context context) {
        this.context = context;
        tryInitLogDirectory();
    }

    private boolean tryInitLogDirectory() {
        if (currentLogFile != null) return true; // Už je připraveno
        File logDir = new File(context.getExternalFilesDir(null), LOG_DIR_NAME);
        if (!logDir.exists()) {
            if (!logDir.mkdirs()) {
                return false; // Nelze vytvořit složku, zřejmě stále chybí práva
            }
        }
        currentLogFile = getNextLogFile(logDir);
        currentFileSize = currentLogFile.exists() ? currentLogFile.length() : 0;
        return true;
    }

    private File getNextLogFile(File logDir) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(new Date());
        return new File(logDir, "rubby_log_" + timestamp + ".txt");
    }

    /**
     * Zapsane log do souboru (async).
     */
    public void log(int priority, String tag, String message) {
        writeExecutor.execute(() -> {
            if (!tryInitLogDirectory()) {
                Log.e(TAG, "Nelze zapisovat logy - chybí práva nebo adresář.");
                return;
            }
            try {
                // Rotation check
                if (currentFileSize > MAX_FILE_SIZE_BYTES) {
                    rotateLog();
                }

                String prefix = getPriorityPrefix(priority);
                String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(new Date());
                String line = String.format("[%s] %s/%s: %s\n", timestamp, prefix, tag, message);

                FileWriter writer = new FileWriter(currentLogFile, true);
                writer.append(line);
                writer.flush();
                writer.close();

                currentFileSize += line.length();
            } catch (IOException e) {
                Log.e(TAG, "Chyba zapisu logu: " + e.getMessage());
                // Pokud selže přístup, resetuj currentLogFile, aby to zkusil příště znovu
                currentLogFile = null;
            }
        });
    }

    private void rotateLog() {
        File logDir = currentLogFile.getParentFile();
        if (logDir == null) return;

        // Smaž nejstarší soubor pokud máme přes LIMIT
        File[] logFiles = logDir.listFiles((dir, name) -> name.startsWith("rubby_log_"));
        if (logFiles != null && logFiles.length >= MAX_LOG_FILES) {
            // Najdi nejstarší (podle času modifikace)
            File oldest = logFiles[0];
            for (File f : logFiles) {
                if (f.lastModified() < oldest.lastModified()) {
                    oldest = f;
                }
            }
            oldest.delete();
            Log.d(TAG, "Smazan starý log: " + oldest.getName());
        }

        currentLogFile = getNextLogFile(logDir);
        currentFileSize = 0;
    }

    private String getPriorityPrefix(int priority) {
        switch (priority) {
            case Log.VERBOSE: return "V";
            case Log.DEBUG:   return "D";
            case Log.INFO:    return "I";
            case Log.WARN:    return "W";
            case Log.ERROR:   return "E";
            default:          return "?";
        }
    }

    /**
     * Vrací cestu k aktuálnímu log souboru.
     */
    public String getCurrentLogPath() {
        return currentLogFile != null ? currentLogFile.getAbsolutePath() : null;
    }

    /**
     * Vrací seznam všech log souborů.
     */
    public File[] getLogFiles() {
        if (currentLogFile == null) return new File[0];
        File logDir = currentLogFile.getParentFile();
        if (logDir == null) return new File[0];
        return logDir.listFiles((dir, name) -> name.startsWith("rubby_log_"));
    }

    // ===== Convenience methods (mirror Log.x) =====

    public void v(String tag, String msg) { 
        Log.v(tag, msg);
        log(Log.VERBOSE, tag, msg); 
    }
    public void d(String tag, String msg) { 
        Log.d(tag, msg);
        log(Log.DEBUG, tag, msg); 
    }
    public void i(String tag, String msg) { 
        Log.i(tag, msg);
        log(Log.INFO, tag, msg); 
    }
    public void w(String tag, String msg) { 
        Log.w(tag, msg);
        log(Log.WARN, tag, msg); 
    }
    public void e(String tag, String msg) { 
        Log.e(tag, msg);
        log(Log.ERROR, tag, msg); 
    }
    public void e(String tag, String msg, Throwable tr) {
        Log.e(tag, msg, tr);
        log(Log.ERROR, tag, msg + "\n" + Log.getStackTraceString(tr));
    }

    public void shutdown() {
        writeExecutor.shutdown();
    }
}
