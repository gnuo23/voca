$ErrorActionPreference = "Stop"

$dir = Split-Path -Parent $MyInvocation.MyCommand.Path
$overviewUrl = "https://chamtoeic.edu.vn/api/v1/question-bank-service/test-sets/accessible/ets-2024-test-9/overview"
$questionsUrl = "https://chamtoeic.edu.vn/api/v1/question-bank-service/test-sets/accessible/ets-2024-test-9/questions"

$headers = @{ Accept = "application/json" }

function ConvertTo-NiceJson {
    param(
        [AllowNull()]
        [object] $Value,

        [int] $Indent = 0
    )

    $currentIndent = "  " * $Indent
    $nextIndent = "  " * ($Indent + 1)

    if ($null -eq $Value) {
        return "null"
    }

    if ($Value -is [System.Array] -or $Value -is [System.Collections.IList]) {
        $items = @($Value)
        if ($items.Count -eq 0) {
            return "[]"
        }

        $lines = @("[")
        for ($i = 0; $i -lt $items.Count; $i++) {
            $suffix = if ($i -lt $items.Count - 1) { "," } else { "" }
            $lines += "$nextIndent$(ConvertTo-NiceJson -Value $items[$i] -Indent ($Indent + 1))$suffix"
        }
        $lines += "$currentIndent]"
        return ($lines -join "`n")
    }

    if ($Value -is [System.Collections.IDictionary]) {
        $properties = @($Value.Keys | ForEach-Object {
            [pscustomobject]@{ Name = $_; Value = $Value[$_] }
        })
    } elseif ($Value -is [pscustomobject]) {
        $properties = @($Value.PSObject.Properties | Where-Object {
            $_.MemberType -eq "NoteProperty"
        })
        if ($properties.Count -eq 0) {
            return ($Value | ConvertTo-Json -Compress)
        }
    } else {
        return ($Value | ConvertTo-Json -Compress)
    }

    if ($properties.Count -eq 0) {
        return "{}"
    }

    $lines = @("{")
    for ($i = 0; $i -lt $properties.Count; $i++) {
        $property = $properties[$i]
        $name = $property.Name | ConvertTo-Json -Compress
        $propertyValue = ConvertTo-NiceJson -Value $property.Value -Indent ($Indent + 1)
        $suffix = if ($i -lt $properties.Count - 1) { "," } else { "" }
        $lines += "$nextIndent${name}: $propertyValue$suffix"
    }
    $lines += "$currentIndent}"
    return ($lines -join "`n")
}

function Save-PrettyJson {
    param(
        [Parameter(Mandatory = $true)]
        [string] $Uri,

        [Parameter(Mandatory = $true)]
        [string] $Path
    )

    $client = New-Object System.Net.WebClient
    $client.Encoding = [System.Text.Encoding]::UTF8
    $client.Headers.Add("Accept", "application/json")
    $payload = $client.DownloadString($Uri) | ConvertFrom-Json
    ConvertTo-NiceJson -Value $payload | Set-Content -Path $Path -Encoding UTF8
    return $payload
}

$overview = Save-PrettyJson -Uri $overviewUrl -Path (Join-Path $dir "overview.json")
$questionsPayload = Save-PrettyJson -Uri $questionsUrl -Path (Join-Path $dir "questions.json")

$groups = @($questionsPayload.data.questionGroups)
$questions = @($groups | ForEach-Object { $_.questions })
$answers = @($questions | ForEach-Object { $_.answers })
$files = @($groups | ForEach-Object { $_.files })

$parts = @()
$groups |
    Group-Object questionPart |
    Sort-Object Name |
    ForEach-Object {
        $partQuestions = @($_.Group | ForEach-Object { $_.questions })
        $parts += [ordered]@{
            part = $_.Name
            groups = $_.Count
            questions = $partQuestions.Count
        }
    }

$summary = [ordered]@{
    source = [ordered]@{
        page = "https://chamtoeic.edu.vn/tests/ets-2024-test-9"
        overviewApi = $overviewUrl
        questionsApi = $questionsUrl
        fetchedAt = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ssZ")
    }
    test = $overview.data
    counts = [ordered]@{
        questionGroups = $groups.Count
        questions = $questions.Count
        answers = $answers.Count
        files = $files.Count
        correctFlagsNonNull = @($answers | Where-Object { $null -ne $_.isCorrect }).Count
        explanationsNonNull = @($questions | Where-Object { $null -ne $_.explanation -and $_.explanation -ne "" }).Count
    }
    parts = $parts
}

ConvertTo-NiceJson -Value $summary | Set-Content -Path (Join-Path $dir "summary.json") -Encoding UTF8

Get-ChildItem $dir -File | Select-Object Name, Length











