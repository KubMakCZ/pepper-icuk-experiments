# =====================================================
#  deploy.ps1 - Nasazeni Rubby APK na zarizeni pres ADB
#  Pouziti: .\deploy.ps1
#           .\deploy.ps1 -Device 192.168.0.36:5555
#           .\deploy.ps1 -Build      (zkompiluje a nasadi)
#           .\deploy.ps1 -Build -Launch (zkompiluje, nasadi, spusti)
# =====================================================

param(
    [string]$Device = "",          # IP:port nebo prazdne = prvni nalezene
    [switch]$Build = $false,       # Zkompilovat pred nasazenim
    [switch]$Launch = $false,      # Spustit app po instalaci
    [switch]$Logcat = $false       # Zobrazit logy po spusteni
)

$APK_PATH = "app\build\outputs\apk\debug\app-debug.apk"
$PACKAGE   = "cz.kubmak.rubby_llm_exp"
$ACTIVITY  = "$PACKAGE/.MainActivity"
$JAVA_HOME_AS = "C:\Program Files\Android\Android Studio\jre"

# Barvy pro výstup
function Write-OK   { param($msg) Write-Host "[OK] $msg" -ForegroundColor Green }
function Write-INFO { param($msg) Write-Host "[..] $msg" -ForegroundColor Cyan }
function Write-WARN { param($msg) Write-Host "[!!] $msg" -ForegroundColor Yellow }
function Write-FAIL { param($msg) Write-Host "[XX] $msg" -ForegroundColor Red }

Write-Host ""
Write-Host "========================================" -ForegroundColor Magenta
Write-Host "   Rubby Deploy Script" -ForegroundColor Magenta
Write-Host "========================================" -ForegroundColor Magenta
Write-Host ""

# --- KROK 1: Kompilace (volitelna) ---
if ($Build) {
    Write-INFO "Kompilace projektu (gradlew assembleDebug)..."
    $env:JAVA_HOME = $JAVA_HOME_AS
    $result = & ".\gradlew.bat" assembleDebug 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-FAIL "Kompilace SELHALA!"
        Write-Host ($result | Select-String "error:" | Out-String) -ForegroundColor Red
        exit 1
    }
    Write-OK "Kompilace uspesna."
    Write-Host ""
}

# --- KROK 2: Kontrola APK ---
if (-not (Test-Path $APK_PATH)) {
    Write-FAIL "APK nenalezeno: $APK_PATH"
    Write-WARN "Spust skript s parametrem -Build nebo nejdriv zkompiluj v Android Studiu."
    exit 1
}
$apkInfo = Get-Item $APK_PATH
Write-OK "APK nalezeno: $APK_PATH ($([math]::Round($apkInfo.Length / 1MB, 1)) MB)"

# --- KROK 3: Hledani zarizeni ---
Write-INFO "Hledam zarizeni..."
$adbDevicesRaw = adb devices 2>&1 | Select-Object -Skip 1  # preskoc prvni radek "List of devices attached"
$devices = @()
foreach ($line in $adbDevicesRaw) {
    $trimmed = $line.ToString().Trim()
    if ($trimmed -match "^(.+)\s+device$") {
        $devices += $Matches[1].Trim()
    }
}

if ($devices.Count -eq 0) {
    Write-FAIL "Zadne zarizeni nenalezeno!"
    Write-WARN "Zkontroluj:"
    Write-Host "  1. Wi-Fi ADB: adb connect 192.168.0.36:5555" -ForegroundColor Yellow
    Write-Host "  2. USB kabel: zkontroluj USB Debugging v nastaveni" -ForegroundColor Yellow
    exit 1
}

