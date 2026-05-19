$mavenVersion = "3.9.6"
$mavenDir = "$PSScriptRoot\.mvn\maven"
$mavenBin = "$mavenDir\apache-maven-$mavenVersion\bin\mvn.cmd"
$mavenZip = "$PSScriptRoot\.mvn\maven.zip"

if (-not (Test-Path $mavenBin)) {
    Write-Host "Downloading Maven $mavenVersion..." -ForegroundColor Cyan
    New-Item -ItemType Directory -Path "$PSScriptRoot\.mvn\maven" -Force | Out-Null
    $url = "https://archive.apache.org/dist/maven/maven-3/$mavenVersion/binaries/apache-maven-$mavenVersion-bin.zip"
    [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
    try {
        Invoke-WebRequest -Uri $url -OutFile $mavenZip -UseBasicParsing
    } catch {
        Write-Host "Primary URL failed, trying mirror..." -ForegroundColor Yellow
        $url = "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$mavenVersion/apache-maven-$mavenVersion-bin.zip"
        Invoke-WebRequest -Uri $url -OutFile $mavenZip -UseBasicParsing
    }
    Write-Host "Extracting Maven..." -ForegroundColor Cyan
    Expand-Archive -Path $mavenZip -DestinationPath $mavenDir -Force
    Remove-Item $mavenZip -Force
    Write-Host "Maven $mavenVersion installed!" -ForegroundColor Green
}

$env:JAVA_HOME = "C:\Program Files\Java\jdk-23"
Write-Host "Starting Spring Boot..." -ForegroundColor Cyan
& "$mavenBin" spring-boot:run
