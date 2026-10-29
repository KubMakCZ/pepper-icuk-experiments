package cz.kubmak.rubby_llm_exp.remote;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cz.kubmak.rubby_llm_exp.animation.AnimationManager;
import cz.kubmak.rubby_llm_exp.util.FileLogger;

/**
 * Vestaveny lehky HTTP server (port 8080) pro vzdalene ovladani Rubby z telefonu/notebooku
 * (webove rozhrani + REST API s CORS pro pripadnou Node.js aplikaci).
 */
public class RemoteControlServer {

    private static final String TAG = "RemoteControlServer";
    public static final int DEFAULT_PORT = 8080;

    public interface RemoteCommandHandler {
        String getCurrentStateName();
        boolean hasRobotFocus();
        int getVolumePercent();
        boolean isVisitorScreenActive();
        String getLastRubbyMessage();

        void onRemotePrompt(String prompt);
        void onRemoteSay(String text, String animationCategory);
        void onRemoteAnimate(String category);
        void onRemoteMove(double xMeters, double yMeters, double thetaRadians);
        void onRemoteStop();
        void onRemoteStartListening();
        void onRemoteSetScreen(boolean visitorMode);
        void onRemoteSetVolume(int percent);
    }

    private final int port;
    private final RemoteCommandHandler handler;
    private ServerSocket serverSocket;
    private volatile boolean running = false;
    private final ExecutorService serverExecutor = Executors.newCachedThreadPool();

    public RemoteControlServer(int port, RemoteCommandHandler handler) {
        this.port = port;
        this.handler = handler;
    }

