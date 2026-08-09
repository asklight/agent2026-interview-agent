[CmdletBinding()]
param(
    [string]$Output = "D:\desk\tjupro\rag-toolkit\data\interview-question-cards.jsonl",
    [string]$Mysql = "D:\MySQL Server\bin\mysql.exe",
    [string]$Database = "agent2026_interview_agent",
    [string]$DbHost = "127.0.0.1",
    [int]$Port = 3306,
    [string]$User = "agent2026_user"
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path -LiteralPath $Mysql)) {
    throw "mysql client not found: $Mysql"
}
if (-not $env:MYSQL_PASSWORD) {
    throw "MYSQL_PASSWORD must be set in the environment"
}
$env:MYSQL_PWD = $env:MYSQL_PASSWORD

$query = @"
SELECT JSON_OBJECT(
  'document_id', CONCAT('question-card-', card_code),
  'content', CONCAT(
    'Question: ', main_question,
    CHAR(10), 'Key points: ', COALESCE(key_points, '[]'),
    CHAR(10), 'Common mistakes: ', COALESCE(common_mistakes, '[]'),
    CHAR(10), 'Follow-ups: ', COALESCE(followups, '[]'),
    CHAR(10), 'Scenario follow-ups: ', COALESCE(scenario_followups, '[]')
  ),
  'source', JSON_OBJECT(
    'title', CONCAT('面试题卡 · ', module, ' · ', card_code),
    'uri', CONCAT('question-card://', card_code)
  ),
  'metadata', JSON_OBJECT(
    'source_type', 'question_card',
    'module', module,
    'difficulty', difficulty,
    'tags', COALESCE(tags, '')
  )
)
FROM question_card
WHERE enabled = 1
ORDER BY module, difficulty, id;
"@
$query = ($query -replace "[\r\n]+", " ").Trim()

$arguments = @(
    "--host=$DbHost",
    "--port=$Port",
    "--user=$User",
    "--database=$Database",
    "--default-character-set=utf8mb4",
    "--batch",
    "--raw",
    "--skip-column-names",
    "--execute=$query"
)

$lines = & $Mysql @arguments
if ($LASTEXITCODE -ne 0) {
    throw "mysql query failed with exit code $LASTEXITCODE"
}

$records = @($lines | Where-Object { $_ -and $_.Trim() })
if ($records.Count -eq 0) {
    throw "no enabled question cards were exported"
}

$parent = Split-Path -Parent $Output
New-Item -ItemType Directory -Force -Path $parent | Out-Null
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllLines($Output, $records, $utf8NoBom)

Write-Output ("exported {0} question cards to {1}" -f $records.Count, $Output)
