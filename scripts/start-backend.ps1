# 启动全部后端服务（JDK 21）。前置：docker compose up -d 已就绪，且已 mvn install。
# 用法：powershell -ExecutionPolicy Bypass -File scripts/start-backend.ps1
$ErrorActionPreference = 'Stop'
$env:JAVA_HOME = 'D:\0-kinn\9-software\0-Dev\0-Jdk\jdk21'
$root = Join-Path $PSScriptRoot '..\backend' | Resolve-Path
New-Item -ItemType Directory -Force "$root\logs" | Out-Null
$order = 'gateway', 'user', 'product', 'order', 'payment', 'logistics'
foreach ($s in $order) {
    $jar = "$root\shop-$s\target\shop-$s-1.0.0.jar"
    if (-not (Test-Path $jar)) { Write-Warning "缺少 $jar，请先在 backend 下执行 mvn -DskipTests clean install"; continue }
    Start-Process -FilePath "$env:JAVA_HOME\bin\java.exe" -ArgumentList "-jar", $jar `
        -RedirectStandardOutput "$root\logs\$s.log" -RedirectStandardError "$root\logs\$s.err.log" -WindowStyle Hidden
    Write-Host "started shop-$s"
}
Write-Host "`n全部服务已后台启动，日志在 backend\logs\；Knife4j 文档示例：http://localhost:9601/doc.html"
