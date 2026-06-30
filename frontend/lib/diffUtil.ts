/**
 * Character-level diff utility using Longest Common Subsequence (LCS).
 * Used to highlight differences between user's answer and the correct answer
 * in the Learn mode's "almost correct" / CLOSE feedback.
 */

export type DiffType = "match" | "wrong" | "missing";

export type DiffSegment = {
  text: string;
  type: DiffType;
};

/**
 * Compute the LCS table for two strings.
 * Returns a 2D array where lcs[i][j] = length of LCS of a[0..i-1] and b[0..j-1].
 */
function buildLcsTable(a: string, b: string): number[][] {
  const m = a.length;
  const n = b.length;
  const table: number[][] = Array.from({ length: m + 1 }, () =>
    new Array<number>(n + 1).fill(0)
  );

  for (let i = 1; i <= m; i++) {
    for (let j = 1; j <= n; j++) {
      if (a[i - 1] === b[j - 1]) {
        table[i][j] = table[i - 1][j - 1] + 1;
      } else {
        table[i][j] = Math.max(table[i - 1][j], table[i][j - 1]);
      }
    }
  }

  return table;
}

/**
 * Backtrack through the LCS table to produce diff segments for both strings.
 *
 * For userAnswer:
 *   - "match" = character present in LCS (correct)
 *   - "wrong" = character NOT in LCS (user typed something extra/wrong)
 *
 * For correctAnswer:
 *   - "match" = character present in LCS
 *   - "missing" = character NOT in LCS (user missed this character)
 */
function backtrack(
  table: number[][],
  a: string,
  b: string
): { aDiff: DiffSegment[]; bDiff: DiffSegment[] } {
  const aDiff: DiffSegment[] = [];
  const bDiff: DiffSegment[] = [];

  let i = a.length;
  let j = b.length;

  // Collect segments in reverse, then reverse at the end
  const aRaw: Array<{ char: string; type: DiffType }> = [];
  const bRaw: Array<{ char: string; type: DiffType }> = [];

  while (i > 0 || j > 0) {
    if (i > 0 && j > 0 && a[i - 1] === b[j - 1]) {
      aRaw.push({ char: a[i - 1], type: "match" });
      bRaw.push({ char: b[j - 1], type: "match" });
      i--;
      j--;
    } else if (j > 0 && (i === 0 || table[i][j - 1] >= table[i - 1][j])) {
      bRaw.push({ char: b[j - 1], type: "missing" });
      j--;
    } else if (i > 0) {
      aRaw.push({ char: a[i - 1], type: "wrong" });
      i--;
    }
  }

  aRaw.reverse();
  bRaw.reverse();

  // Merge consecutive chars of the same type into segments
  for (const { char, type } of aRaw) {
    const last = aDiff[aDiff.length - 1];
    if (last && last.type === type) {
      last.text += char;
    } else {
      aDiff.push({ text: char, type });
    }
  }

  for (const { char, type } of bRaw) {
    const last = bDiff[bDiff.length - 1];
    if (last && last.type === type) {
      last.text += char;
    } else {
      bDiff.push({ text: char, type });
    }
  }

  return { aDiff, bDiff };
}

/**
 * Compute character-level diff between userAnswer and correctAnswer.
 *
 * @returns userDiff — segments for the user's answer (match + wrong)
 * @returns correctDiff — segments for the correct answer (match + missing)
 *
 * Example:
 *   computeDiff("accomodate", "accommodate")
 *   userDiff:    [{ text: "accom", type: "match" }, { text: "o", type: "wrong" }, { text: "date", type: "match" }]
 *   correctDiff: [{ text: "accomm", type: "match" }, { text: "o", type: "missing" }, { text: "date", type: "match" }]
 */
export function computeDiff(
  userAnswer: string,
  correctAnswer: string
): { userDiff: DiffSegment[]; correctDiff: DiffSegment[] } {
  if (!userAnswer && !correctAnswer) {
    return { userDiff: [], correctDiff: [] };
  }

  if (!userAnswer) {
    return {
      userDiff: [],
      correctDiff: [{ text: correctAnswer, type: "missing" }],
    };
  }

  if (!correctAnswer) {
    return {
      userDiff: [{ text: userAnswer, type: "wrong" }],
      correctDiff: [],
    };
  }

  // Case-insensitive comparison for diff, but preserve original case in output
  const lowerUser = userAnswer.toLowerCase();
  const lowerCorrect = correctAnswer.toLowerCase();
  const table = buildLcsTable(lowerUser, lowerCorrect);
  const { aDiff: lowerUserDiff, bDiff: lowerCorrectDiff } = backtrack(
    table,
    lowerUser,
    lowerCorrect
  );

  // Map back to original case
  let userIdx = 0;
  const userDiff: DiffSegment[] = lowerUserDiff.map((seg) => {
    const text = userAnswer.slice(userIdx, userIdx + seg.text.length);
    userIdx += seg.text.length;
    return { text, type: seg.type };
  });

  let correctIdx = 0;
  const correctDiff: DiffSegment[] = lowerCorrectDiff.map((seg) => {
    const text = correctAnswer.slice(correctIdx, correctIdx + seg.text.length);
    correctIdx += seg.text.length;
    return { text, type: seg.type };
  });

  return { userDiff, correctDiff };
}
