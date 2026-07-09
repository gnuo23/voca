$ErrorActionPreference = "Stop"

$dir = Split-Path -Parent $MyInvocation.MyCommand.Path
$questionsPath = Join-Path $dir "questions.json"
$submitUrl = "https://chamtoeic.edu.vn/api/v1/test-evaluation-service/submit-test/accessible"
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

$questionsPayload = Get-Content $questionsPath -Raw | ConvertFrom-Json
$groups = @($questionsPayload.data.questionGroups)

$userAnswers = @()
$answered = $false
foreach ($group in $groups) {
    foreach ($question in @($group.questions)) {
        $sortedAnswers = @($question.answers | Sort-Object answerOrder)
        $answerId = $null
        if (-not $answered -and $sortedAnswers.Count -gt 0) {
            $answerId = $sortedAnswers[0].id
            $answered = $true
        }

        $userAnswers += [ordered]@{
            questionGroupId = $group.id
            questionId = $question.id
            answerId = $answerId
            content = $null
            questionPart = Get-PartName -Group $group
            questionNumber = $question.questionNumber
        }
    }
}

$partsTaken = @($groups | ForEach-Object { Get-PartName -Group $_ } | Select-Object -Unique)
$request = [ordered]@{
    collectionId = $questionsPayload.data.collectionId
    testsetId = $questionsPayload.data.testSetId
    testsetName = $questionsPayload.data.testName
    isFulltest = $false
    partsTaken = $partsTaken
    userAnswers = $userAnswers
    completionTime = 1
}

ConvertTo-NiceJson -Value $request | Set-Content -Path (Join-Path $dir "submit-one-answer-request.json") -Encoding UTF8

$client = New-Object System.Net.WebClient
$client.Encoding = [System.Text.Encoding]::UTF8
$client.Headers.Add("Accept", "application/json")
$client.Headers.Add("Content-Type", "application/json")
$response = $client.UploadString($submitUrl, "POST", ($request | ConvertTo-Json -Depth 100)) | ConvertFrom-Json

ConvertTo-NiceJson -Value $response | Set-Content -Path (Join-Path $dir "submit-one-answer-response.json") -Encoding UTF8

$detailByQuestionId = @{}
$response.data.answerDetails.PSObject.Properties | ForEach-Object {
    $detailByQuestionId[$_.Value.questionId] = $_.Value
}

$answerKey = @()
$globalNumber = 1
foreach ($group in $groups) {
    foreach ($question in @($group.questions)) {
        $detail = $detailByQuestionId[$question.id]
        $correctAnswerId = if ($detail) { $detail.correctAnswerId } else { $null }
        $correctAnswer = @($question.answers | Where-Object { $_.id -eq $correctAnswerId } | Select-Object -First 1)
        $correctOrder = if ($correctAnswer.Count -gt 0) { $correctAnswer[0].answerOrder } else { $null }
        $correctLabel = if ($null -ne $correctOrder -and $correctOrder -gt 0 -and $correctOrder -le $labels.Count) {
            $labels[$correctOrder - 1]
        } else {
            $null
        }
        $correctContent = if ($correctAnswer.Count -gt 0) { $correctAnswer[0].content } elseif ($detail) { $detail.correctContent } else { $null }
        $tags = [System.Collections.ArrayList]::new()
        if ($detail -and $detail.tags) {
            @($detail.tags) | ForEach-Object { [void] $tags.Add($_) }
        }

        foreach ($answer in @($question.answers)) {
            $answer.isCorrect = ($answer.id -eq $correctAnswerId)
        }

        $question | Add-Member -NotePropertyName correctAnswerId -NotePropertyValue $correctAnswerId -Force
        $question | Add-Member -NotePropertyName correctAnswerOrder -NotePropertyValue $correctOrder -Force
        $question | Add-Member -NotePropertyName correctAnswerLabel -NotePropertyValue $correctLabel -Force
        $question | Add-Member -NotePropertyName tags -NotePropertyValue $tags -Force

        $answerKey += [ordered]@{
            globalQuestionNumber = $globalNumber
            questionPart = Get-PartName -Group $group
            questionGroupOrder = $group.questionGroupOrder
            questionNumber = $question.questionNumber
            questionGroupId = $group.id
            questionId = $question.id
            correctAnswerId = $correctAnswerId
            correctAnswerOrder = $correctOrder
            correctAnswerLabel = $correctLabel
            correctAnswerContent = $correctContent
            explanation = $question.explanation
            tags = $tags
        }

        $globalNumber++
    }
}

$answerKeyPayload = [ordered]@{
    source = [ordered]@{
        questions = "questions.json"
        submitResponse = "submit-one-answer-response.json"
        generatedAt = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ssZ")
        note = "Public anonymous submit response includes correct answers but no explanations."
    }
    testSetId = $questionsPayload.data.testSetId
    testSetSlug = $questionsPayload.data.testSetSlug
    testName = $questionsPayload.data.testName
    answers = $answerKey
}

ConvertTo-NiceJson -Value $answerKeyPayload | Set-Content -Path (Join-Path $dir "answer-key.json") -Encoding UTF8
ConvertTo-NiceJson -Value $questionsPayload | Set-Content -Path (Join-Path $dir "questions-with-answer-key.json") -Encoding UTF8

$explanationCount = @($answerKey | Where-Object { $null -ne $_.explanation -and $_.explanation -ne "" }).Count
[pscustomobject]@{
    answers = $answerKey.Count
    explanations = $explanationCount
    output = $dir
}