# Vyber zarizeni
if ($Device -ne "") {
    $targetDevice = $Device
    Write-INFO "Pouzivam zadane zarizeni: $targetDevice"
} elseif ($devices.Count -eq 1) {
    $targetDevice = $devices[0]
    Write-OK "Nalezeno zarizeni: $targetDevice"
} else {
    Write-WARN "Nalezeno vice zarizeni - vyber:"
    for ($i = 0; $i -lt $devices.Count; $i++) {
        Write-Host "  [$i] $($devices[$i])" -ForegroundColor Cyan
    }
    $idx = Read-Host "Cislo zarizeni"
    $targetDevice = $devices[[int]$idx]
}

Write-Host ""

# --- KROK 4: Přenos APK (adb push - ukazuje % průběhu) ---
$remoteApkPath = "/data/local/tmp/rubby_deploy.apk"
Write-INFO "Prenos APK na zarizeni (uvidis % prubehu)..."
Write-Host ""
adb -s $targetDevice push $APK_PATH $remoteApkPath
if ($LASTEXITCODE -ne 0) {
    Write-FAIL "Prenos APK selhal! Zkontroluj WiFi spojeni."
    exit 1
}
Write-Host ""
Write-OK "Prenos dokoncen."

# --- KROK 5: Instalace (rychle, jen secunada) ---
Write-INFO "Instaluji APK na zarizeni..."
$installResult = adb -s $targetDevice shell "pm install -r -t $remoteApkPath" 2>&1 | Out-String
if ($installResult -notmatch "Success") {
    Write-WARN "Znovu navazuji ADB spojeni po dlouhem prenosu a opakuji instalaci..."
    adb disconnect | Out-Null
    Start-Sleep -Seconds 1
    adb connect $targetDevice | Out-Null
    Start-Sleep -Seconds 1
    $installResult = adb -s $targetDevice shell "pm install -r -t $remoteApkPath" 2>&1 | Out-String
}
# Uklidime docasny soubor
adb -s $targetDevice shell "rm -f $remoteApkPath" | Out-Null

if ($installResult -notmatch "Success") {
    Write-FAIL "Instalace SELHALA!"
    Write-Host $installResult -ForegroundColor Red
    exit 1
}
Write-OK "Instalace uspesna!"

# --- KROK 5: Spusteni (volitelne) ---
if ($Launch) {
    Write-Host ""
    Write-INFO "Spoustim aplikaci..."
    adb -s $targetDevice shell "am force-stop $PACKAGE" | Out-Null
    adb -s $targetDevice shell "am start -n $ACTIVITY" | Out-Null
    Start-Sleep -Seconds 2
    
    $psLine = adb -s $targetDevice shell "ps | grep $PACKAGE" 2>$null | Out-String
    if ($psLine -and $psLine.Trim() -ne "") {
        $tokens = $psLine.Trim() -split "\s+"
        $appPid = if ($tokens.Count -ge 2) { $tokens[1] } else { "aktivni" }
        Write-OK "Aplikace bezi (PID: $appPid)"
    } else {
        Write-WARN "Aplikace se nespustila automaticky - spust ji rucne na zarizeni."
    }
}

# --- KROK 6: Logcat (volitelny) ---
if ($Logcat) {
    Write-Host ""
    Write-OK "Spoustim Logcat (filtr: RubbyLLM, OpenRouterService, GeminiService)"
    Write-WARN "Stiskni Ctrl+C pro ukonceni."
    Write-Host ""
    adb -s $targetDevice logcat -c  # Vymaze stare logy
    adb -s $targetDevice logcat RubbyLLM:D OpenRouterService:D GeminiService:D FileLogger:D *:S
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Magenta
Write-OK "Hotovo! Aplikaci spust rucne na zarizeni nebo pouzij -Launch."
Write-Host "========================================" -ForegroundColor Magenta
Write-Host ""
Write-Host "Tipy:" -ForegroundColor Gray
Write-Host "  .\deploy.ps1 -Build -Launch -Logcat   # vse najednou" -ForegroundColor Gray
Write-Host "  .\deploy.ps1 -Device 192.168.0.36:5555 # konkretni zarizeni" -ForegroundColor Gray
Write-Host "  adb connect 192.168.0.36:5555          # pripojit pres WiFi" -ForegroundColor Gray
