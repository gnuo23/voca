$ErrorActionPreference = "Stop"

$dir = Split-Path -Parent $MyInvocation.MyCommand.Path
$overviewPath = Join-Path $dir "overview.json"
$completePath = Join-Path $dir "questions-complete-mapped.json"
$mediaLinksPath = Join-Path $dir "media-links.json"
$outputPath = Join-Path $dir "ets-2023-test-10-full.json"

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

$overview = Get-Content $overviewPath -Raw -Encoding UTF8 | ConvertFrom-Json
$complete = Get-Content $completePath -Raw -Encoding UTF8 | ConvertFrom-Json
$mediaLinks = Get-Content $mediaLinksPath -Raw -Encoding UTF8 | ConvertFrom-Json

$groups = @($complete.data.questionGroups)
$questions = @($groups | ForEach-Object { $_.questions })
$answers = @($questions | ForEach-Object { $_.answers })

$payload = [ordered]@{
    schemaVersion = 1
    source = [ordered]@{
        page = "https://chamtoeic.edu.vn/tests/ets-2023-test-10"
        overviewApi = "https://chamtoeic.edu.vn/api/v1/question-bank-service/test-sets/accessible/ets-2023-test-10/overview"
        questionsApi = "https://chamtoeic.edu.vn/api/v1/question-bank-service/test-sets/accessible/ets-2023-test-10/questions"
        questionsWithAnswersApi = "https://chamtoeic.edu.vn/api/v1/question-bank-service/test-sets/accessible/ets-2023-test-10/questions?show_answers=true"
        generatedAt = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ssZ")
    }
    test = $overview.data
    counts = [ordered]@{
        questionGroups = $groups.Count
        questions = $questions.Count
        answers = $answers.Count
        correctAnswers = @($answers | Where-Object { $_.isCorrect -eq $true }).Count
        mediaFiles = $mediaLinks.counts.total
        audioFiles = $mediaLinks.counts.audio
        imageFiles = $mediaLinks.counts.image
        questionsWithMedia = @($questions | Where-Object { @($_.mediaFiles).Count -gt 0 }).Count
        questionsWithAudio = @($questions | Where-Object { @($_.audioUrls).Count -gt 0 }).Count
        questionsWithImage = @($questions | Where-Object { @($_.imageUrls).Count -gt 0 }).Count
        groupsWithAudioTranscript = @($groups | Where-Object { $_.audioTranscript }).Count
        questionsWithAudioTranscript = @($questions | Where-Object { $_.audioTranscriptText }).Count
        groupsWithExplanation = @($groups | Where-Object { $_.explanation }).Count
        questionsWithGroupExplanation = @($questions | Where-Object { $_.groupExplanationText }).Count
    }
    questionGroups = $groups
    mediaFiles = $mediaLinks.files
}

ConvertTo-NiceJson -Value $payload | Set-Content -Path $outputPath -Encoding UTF8

[pscustomobject]$payload.counts











