# ------------------------------------------------------------------
# CONFIGURATION
# ------------------------------------------------------------------

# VARIABLES
$Var1Name = "HADOOP_HOME"
$Var2Name = "SPARK_HOME"
$VarValue = "C:\spark-3.5.3-bin-hadoop3"

# PATH TO ADD
$PathToAppend = "C:\spark-3.5.3-bin-hadoop3\bin" 

# ------------------------------------------------------------------
# EXECUTION
# ------------------------------------------------------------------

Write-Host " Starting setting of System variables (Scope: Machine)..." -ForegroundColor Cyan
Write-Host "----------------------------------------------------------------"

# --- VARIABLES CREATION ---

# Variable 1
[System.Environment]::SetEnvironmentVariable($Var1Name, $VarValue, "Machine")
Write-Host "   Variable '$Var1Name' created successfully." -ForegroundColor Green

# Variable 2
[System.Environment]::SetEnvironmentVariable($Var2Name, $VarValue, "Machine")
Write-Host "   Variable '$Var2Name' created successfully." -ForegroundColor Green


# --- APPEND TO PATH VARIABLE (SAFE!) ---

Write-Host "`n Starting modify PATH..." -ForegroundColor Red

# 1. Retrieve current PATH
$CurrentPath = [System.Environment]::GetEnvironmentVariable("Path", "Machine")

# 2. Check if the new path is already present (to avoid duplicates)
if ($CurrentPath -split ';' -contains $PathToAppend) {
    Write-Host "   No worries: '$PathToAppend' is already in PATH." -ForegroundColor Yellow
} else {
    # 3. Append with ; and save
    $FinalPath = $CurrentPath + ";" + $PathToAppend
    [System.Environment]::SetEnvironmentVariable("Path", $FinalPath, "Machine")
    Write-Host "   Path '$PathToAppend' added to system PATH." -ForegroundColor Green
}

Write-Host "`n Operation completed. All changes are persistent." -ForegroundColor DarkGray
Write-Host "You have to RESTART the terminal (or the Windows session) to load them!" -ForegroundColor DarkGray