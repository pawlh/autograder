package edu.byu.cs.autograder.git;

/**
 * Represents several configurable values about the
 * commit verification system.
 *
 * @param requiredCommits The number of significant commits that must exist
 * @param requiredDaysWithCommits The number of unique days that on which any commit is made
 * @param commitVerificationPenaltyPct The penalty percentage which should be deducted when conditions are not met
 * @param minimumChangedLinesPerCommit The number of line changes (insertions + deletions) that qualify a commit as "significant"
 * @param forgivenessMinutesHead The number of minutes in the future we will tolerate local clock non-synchronization.
 *                               <br>
 *                               Note that we can't reliably apply this same adjustment to the tail end of the
 *                               valid window since that could end up double-counting the final submission
 *                               from the previous phase. This is less of an issue, however, because
 *                               the tail timestamp comes from a commit which is generated by the same
 *                               local clock that would be generating the subsequent commit timestamps.
 */
public record CommitVerificationConfig(
        // Variable values
        int requiredCommits,
        int requiredDaysWithCommits,
        int minimumChangedLinesPerCommit,

        // Generally constant values
        int commitVerificationPenaltyPct,
        int forgivenessMinutesHead
) { }
