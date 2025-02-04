package edu.byu.cs.autograder.git.CommitValidation;

import edu.byu.cs.autograder.GradingContext;
import edu.byu.cs.autograder.GradingException;
import edu.byu.cs.autograder.score.LateDayCalculator;
import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.util.PhaseUtils;

import java.util.Collection;

public class DefaultGitVerificationStrategy implements CommitVerificationStrategy {
    private GradingContext gradingContext;
    private Result warnings;
    private Result errors;
    private Collection<String> expandExcludeSet;
    private final LateDayCalculator lateDayCalculator;

    public DefaultGitVerificationStrategy(LateDayCalculator lateDayCalculator) {
        this.lateDayCalculator = lateDayCalculator;
    }

    @Override
    public void evaluate(CommitVerificationContext commitContext, GradingContext gradingContext) throws GradingException, DataAccessException {
        this.gradingContext = gradingContext;
        var requiredCommits = commitContext.config().requiredCommits();
        var requiredDaysWithCommits = commitContext.config().requiredDaysWithCommits();

        var daysWithCommits = commitContext.daysWithCommits();
        var numCommits = commitContext.numCommits();
        var significantCommits = commitContext.significantCommits();
        var commitsByDay = commitContext.commitsByDay();

        var daysSubmittedEarly = lateDayCalculator.calculateEarlyDays(gradingContext.phase(), gradingContext.netId());
        var insufficientDaysWithCommits = daysWithCommits + daysSubmittedEarly < requiredDaysWithCommits;

        CV[] assertedConditions = {
                new CV(
                        numCommits < requiredCommits,
                        String.format("Not enough commits to pass off (%d/%d).", numCommits, requiredCommits)),
                new CV(
                        numCommits >= requiredCommits && significantCommits < requiredCommits,
                        String.format("Have some commits, but some of them are too insignificant for credit (%d/%d).", significantCommits, requiredCommits)),
                new CV(
                        insufficientDaysWithCommits,
                        String.format("Did not commit on enough days to pass off (%d/%d).", daysWithCommits, requiredDaysWithCommits)),
                new CV(
                        commitsByDay.commitsInFuture(),
                        "Suspicious commit history. Some commits are authored after the hand in date. Is your clock set incorrectly?"),
                new CV(
                        commitsByDay.commitsBackdated(),
                        "Suspicious commit history. Some commits have been backdated."),
        };
        CV[] warningConditions = {
                new CV(
                        !insufficientDaysWithCommits && daysWithCommits < requiredDaysWithCommits && daysSubmittedEarly > 0,
                        String.format("Committed %d of %d required days, but early completion made up the difference.", daysWithCommits, requiredDaysWithCommits)),
                new CV(
                        !commitsByDay.commitsInOrder(),
                        "Congratulations! You have changed the order of some of your commits. You won a medal for manipulating your git history in advanced ways🏅"),
                new CV(
                        commitsByDay.commitTimestampsDuplicated(),
                        commitsByDay.getErroringCommitsSet("commitTimestampsDuplicatedSubsequentOnly"),
                        "Mistaken history manipulation. Multiple commits have the exact same timestamp. Likely, commits were pushed and amended and merged together."),
                new CV(
                        commitsByDay.missingTailHash(),
                        "Missing tail hash. The previous submission commit could not be found in the repository."),
                new CV(
                        commitsByDay.commitsInPast() && !commitsByDay.missingTailHash(),
                        "Some commits excluded. Commits authored before the previous phase submission were not counted."),
        };

        // Preserve the first set of messages (preserve the original warnings about amending commits)
        if (warnings == null) warnings = Result.evaluateConditions(warningConditions, this::warningMessageTerminator);
        if (errors == null)   errors = Result.evaluateConditions(assertedConditions, this::errorMessageTerminator);

        // Rerun the analysis only if we detected amended commits
        this.expandExcludeSet = commitsByDay.getErroringCommitsSet("commitTimestampsDuplicatedSubsequentOnly");
    }

    @Override
    public Collection<String> extendExcludeSet() {
        return expandExcludeSet;
    }

    @Override
    public Result getWarnings() {
        return warnings;
    }

    @Override
    public Result getErrors() {
        return errors;
    }


    void warningMessageTerminator(Collection<String> warningMessages) {
        warningMessages.add("Grading will continue on this submission despite detecting Git warnings. "
                + "We recommend asking a TA to understand why these warnings appeared.");
    }

    void errorMessageTerminator(Collection<String> errorMessages) {
        if (!PhaseUtils.requiresTAPassoffForCommits(gradingContext.phase())) return;

        int commitVerificationPenaltyPct = gradingContext.verificationConfig().commitVerificationPenaltyPct();
        errorMessages.add("Since you did not meet the prerequisites for commit frequency, "
                + "you will need to talk to a TA to receive a score. ");
        errorMessages.add(String.format("It may come with a %d%% penalty.", commitVerificationPenaltyPct));
    }
}
