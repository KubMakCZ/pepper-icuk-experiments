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
 * (webove rozhrani s realtime virtualnim joystickem + REST API s CORS pro pripadnou Node.js aplikaci).
 */
public class RemoteControlServer {

    private static final String TAG = "RemoteControlServer";
    public static final int DEFAULT_PORT = 8080;

    public interface RemoteCommandHandler {
        String getCurrentStateName();
        boolean hasRobotFocus();
        boolean isChargingFlapOpen();
        boolean isFaceTrackingEnabled();
        boolean isNarrowSpaceMode();
        int getVolumePercent();
        boolean isVisitorScreenActive();
        String getLastRubbyMessage();

        void onRemotePrompt(String prompt);
        void onRemoteSay(String text, String animationCategory);
        void onRemoteAnimate(String category);
        void onRemoteMove(double xMeters, double yMeters, double thetaRadians);
        void onRemoteJoystick(double normX, double normY, double normTheta);
        void onRemoteStopMove();
        void onRemoteStop();
        void onRemoteStartListening();
        void onRemoteSetScreen(boolean visitorMode);
        void onRemoteSetVolume(int percent);
        void onRemoteSetFaceTracking(boolean enabled);
        void onRemoteSetNarrowSpace(boolean narrow);
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
            } else if ("/api/joystick".equals(path)) {
                double x = parseDouble(params.get("x"), 0.0);
                double y = parseDouble(params.get("y"), 0.0);
                double theta = parseDouble(params.get("theta"), 0.0);
                handler.onRemoteJoystick(x, y, theta);
                sendResponse(out, "200 OK", "application/json; charset=utf-8", "{\"ok\":true,\"action\":\"joystick\"}");
            } else if ("/api/stop_move".equals(path)) {
                handler.onRemoteStopMove();
                sendResponse(out, "200 OK", "application/json; charset=utf-8", "{\"ok\":true,\"action\":\"stop_move\"}");
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
            } else if ("/api/awareness".equals(path)) {
                boolean enabled = "true".equalsIgnoreCase(params.get("enabled")) || "1".equals(params.get("enabled"));
                handler.onRemoteSetFaceTracking(enabled);
                sendResponse(out, "200 OK", "application/json; charset=utf-8", "{\"ok\":true,\"faceTracking\":" + enabled + "}");
            } else if ("/api/obstacles".equals(path)) {
                boolean narrow = "true".equalsIgnoreCase(params.get("narrow")) || "1".equals(params.get("narrow"));
                handler.onRemoteSetNarrowSpace(narrow);
                sendResponse(out, "200 OK", "application/json; charset=utf-8", "{\"ok\":true,\"narrowSpace\":" + narrow + "}");
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
                "\"chargingFlapOpen\":" + handler.isChargingFlapOpen() + "," +
                "\"faceTracking\":" + handler.isFaceTrackingEnabled() + "," +
                "\"narrowSpace\":" + handler.isNarrowSpaceMode() + "," +
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
        String wlanIp = null;
        String fallbackIp = null;
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface intf = interfaces.nextElement();
                String ifName = intf.getName() != null ? intf.getName().toLowerCase() : "";
                Enumeration<InetAddress> addrs = intf.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if (!addr.isLoopbackAddress() && addr instanceof Inet4Address) {
                        String ip = addr.getHostAddress();
                        FileLogger.getInstance(null).i("RubbyLLM", "Detekovane sitove rozhrani [" + ifName + "] -> IP: " + ip);
                        // Ignoruj interni sit mezi tabletem a hlavou Peppera (198.18.0.x)
                        if (ip != null && ip.startsWith("198.18.")) {
                            continue;
                        }
                        // Uprednostni Wi-Fi rozhrani (wlan0)
                        if (ifName.startsWith("wlan") || ifName.startsWith("ap")) {
                            wlanIp = ip;
                        } else if (fallbackIp == null) {
                            fallbackIp = ip;
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        String selected = wlanIp != null ? wlanIp : (fallbackIp != null ? fallbackIp : "127.0.0.1");
        FileLogger.getInstance(null).i("RubbyLLM", "Vybrana IP adresa pro Web ovladac: http://" + selected + ":" + DEFAULT_PORT);
        return selected;
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
                "<meta name='viewport' content='width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no'>" +
                "<title>Rubby Dálkové Ovládání (ICUK)</title>" +
                "<style>" +
                "body{font-family:system-ui,-apple-system,sans-serif;background:#0f172a;color:#f8fafc;margin:0;padding:12px;overscroll-behavior:contain;}" +
                "h1{font-size:20px;margin:0 0 8px 0;color:#38bdf8;}" +
                ".card{background:#1e293b;border-radius:12px;padding:14px;margin-bottom:14px;box-shadow:0 4px 12px rgba(0,0,0,0.3);}" +
                ".card h2{font-size:15px;margin:0 0 10px 0;color:#94a3b8;text-transform:uppercase;letter-spacing:0.5px;}" +
                ".status-bar{display:flex;flex-wrap:wrap;gap:8px;align-items:center;justify-content:space-between;}" +
                ".badge{padding:6px 12px;border-radius:8px;font-weight:bold;background:#334155;}" +
                "input[type=text],textarea{width:100%;box-sizing:border-box;padding:12px;border-radius:8px;border:1px solid #475569;background:#0f172a;color:#fff;font-size:16px;margin-bottom:8px;}" +
                "button{border:none;border-radius:8px;padding:12px 14px;font-size:14px;font-weight:bold;color:#fff;cursor:pointer;touch-action:manipulation;}" +
                "button:active{opacity:0.8;transform:scale(0.98);}" +
                ".btn-primary{background:#2563eb;width:100%;margin-bottom:8px;}" +
                ".btn-green{background:#16a34a;width:100%;margin-bottom:8px;}" +
                ".btn-stop{background:#dc2626;width:100%;font-size:16px;padding:14px;}" +
                ".preset-grid{display:grid;grid-template-columns:1fr 1fr;gap:8px;margin-top:6px;}" +
                ".btn-preset{background:#4f46e5;font-size:13px;padding:10px;}" +
                ".joy-wrap{display:flex;flex-wrap:wrap;justify-content:space-around;align-items:center;gap:14px;margin:10px 0;user-select:none;-webkit-user-select:none;}" +
                ".joy-box{display:flex;flex-direction:column;align-items:center;}" +
                ".joy-label{font-size:13px;color:#cbd5e1;margin-bottom:6px;font-weight:bold;}" +
                ".joy-zone{width:170px;height:170px;border-radius:50%;background:radial-gradient(circle,#334155 0%,#0f172a 100%);border:3px solid #38bdf8;position:relative;touch-action:none;}" +
                ".joy-CrossH{position:absolute;top:50%;left:10%;right:10%;height:1px;background:rgba(255,255,255,0.15);pointer-events:none;}" +
                ".joy-CrossV{position:absolute;left:50%;top:10%;bottom:10%;width:1px;background:rgba(255,255,255,0.15);pointer-events:none;}" +
                ".joy-stick{width:64px;height:64px;border-radius:50%;background:radial-gradient(circle,#60a5fa 0%,#2563eb 100%);box-shadow:0 4px 10px rgba(0,0,0,0.5);position:absolute;top:53px;left:53px;pointer-events:none;transition:transform 0.05s linear;}" +
                ".dpad{display:grid;grid-template-columns:1fr 1fr 1fr;gap:8px;text-align:center;margin-top:12px;}" +
                ".btn-move{background:#0284c7;padding:12px 6px;font-size:13px;}" +
                ".anim-grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(95px,1fr));gap:6px;}" +
                ".btn-anim{background:#334155;font-size:13px;padding:10px 6px;}" +
                "</style></head><body>" +
                "<div class='card'><div class='status-bar'>" +
                "<h1>🤖 Rubby Dálkové Ovládání (ICUK)</h1>" +
                "<span id='stBadge' class='badge'>Stav: ...</span>" +
                "</div>" +
                "<div id='flapWarn' style='display:none;background:#b91c1c;color:#fff;padding:12px;border-radius:8px;margin-top:10px;font-weight:bold;font-size:15px;border:2px solid #fca5a5;'>" +
                "⚠️ POZOR: Napájecí port / dvířka vzadu dole jsou OTEVŘENÁ! Motory kol jsou zablokované a Rubby se nemůže pohybovat." +
                "</div>" +
                "<div id='lastMsg' style='font-size:14px;color:#cbd5e1;margin-top:8px;'>Poslední odpověď: -</div>" +
                "<div style='display:grid;grid-template-columns:1fr 1fr 1fr;gap:6px;margin-top:10px;'>" +
                "<button style='background:#0d9488;font-size:13px;' onclick=\"api('/api/listen')\">🎤 Zapnout MIC</button>" +
                "<button style='background:#0369a1;font-size:13px;' onclick=\"api('/api/screen?mode=visitor')\">▶ Prezentace</button>" +
                "<button style='background:#475569;font-size:13px;' onclick=\"api('/api/screen?mode=admin')\">⚙ Admin/Chat</button>" +
                "</div></div>" +

                "<div class='card'><h2>🕹️ 1. Realtime Virtuální Joysticky (Drž a táhni prstem)</h2>" +
                "<div style='display:grid;grid-template-columns:1fr 1fr;gap:8px;margin-bottom:10px;'>" +
                "<button id='btnFaceTrack' style='background:#0284c7;font-size:13px;padding:10px;' onclick='toggleFaceTrack()'>👀 Sledování obličejů: ZAP</button>" +
                "<button id='btnNarrowSpace' style='background:#475569;font-size:13px;padding:10px;' onclick='toggleNarrowSpace()'>🛡️ Odstup: BĚŽNÝ (35 cm)</button>" +
                "</div>" +
                "<div class='joy-wrap'>" +
                "<div class='joy-box'>" +
                "<div class='joy-label'>LEVÝ: Pohyb (Dopředu / Dozadu / Do stran)</div>" +
                "<div id='joyDrive' class='joy-zone'><div class='joy-CrossH'></div><div class='joy-CrossV'></div><div id='stickDrive' class='joy-stick'></div></div>" +
                "</div>" +
                "<div class='joy-box'>" +
                "<div class='joy-label'>PRAVÝ: Čisté otáčení (Doleva / Doprava)</div>" +
                "<div id='joyStrafe' class='joy-zone' style='border-color:#a855f7;'><div class='joy-CrossH'></div><div class='joy-CrossV'></div><div id='stickStrafe' class='joy-stick' style='background:radial-gradient(circle,#c084fc 0%,#7e22ce 100%);'></div></div>" +
                "</div>" +
                "</div>" +
                "<div id='joyReadout' style='text-align:center;font-size:13px;color:#38bdf8;margin-bottom:8px;'>Joystick v klidu (pusť prst pro okamžité zastavení)</div>" +
                "<button class='btn-stop' onclick=\"api('/api/stop')\">🛑 NOUZOVÝ STOP (Zastavit pohyb i řeč)</button>" +
                "<details style='margin-top:10px;'><summary style='cursor:pointer;color:#94a3b8;font-size:13px;'>Zobrazit kroková tlačítka (přesné otočení 45° / 90°)</summary>" +
                "<div class='dpad'>" +
                "<button class='btn-move' onclick=\"move(0,0,0.785)\">↩️ Otočit L 45°</button>" +
                "<button class='btn-move' onclick=\"move(0.45,0,0)\">⬆️ Dopředu 45cm</button>" +
                "<button class='btn-move' onclick=\"move(0,0,-0.785)\">↪️ Otočit P 45°</button>" +
                "<button class='btn-move' onclick=\"move(0,0.3,0)\">⬅️ Úkrok vlevo</button>" +
                "<button class='btn-move' style='background:#dc2626;' onclick=\"api('/api/stop_move')\">⏹ Zastavit pojezd</button>" +
                "<button class='btn-move' onclick=\"move(0,-0.3,0)\">➡️ Úkrok vpravo</button>" +
                "<button class='btn-move' onclick=\"move(0,0,1.57)\">↩️ Otočit L 90°</button>" +
                "<button class='btn-move' onclick=\"move(-0.35,0,0)\">⬇️ Couvnout 35cm</button>" +
                "<button class='btn-move' onclick=\"move(0,0,-1.57)\">↪️ Otočit P 90°</button>" +
                "</div></details>" +
                "<div style='display:flex;align-items:center;gap:10px;margin-top:12px;'>" +
                "<span>🔊 Hlasitost:</span>" +
                "<input type='range' min='0' max='100' value='80' id='volRange' style='flex:1;height:28px;' onchange='setVol(this.value)'>" +
                "<span id='volVal'>80%</span>" +
                "</div></div>" +

                "<div class='card'><h2>🧠 2. Poslat AI Prompt (Rubby sama vymyslí a řekne odpověď)</h2>" +
                "<input id='inpPrompt' type='text' placeholder='Např.: Představ se divákům a řekni zahajovací řeč...'>" +
                "<button class='btn-primary' onclick='sendPrompt()'>🧠 Odeslat prompt do AI (LLM)</button>" +
                "<div class='preset-grid'>" +
                "<button class='btn-preset' onclick=\"quickPrompt('Přivítej diváky na dnešní akci v Inovačním centru Ústeckého kraje, představ se jako robotka Rubby a popřej všem skvělý den plný inovací.')\">🎬 Zahajovací řeč ICUK</button>" +
                "<button class='btn-preset' onclick=\"quickPrompt('Představ se publiku, řekni kdo jsi, co všechno umíš a přátelsky všem zamávej.')\">👋 Představení na pódiu</button>" +
                "<button class='btn-preset' onclick=\"quickPrompt('Řekni krátký vtipný postřeh o tom, jaké to je být humanoidní robotkou v Ústí nad Labem, a zasměj se.')\">😄 Řekni vtip o robotech</button>" +
                "<button class='btn-preset' onclick=\"quickPrompt('Poděkuj publiku za pozornost, rozluč se a popřej jim šťastnou cestu.')\">🎉 Závěrečné poděkování</button>" +
                "</div></div>" +

                "<div class='card'><h2>🗣️ 3. Říct přesnou větu (Teleprompter bez AI prodlevy)</h2>" +
                "<input id='inpSay' type='text' placeholder='Napiš přesný text, který má Rubby ihned vyslovit...'>" +
                "<button class='btn-green' onclick='sendSay()'>🗣️ Říct přesně tento text hned</button>" +
                "</div>" +

                "<div class='card'><h2>🎭 4. Okamžité spuštění animace (44 kategorií)</h2>" +
                "<div class='anim-grid'>" + animButtons.toString() + "</div></div>" +

                "<script>" +
                "function api(url){fetch(url,{method:'POST'}).then(()=>refreshStatus()).catch(()=>{});}" +
                "function sendPrompt(){var v=document.getElementById('inpPrompt').value;if(v){api('/api/prompt?prompt='+encodeURIComponent(v));document.getElementById('inpPrompt').value='';}}" +
                "function quickPrompt(t){api('/api/prompt?prompt='+encodeURIComponent(t));}" +
                "function sendSay(){var v=document.getElementById('inpSay').value;if(v){api('/api/say?text='+encodeURIComponent(v));document.getElementById('inpSay').value='';}}" +
                "function move(x,y,th){api('/api/move?x='+x+'&y='+y+'&theta='+th);}" +
                "function setVol(v){document.getElementById('volVal').innerText=v+'%';api('/api/volume?percent='+v);}" +

                // Virtual Joystick implementation
                "function setupJoystick(zoneId,stickId,onMove,onRelease){" +
                "var zone=document.getElementById(zoneId),stick=document.getElementById(stickId);" +
                "var active=false,timer=null,curNx=0,curNy=0,maxR=53;" +
                "function updatePos(clientX,clientY){" +
                "var rect=zone.getBoundingClientRect();" +
                "var cx=rect.left+rect.width/2,cy=rect.top+rect.height/2;" +
                "var dx=clientX-cx,dy=clientY-cy;" +
                "var dist=Math.sqrt(dx*dx+dy*dy);" +
                "if(dist>maxR){dx=dx*maxR/dist;dy=dy*maxR/dist;}" +
                "stick.style.transform='translate('+dx.toFixed(1)+'px,'+dy.toFixed(1)+'px)';" +
                "curNx=dx/maxR;curNy=-dy/maxR;" + // curNy > 0 je dopredu (nahoru), curNx > 0 je doprava
                "}" +
                "function start(x,y){active=true;updatePos(x,y);onMove(curNx,curNy);" +
                "if(timer)clearInterval(timer);" +
                "timer=setInterval(function(){if(active)onMove(curNx,curNy);},300);" +
                "}" +
                "function end(){if(!active)return;active=false;if(timer){clearInterval(timer);timer=null;}" +
                "stick.style.transform='translate(0px,0px)';curNx=0;curNy=0;onRelease();}" +
                "zone.addEventListener('touchstart',function(e){e.preventDefault();var t=e.changedTouches[0];start(t.clientX,t.clientY);},{passive:false});" +
                "zone.addEventListener('touchmove',function(e){e.preventDefault();if(!active)return;var t=e.changedTouches[0];updatePos(t.clientX,t.clientY);},{passive:false});" +
                "zone.addEventListener('touchend',function(e){e.preventDefault();end();},{passive:false});" +
                "zone.addEventListener('touchcancel',function(e){e.preventDefault();end();},{passive:false});" +
                "zone.addEventListener('mousedown',function(e){e.preventDefault();start(e.clientX,e.clientY);});" +
                "window.addEventListener('mousemove',function(e){if(active)updatePos(e.clientX,e.clientY);});" +
                "window.addEventListener('mouseup',function(){if(active)end();});" +
                "}" +

                "var curFaceTrack=true,curNarrowSpace=false;" +
                "function toggleFaceTrack(){curFaceTrack=!curFaceTrack;api('/api/awareness?enabled='+curFaceTrack);}" +
                "function toggleNarrowSpace(){curNarrowSpace=!curNarrowSpace;api('/api/obstacles?narrow='+curNarrowSpace);}" +
                // Stav obou joysticku (levy = posun X/Y, pravy = rotace Theta)
                "var joyMoveActive=false,joyRotActive=false,joyX=0,joyY=0,joyTh=0;" +
                "function sendCombinedJoy(){" +
                "if(!joyMoveActive && !joyRotActive){" +
                "document.getElementById('joyReadout').innerText='Zastavuji pojezd...';" +
                "fetch('/api/stop_move',{method:'POST'}).catch(()=>{});" +
                "return;" +
                "}" +
                "document.getElementById('joyReadout').innerText='Dopředu: '+(joyX*100).toFixed(0)+'% | Bokem: '+(joyY*100).toFixed(0)+'% | Otáčení: '+(joyTh*100).toFixed(0)+'%';" +
                "fetch('/api/joystick?x='+joyX.toFixed(2)+'&y='+joyY.toFixed(2)+'&theta='+joyTh.toFixed(2),{method:'POST'}).catch(()=>{});" +
                "}" +

                // Levy joystick: Ciste pohyb dopredu/dozadu (x) a do stran/ukroky (y)
                "setupJoystick('joyDrive','stickDrive',function(nx,ny){" +
                "joyMoveActive=true;joyX=ny;joyY=-nx;" +
                "sendCombinedJoy();" +
                "},function(){" +
                "joyMoveActive=false;joyX=0;joyY=0;" +
                "sendCombinedJoy();" +
                "});" +

                // Pravy joystick: Ciste otaceni na miste doleva/doprava (theta)
                "setupJoystick('joyStrafe','stickStrafe',function(nx,ny){" +
                "joyRotActive=true;joyTh=-nx;" +
                "sendCombinedJoy();" +
                "},function(){" +
                "joyRotActive=false;joyTh=0;" +
                "sendCombinedJoy();" +
                "});" +

                "function refreshStatus(){fetch('/api/status').then(r=>r.json()).then(d=>{" +
                "document.getElementById('stBadge').innerText='Stav: '+d.state+(d.robotFocus?' (QiSDK)':' (Bez Focusu)');" +
                "document.getElementById('flapWarn').style.display=d.chargingFlapOpen?'block':'none';" +
                "curFaceTrack=!!d.faceTracking;curNarrowSpace=!!d.narrowSpace;" +
                "var bf=document.getElementById('btnFaceTrack');" +
                "bf.innerText=curFaceTrack?'👀 Sledování obličejů: ZAP':'🔒 Hlava rovně (Focus VYP)';" +
                "bf.style.background=curFaceTrack?'#0284c7':'#d97706';" +
                "var bn=document.getElementById('btnNarrowSpace');" +
                "bn.innerText=curNarrowSpace?'🪑 Úzký prostor: ZAP (8 cm)':'🛡️ Odstup: BĚŽNÝ (35 cm)';" +
                "bn.style.background=curNarrowSpace?'#7c3aed':'#475569';" +
                "if(d.lastMessage)document.getElementById('lastMsg').innerText='Poslední odpověď: '+d.lastMessage;" +
                "}).catch(()=>{});}" +
                "setInterval(refreshStatus,2000);refreshStatus();" +
                "</script></body></html>";
    }
}
