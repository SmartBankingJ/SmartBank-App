param(
    [string]$JavaHome = $env:JAVA_HOME,
    [string]$JavaFxLib = $env:SMARTBANK_JAVAFX_LIB,
    [string]$GsonJar = $env:SMARTBANK_GSON_JAR
)

$ErrorActionPreference = 'Stop'
$frontendPath = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\..\SmartBank-frontend'))
if (-not (Test-Path -LiteralPath (Join-Path $frontendPath 'pom.xml'))) {
    throw 'Clonez SmartBank-frontend a cote de SmartBank-App.'
}
if ($JavaHome -and -not (Test-Path -LiteralPath (Join-Path $JavaHome 'bin\javac.exe'))) {
    if ($PSBoundParameters.ContainsKey('JavaHome')) { throw 'Le dossier -JavaHome doit contenir un JDK avec bin\javac.exe.' }
    $JavaHome = $null
}
if (-not $JavaHome) {
    $jdk = Get-ChildItem -LiteralPath (Join-Path $env:ProgramFiles 'Java') -Directory -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -like 'jdk*' -and (Test-Path -LiteralPath (Join-Path $_.FullName 'bin\javac.exe')) } |
        Sort-Object Name -Descending | Select-Object -First 1
    if ($jdk) { $JavaHome = $jdk.FullName }
}
if ($JavaHome) {
    $env:JAVA_HOME = $JavaHome
    $env:Path = (Join-Path $JavaHome 'bin') + ';' + $env:Path
}
$javaCommand = if ($JavaHome) { Join-Path $JavaHome 'bin\java.exe' } else { (Get-Command java -ErrorAction Stop).Source }
$javacCommand = if ($JavaHome) { Join-Path $JavaHome 'bin\javac.exe' } else { (Get-Command javac -ErrorAction Stop).Source }
if (-not $JavaFxLib) {
    $localRuntime = Join-Path $env:LOCALAPPDATA 'SmartBankingJ\tools\javafx-runtime'
    if (Test-Path -LiteralPath $localRuntime) { $JavaFxLib = $localRuntime }
}
if (-not $GsonJar) {
    foreach ($candidate in @(
        (Join-Path $env:LOCALAPPDATA 'SmartBankingJ\tools\auth-libs\gson-2.11.0.jar'),
        (Join-Path $env:USERPROFILE '.m2\repository\com\google\code\gson\gson\2.11.0\gson-2.11.0.jar')
    )) { if (Test-Path -LiteralPath $candidate) { $GsonJar = $candidate; break } }
}

Push-Location -LiteralPath $frontendPath
try {
    Write-Output "SmartBanking desktop : $frontendPath"
    if ($JavaFxLib) {
        if (-not $GsonJar) { throw 'Gson 2.11.0 est requis. Utilisez Maven ou renseignez -GsonJar.' }
        $GsonJar = (Resolve-Path -LiteralPath $GsonJar).Path
        # Optional offline launch using an existing JavaFX SDK/runtime.
        $JavaFxLib = (Resolve-Path -LiteralPath $JavaFxLib).Path
        New-Item -ItemType Directory -Force -Path 'target\classes' | Out-Null
        $sources = @(Get-ChildItem 'src\main\java' -Filter '*.java' -Recurse | ForEach-Object FullName)
        & $javacCommand --release 17 --module-path $JavaFxLib --add-modules javafx.controls,javafx.fxml -cp $GsonJar -encoding UTF-8 -d 'target\classes' @sources
        if ($LASTEXITCODE -ne 0) { throw 'Compilation JavaFX echouee.' }
        Copy-Item 'src\main\resources\*' 'target\classes' -Recurse -Force
        $desktopClasspath = 'target\classes;' + $GsonJar
        & $javaCommand --module-path $JavaFxLib --add-modules javafx.controls,javafx.fxml -cp $desktopClasspath ma.bankdirect.App
        if ($LASTEXITCODE -ne 0) { throw 'Demarrage JavaFX echoue.' }
    } else {
        $maven = Get-Command mvn.cmd -ErrorAction SilentlyContinue
        if (-not $maven) { throw 'Installez Maven, ou indiquez -JavaFxLib avec le dossier lib du SDK JavaFX.' }
        & $maven.Source javafx:run
        if ($LASTEXITCODE -ne 0) { throw 'Demarrage Maven echoue.' }
    }
} finally {
    Pop-Location
}
