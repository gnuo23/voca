$ErrorActionPreference = "Stop"

$dir = Split-Path -Parent $MyInvocation.MyCommand.Path
$sourcePath = Join-Path $dir "questions-with-answer-key.json"
$mappedPath = Join-Path $dir "questions-with-answer-key-and-media.json"
$flatMapPath = Join-Path $dir "question-media-map.json"
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

$payload = Get-Content $sourcePath -Raw -Encoding UTF8 | ConvertFrom-Json
$flatMap = @()
$globalQuestionNumber = 1

foreach ($group in @($payload.data.questionGroups | Sort-Object questionGroupOrder)) {
    $mediaFiles = @($group.files | Sort-Object displayOrder | ForEach-Object { New-MediaEntry -File $_ })
    $audioUrls = @($mediaFiles | Where-Object { $_.type -eq "AUDIO" } | ForEach-Object { $_.url })
    $imageUrls = @($mediaFiles | Where-Object { $_.type -eq "IMAGE" } | ForEach-Object { $_.url })

    foreach ($question in @($group.questions | Sort-Object questionNumber)) {
        $question | Add-Member -NotePropertyName mediaFiles -NotePropertyValue $mediaFiles -Force
        $question | Add-Member -NotePropertyName audioUrls -NotePropertyValue $audioUrls -Force
        $question | Add-Member -NotePropertyName imageUrls -NotePropertyValue $imageUrls -Force

        $answers = @($question.answers | Sort-Object answerOrder)
        $correctAnswer = @($answers | Where-Object { $_.isCorrect -eq $true } | Select-Object -First 1)
        $correctOrder = if ($correctAnswer.Count -gt 0) { $correctAnswer[0].answerOrder } else { $question.correctAnswerOrder }
        $correctLabel = if ($question.correctAnswerLabel) {
            $question.correctAnswerLabel
        } elseif ($null -ne $correctOrder -and $correctOrder -gt 0 -and $correctOrder -le $labels.Count) {
            $labels[$correctOrder - 1]
        } else {
            $null
        }

        $flatMap += [ordered]@{
            globalQuestionNumber = $globalQuestionNumber
            part = Get-PartName -Group $group
            questionGroupOrder = $group.questionGroupOrder
            questionGroupId = $group.id
            questionId = $question.id
            questionNumber = $question.questionNumber
            questionText = $question.questionText
            answerCount = $answers.Count
            correctAnswerLabel = $correctLabel
            correctAnswerOrder = $correctOrder
            mediaFiles = $mediaFiles
            audioUrls = $audioUrls
            imageUrls = $imageUrls
        }

        $globalQuestionNumber++
    }
}

$summary = [ordered]@{
    source = "questions-with-answer-key.json"
    generatedAt = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ssZ")
    testSetId = $payload.data.testSetId
    testSetSlug = $payload.data.testSetSlug
    testName = $payload.data.testName
    counts = [ordered]@{
        questions = $flatMap.Count
        questionsWithMedia = @($flatMap | Where-Object { @($_.mediaFiles).Count -gt 0 }).Count
        questionsWithAudio = @($flatMap | Where-Object { @($_.audioUrls).Count -gt 0 }).Count
        questionsWithImage = @($flatMap | Where-Object { @($_.imageUrls).Count -gt 0 }).Count
        uniqueMedia = @($flatMap | ForEach-Object { $_.mediaFiles } | ForEach-Object { $_["url"] } | Select-Object -Unique).Count
    }
    questions = $flatMap
}

ConvertTo-NiceJson -Value $payload | Set-Content -Path $mappedPath -Encoding UTF8
ConvertTo-NiceJson -Value $summary | Set-Content -Path $flatMapPath -Encoding UTF8

[pscustomobject]@{
    questions = $summary.counts.questions
    questionsWithMedia = $summary.counts.questionsWithMedia
    questionsWithAudio = $summary.counts.questionsWithAudio
    questionsWithImage = $summary.counts.questionsWithImage
    uniqueMedia = $summary.counts.uniqueMedia
}










