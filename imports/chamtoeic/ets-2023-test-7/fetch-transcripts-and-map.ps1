$ErrorActionPreference = "Stop"

$dir = Split-Path -Parent $MyInvocation.MyCommand.Path
$slug = "ets-2023-test-7"
$questionsUrl = "https://chamtoeic.edu.vn/api/v1/question-bank-service/test-sets/accessible/$slug/questions?show_answers=true"
$rawPath = Join-Path $dir "questions-show-answers.json"
$completePath = Join-Path $dir "questions-complete-mapped.json"
$transcriptMapPath = Join-Path $dir "transcript-map.json"
$labels = @("A", "B", "C", "D", "E", "F", "G", "H")

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

function Convert-HtmlToText {
    param([AllowNull()][string] $Html)

    if ([string]::IsNullOrWhiteSpace($Html)) {
        return $null
    }

    $text = $Html
    $text = $text -replace "(?i)<\s*br\s*/?\s*>", "`n"
    $text = $text -replace "(?i)</\s*p\s*>", "`n"
    $text = $text -replace "(?is)<[^>]+>", ""
    $text = [System.Net.WebUtility]::HtmlDecode($text)
    $text = $text -replace "\u00a0", " "
    $text = $text -replace "[ \t]+\r?\n", "`n"
    $text = $text -replace "\r?\n\s*\r?\n+", "`n"
    return $text.Trim()
}

function Get-PartName {
    param([object] $Group)

    if ($Group.questionPart -eq "CUSTOM" -and $Group.customName) {
        return "CUSTOM:$($Group.customName)"
    }

    return $Group.questionPart
}

function New-MediaEntry {
    param([object] $File)

    return [ordered]@{
        id = $File.id
        type = $File.fileType
        displayOrder = $File.displayOrder
        url = $File.url
    }
}

$client = New-Object System.Net.WebClient
$client.Encoding = [System.Text.Encoding]::UTF8
$client.Headers.Add("Accept", "application/json")
$payload = $client.DownloadString($questionsUrl) | ConvertFrom-Json

$flatTranscriptMap = @()
$globalQuestionNumber = 1

foreach ($group in @($payload.data.questionGroups | Sort-Object questionGroupOrder)) {
    $part = Get-PartName -Group $group
    $mediaFiles = @($group.files | Sort-Object displayOrder | ForEach-Object { New-MediaEntry -File $_ })
    $audioUrls = @($mediaFiles | Where-Object { $_.type -eq "AUDIO" } | ForEach-Object { $_.url })
    $imageUrls = @($mediaFiles | Where-Object { $_.type -eq "IMAGE" } | ForEach-Object { $_.url })
    $audioTranscriptHtml = $group.audioTranscript
    $audioTranscriptText = Convert-HtmlToText -Html $audioTranscriptHtml
    $groupExplanationHtml = $group.explanation
    $groupExplanationText = Convert-HtmlToText -Html $groupExplanationHtml

    foreach ($question in @($group.questions | Sort-Object questionNumber)) {
        $answers = @($question.answers | Sort-Object answerOrder)
        $correctAnswer = @($answers | Where-Object { $_.isCorrect -eq $true } | Select-Object -First 1)
        $correctAnswerId = if ($correctAnswer.Count -gt 0) { $correctAnswer[0].id } else { $null }
        $correctOrder = if ($correctAnswer.Count -gt 0) { $correctAnswer[0].answerOrder } else { $null }
        $correctLabel = if ($null -ne $correctOrder -and $correctOrder -gt 0 -and $correctOrder -le $labels.Count) {
            $labels[$correctOrder - 1]
        } else {
            $null
        }

        $question | Add-Member -NotePropertyName correctAnswerId -NotePropertyValue $correctAnswerId -Force
        $question | Add-Member -NotePropertyName correctAnswerOrder -NotePropertyValue $correctOrder -Force
        $question | Add-Member -NotePropertyName correctAnswerLabel -NotePropertyValue $correctLabel -Force
        $question | Add-Member -NotePropertyName mediaFiles -NotePropertyValue $mediaFiles -Force
        $question | Add-Member -NotePropertyName audioUrls -NotePropertyValue $audioUrls -Force
        $question | Add-Member -NotePropertyName imageUrls -NotePropertyValue $imageUrls -Force
        $question | Add-Member -NotePropertyName audioTranscriptHtml -NotePropertyValue $audioTranscriptHtml -Force
        $question | Add-Member -NotePropertyName audioTranscriptText -NotePropertyValue $audioTranscriptText -Force
        $question | Add-Member -NotePropertyName groupExplanationHtml -NotePropertyValue $groupExplanationHtml -Force
        $question | Add-Member -NotePropertyName groupExplanationText -NotePropertyValue $groupExplanationText -Force

        $flatTranscriptMap += [ordered]@{
            globalQuestionNumber = $globalQuestionNumber
            part = $part
            questionGroupOrder = $group.questionGroupOrder
            questionGroupId = $group.id
            questionId = $question.id
            questionNumber = $question.questionNumber
            questionText = $question.questionText
            correctAnswerLabel = $correctLabel
            audioUrls = $audioUrls
            imageUrls = $imageUrls
            audioTranscriptHtml = $audioTranscriptHtml
            audioTranscriptText = $audioTranscriptText
            groupExplanationHtml = $groupExplanationHtml
            groupExplanationText = $groupExplanationText
        }

        $globalQuestionNumber++
    }
}

$summary = [ordered]@{
    source = $questionsUrl
    generatedAt = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ssZ")
    testSetId = $payload.data.testSetId
    testSetSlug = $payload.data.testSetSlug
    testName = $payload.data.testName
    counts = [ordered]@{
        groups = @($payload.data.questionGroups).Count
        questions = $flatTranscriptMap.Count
        groupsWithAudioTranscript = @($payload.data.questionGroups | Where-Object { $_.audioTranscript }).Count
        questionsWithAudioTranscript = @($flatTranscriptMap | Where-Object { $_.audioTranscriptText }).Count
        groupsWithExplanation = @($payload.data.questionGroups | Where-Object { $_.explanation }).Count
        questionsWithGroupExplanation = @($flatTranscriptMap | Where-Object { $_.groupExplanationText }).Count
    }
    questions = $flatTranscriptMap
}

ConvertTo-NiceJson -Value $payload | Set-Content -Path $rawPath -Encoding UTF8
ConvertTo-NiceJson -Value $payload | Set-Content -Path $completePath -Encoding UTF8
ConvertTo-NiceJson -Value $summary | Set-Content -Path $transcriptMapPath -Encoding UTF8

[pscustomobject]$summary.counts