    public void start() {
        if (running) return;
        running = true;
        serverExecutor.execute(() -> {
            try {
                serverSocket = new ServerSocket(port);
                FileLogger.getInstance(null).i(TAG, "RemoteControlServer bezi na portu " + port + " (IP: " + getLocalIpAddress() + ")");
                while (running && !serverSocket.isClosed()) {
                    try {
                        final Socket client = serverSocket.accept();
                        serverExecutor.execute(() -> handleClient(client));
                    } catch (IOException e) {
                        if (running) {
                            FileLogger.getInstance(null).w(TAG, "Chyba pri prijmu spojeni: " + e.getMessage());
                        }
                    }
                }
            } catch (IOException e) {
                FileLogger.getInstance(null).e(TAG, "Nelze spustit HTTP server na portu " + port + ": " + e.getMessage());
            }
        });
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException ignored) {}
        serverExecutor.shutdownNow();
    }

    private void handleClient(Socket client) {
        try {
            client.setSoTimeout(5000);
            BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
            OutputStream out = client.getOutputStream();

            String requestLine = reader.readLine();
            if (requestLine == null || requestLine.isEmpty()) {
                client.close();
                return;
            }

            String[] parts = requestLine.split(" ");
            String method = parts.length > 0 ? parts[0] : "GET";
            String fullPath = parts.length > 1 ? parts[1] : "/";

            int contentLength = 0;
            String headerLine;
            while ((headerLine = reader.readLine()) != null && !headerLine.isEmpty()) {
                if (headerLine.toLowerCase().startsWith("content-length:")) {
                    try {
                        contentLength = Integer.parseInt(headerLine.substring(15).trim());
                    } catch (NumberFormatException ignored) {}
                }
            }

            if ("OPTIONS".equalsIgnoreCase(method)) {
                sendResponse(out, "200 OK", "text/plain; charset=utf-8", "");
                client.close();
                return;
            }

            String body = "";
            if (contentLength > 0 && contentLength < 65536) {
                char[] buf = new char[contentLength];
                int read = 0;
                while (read < contentLength) {
                    int r = reader.read(buf, read, contentLength - read);
                    if (r == -1) break;
                    read += r;
                }
                body = new String(buf, 0, read);
            }

            String path = fullPath;
            String query = "";
            int qIdx = fullPath.indexOf('?');
            if (qIdx >= 0) {
                path = fullPath.substring(0, qIdx);
                query = fullPath.substring(qIdx + 1);
            }

            Map<String, String> params = parseQueryParams(query);
            if (!body.isEmpty() && body.contains("=")) {
                params.putAll(parseQueryParams(body));
            }

            if ("/".equals(path) || "/index.html".equals(path)) {
                sendResponse(out, "200 OK", "text/html; charset=utf-8", buildWebDashboardHtml());
            } else if ("/api/status".equals(path)) {
                sendResponse(out, "200 OK", "application/json; charset=utf-8", buildStatusJson());
            } else if ("/api/prompt".equals(path) || "/api/ask".equals(path)) {
                String prompt = params.get("prompt");
                if (prompt != null && !prompt.trim().isEmpty()) {
                    handler.onRemotePrompt(prompt.trim());
                    sendResponse(out, "200 OK", "application/json; charset=utf-8", "{\"ok\":true,\"action\":\"prompt\"}");
                } else {
                    sendResponse(out, "400 Bad Request", "application/json; charset=utf-8", "{\"ok\":false,\"error\":\"Chybi parametr prompt\"}");
                }
            } else if ("/api/say".equals(path)) {
                String text = params.get("text");
                String anim = params.get("anim");
                if (text != null && !text.trim().isEmpty()) {
                    handler.onRemoteSay(text.trim(), anim);
                    sendResponse(out, "200 OK", "application/json; charset=utf-8", "{\"ok\":true,\"action\":\"say\"}");
                } else {
                    sendResponse(out, "400 Bad Request", "application/json; charset=utf-8", "{\"ok\":false,\"error\":\"Chybi parametr text\"}");
                }
            } else if ("/api/animate".equals(path)) {
                String category = params.get("category");
                if (category != null && !category.trim().isEmpty()) {
                    handler.onRemoteAnimate(category.trim());
                    sendResponse(out, "200 OK", "application/json; charset=utf-8", "{\"ok\":true,\"action\":\"animate\",\"category\":\"" + escapeJson(category) + "\"}");
                } else {
                    sendResponse(out, "400 Bad Request", "application/json; charset=utf-8", "{\"ok\":false,\"error\":\"Chybi parametr category\"}");
                }
            } else if ("/api/move".equals(path)) {
                double x = parseDouble(params.get("x"), 0.0);
                double y = parseDouble(params.get("y"), 0.0);
                double theta = parseDouble(params.get("theta"), 0.0);
                handler.onRemoteMove(x, y, theta);
                sendResponse(out, "200 OK", "application/json; charset=utf-8", "{\"ok\":true,\"action\":\"move\"}");
            } else if ("/api/stop".equals(path)) {
                handler.onRemoteStop();
                sendResponse(out, "200 OK", "application/json; charset=utf-8", "{\"ok\":true,\"action\":\"stop\"}");
            } else if ("/api/listen".equals(path)) {
                handler.onRemoteStartListening();
                sendResponse(out, "200 OK", "application/json; charset=utf-8", "{\"ok\":true,\"action\":\"listen\"}");
            } else if ("/api/screen".equals(path)) {
                String mode = params.get("mode");
                boolean visitor = "visitor".equalsIgnoreCase(mode);
                handler.onRemoteSetScreen(visitor);
                sendResponse(out, "200 OK", "application/json; charset=utf-8", "{\"ok\":true,\"visitor\":" + visitor + "}");
            } else if ("/api/volume".equals(path)) {
                int percent = (int) parseDouble(params.get("percent"), 80);
                handler.onRemoteSetVolume(percent);
                sendResponse(out, "200 OK", "application/json; charset=utf-8", "{\"ok\":true,\"volume\":" + percent + "}");
            } else {
                sendResponse(out, "404 Not Found", "application/json; charset=utf-8", "{\"ok\":false,\"error\":\"Not found\"}");
            }

            client.close();
        } catch (Exception e) {
            FileLogger.getInstance(null).w(TAG, "Chyba pri obsluze HTTP klienta: " + e.getMessage());
            try {
                client.close();
            } catch (IOException ignored) {}
        }
    }

    private void sendResponse(OutputStream out, String status, String contentType, String content) throws IOException {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        String headers = "HTTP/1.1 " + status + "\r\n" +
                "Content-Type: " + contentType + "\r\n" +
                "Content-Length: " + bytes.length + "\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Access-Control-Allow-Methods: GET, POST, OPTIONS\r\n" +
                "Access-Control-Allow-Headers: Content-Type\r\n" +
                "Connection: close\r\n\r\n";
        out.write(headers.getBytes(StandardCharsets.UTF_8));
        out.write(bytes);
        out.flush();
    }

    private String buildStatusJson() {
        return "{" +
                "\"ok\":true," +
                "\"state\":\"" + escapeJson(handler.getCurrentStateName()) + "\"," +
                "\"robotFocus\":" + handler.hasRobotFocus() + "," +
                "\"volume\":" + handler.getVolumePercent() + "," +
                "\"visitorScreen\":" + handler.isVisitorScreenActive() + "," +
                "\"lastMessage\":\"" + escapeJson(handler.getLastRubbyMessage()) + "\"" +
                "}";
    }

    private static Map<String, String> parseQueryParams(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null || query.isEmpty()) return map;
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf('=');
            if (idx > 0) {
                try {
                    String k = URLDecoder.decode(pair.substring(0, idx), "UTF-8");
                    String v = URLDecoder.decode(pair.substring(idx + 1), "UTF-8");
                    map.put(k, v);
                } catch (Exception ignored) {}
            }
        }
        return map;
    }

    private static double parseDouble(String val, double def) {
        if (val == null || val.isEmpty()) return def;
        try {
            return Double.parseDouble(val);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }

    public static String getLocalIpAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface intf = interfaces.nextElement();
                Enumeration<InetAddress> addrs = intf.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if (!addr.isLoopbackAddress() && addr instanceof Inet4Address) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception ignored) {}
        return "127.0.0.1";
    }

    private String buildWebDashboardHtml() {
        StringBuilder animButtons = new StringBuilder();
        String[] keys = AnimationManager.getCategoryKeys();
        for (String key : keys) {
            animButtons.append("<button class='btn-anim' onclick=\"api('/api/animate?category=")
                    .append(key)
                    .append("')\">")
                    .append(key)
                    .append("</button>");
        }

        return "<!DOCTYPE html><html lang='cs'><head><meta charset='utf-8'>" +
                "<meta name='viewport' content='width=device-width,initial-scale=1,maximum-scale=1'>" +
                "<title>Rubby Dálkové Ovládání (ICUK)</title>" +
                "<style>" +
                "body{font-family:system-ui,-apple-system,sans-serif;background:#0f172a;color:#f8fafc;margin:0;padding:12px;}" +
                "h1{font-size:20px;margin:0 0 8px 0;color:#38bdf8;}" +
                ".card{background:#1e293b;border-radius:12px;padding:14px;margin-bottom:14px;box-shadow:0 4px 12px rgba(0,0,0,0.3);}" +
                ".card h2{font-size:16px;margin:0 0 10px 0;color:#94a3b8;text-transform:uppercase;letter-spacing:0.5px;}" +
                ".status-bar{display:flex;flex-wrap:wrap;gap:8px;align-items:center;justify-content:space-between;}" +
                ".badge{padding:6px 12px;border-radius:8px;font-weight:bold;background:#334155;}" +
                "input[type=text],textarea{width:100%;box-sizing:border-box;padding:12px;border-radius:8px;border:1px solid #475569;background:#0f172a;color:#fff;font-size:16px;margin-bottom:8px;}" +
                "button{border:none;border-radius:8px;padding:12px 16px;font-size:15px;font-weight:bold;color:#fff;cursor:pointer;touch-action:manipulation;}" +
                "button:active{opacity:0.8;transform:scale(0.98);}" +
                ".btn-primary{background:#2563eb;width:100%;margin-bottom:8px;}" +
                ".btn-green{background:#16a34a;width:100%;margin-bottom:8px;}" +
                ".btn-stop{background:#dc2626;width:100%;font-size:17px;padding:14px;}" +
                ".preset-grid{display:grid;grid-template-columns:1fr 1fr;gap:8px;margin-top:6px;}" +
                ".btn-preset{background:#4f46e5;font-size:13px;padding:10px;}" +
                ".dpad{display:grid;grid-template-columns:1fr 1fr 1fr;gap:8px;text-align:center;margin-bottom:10px;}" +
                ".btn-move{background:#0284c7;padding:16px 8px;font-size:15px;}" +
                ".anim-grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(95px,1fr));gap:6px;}" +
                ".btn-anim{background:#334155;font-size:13px;padding:10px 6px;}" +
                "</style></head><body>" +
                "<div class='card'><div class='status-bar'>" +
                "<h1>🤖 Rubby Dálkové Ovládání (ICUK)</h1>" +
                "<span id='stBadge' class='badge'>Stav: ...</span>" +
                "</div><div id='lastMsg' style='font-size:14px;color:#cbd5e1;margin-top:8px;'>Poslední odpověď: -</div>" +
                "<div style='display:grid;grid-template-columns:1fr 1fr 1fr;gap:6px;margin-top:10px;'>" +
                "<button style='background:#0d9488;font-size:13px;' onclick=\"api('/api/listen')\">🎤 Zapnout MIC</button>" +
                "<button style='background:#0369a1;font-size:13px;' onclick=\"api('/api/screen?mode=visitor')\">👁️ Prezentace</button>" +
                "<button style='background:#475569;font-size:13px;' onclick=\"api('/api/screen?mode=admin')\">⚙️ Admin/Chat</button>" +
                "</div></div>" +

                "<div class='card'><h2>🧠 1. Poslat AI Prompt (Rubby sama vymyslí a řekne odpověď)</h2>" +
                "<input id='inpPrompt' type='text' placeholder='Např.: Představ se divákům a řekni zahajovací řeč...'>" +
                "<button class='btn-primary' onclick='sendPrompt()'>🧠 Odeslat prompt do AI (LLM)</button>" +
                "<div class='preset-grid'>" +
                "<button class='btn-preset' onclick=\"quickPrompt('Přivítej diváky na dnešní akci v Inovačním centru Ústeckého kraje, představ se jako robotka Rubby a popřej všem skvělý den plný inovací.')\">🎬 Zahajovací řeč ICUK</button>" +
                "<button class='btn-preset' onclick=\"quickPrompt('Představ se publiku, řekni kdo jsi, co všechno umíš a přátelsky všem zamávej.')\">👋 Představení na pódiu</button>" +
                "<button class='btn-preset' onclick=\"quickPrompt('Řekni krátký vtipný postřeh o tom, jaké to je být humanoidní robotkou v Ústí nad Labem, a zasměj se.')\">😄 Řekni vtip o robotech</button>" +
                "<button class='btn-preset' onclick=\"quickPrompt('Poděkuj publiku za pozornost, rozluč se a popřej jim šťastnou cestu.')\">🎉 Závěrečné poděkování</button>" +
                "</div></div>" +

                "<div class='card'><h2>🗣️ 2. Říct přesnou větu (Teleprompter bez AI prodlevy)</h2>" +
                "<input id='inpSay' type='text' placeholder='Napiš přesný text, který má Rubby ihned vyslovit...'>" +
                "<button class='btn-green' onclick='sendSay()'>🗣️ Říct přesně tento text hned</button>" +
                "</div>" +

                "<div class='card'><h2>🕹️ 3. Pojezd a otáčení po pódiu</h2>" +
                "<div class='dpad'>" +
                "<button class='btn-move' onclick=\"move(0,0,0.785)\">↩️ Otočit L 45°</button>" +
                "<button class='btn-move' onclick=\"move(0.45,0,0)\">⬆️ Dopředu 45cm</button>" +
                "<button class='btn-move' onclick=\"move(0,0,-0.785)\">↪️ Otočit P 45°</button>" +
                "<button class='btn-move' onclick=\"move(0,0.3,0)\">⬅️ Úkrok vlevo</button>" +
                "<button class='btn-stop' onclick=\"api('/api/stop')\">🛑 STOP</button>" +
                "<button class='btn-move' onclick=\"move(0,-0.3,0)\">➡️ Úkrok vpravo</button>" +
                "<button class='btn-move' onclick=\"move(0,0,1.57)\">↩️ Otočit L 90°</button>" +
                "<button class='btn-move' onclick=\"move(-0.35,0,0)\">⬇️ Couvnout 35cm</button>" +
                "<button class='btn-move' onclick=\"move(0,0,-1.57)\">↪️ Otočit P 90°</button>" +
                "</div>" +
                "<div style='display:flex;align-items:center;gap:10px;margin-top:8px;'>" +
                "<span>🔊 Hlasitost:</span>" +
                "<input type='range' min='0' max='100' value='80' id='volRange' style='flex:1;' onchange='setVol(this.value)'>" +
                "<span id='volVal'>80%</span>" +
                "</div></div>" +

                "<div class='card'><h2>🎭 4. Okamžité spuštění animace (44 kategorií)</h2>" +
                "<div class='anim-grid'>" + animButtons.toString() + "</div></div>" +

                "<script>" +
                "function api(url){fetch(url,{method:'POST'}).then(()=>refreshStatus());}" +
                "function sendPrompt(){var v=document.getElementById('inpPrompt').value;if(v){api('/api/prompt?prompt='+encodeURIComponent(v));document.getElementById('inpPrompt').value='';}}" +
                "function quickPrompt(t){api('/api/prompt?prompt='+encodeURIComponent(t));}" +
                "function sendSay(){var v=document.getElementById('inpSay').value;if(v){api('/api/say?text='+encodeURIComponent(v));document.getElementById('inpSay').value='';}}" +
                "function move(x,y,th){api('/api/move?x='+x+'&y='+y+'&theta='+th);}" +
                "function setVol(v){document.getElementById('volVal').innerText=v+'%';api('/api/volume?percent='+v);}" +
                "function refreshStatus(){fetch('/api/status').then(r=>r.json()).then(d=>{" +
                "document.getElementById('stBadge').innerText='Stav: '+d.state+(d.robotFocus?' (QiSDK)':' (Bez Focusu)');" +
                "if(d.lastMessage)document.getElementById('lastMsg').innerText='Poslední odpověď: '+d.lastMessage;" +
                "}).catch(()=>{});}" +
                "setInterval(refreshStatus,2000);refreshStatus();" +
                "</script></body></html>";
    }
}
