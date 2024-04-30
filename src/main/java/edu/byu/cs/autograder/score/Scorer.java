package edu.byu.cs.autograder.score;

import edu.byu.cs.autograder.GradingContext;
import edu.byu.cs.autograder.GradingException;
import edu.byu.cs.canvas.CanvasException;
import edu.byu.cs.canvas.CanvasService;
import edu.byu.cs.canvas.CanvasUtils;
import edu.byu.cs.canvas.model.CanvasRubricAssessment;
import edu.byu.cs.canvas.model.CanvasSubmissionRubricItem;
import edu.byu.cs.canvas.model.CanvasSubmission;
import edu.byu.cs.dataAccess.DaoService;
import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.dataAccess.SubmissionDao;
import edu.byu.cs.dataAccess.UserDao;
import edu.byu.cs.model.Rubric;
import edu.byu.cs.model.RubricConfig;
import edu.byu.cs.model.Submission;
import edu.byu.cs.model.User;
import edu.byu.cs.util.PhaseUtils;
import org.eclipse.jgit.api.Git;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;

public class Scorer {
    private static final Logger LOGGER = LoggerFactory.getLogger(Scorer.class);

    /**
     * The penalty to be applied per day to a late submission.
     * This is out of 1. So putting 0.1 would be a 10% deduction per day
     */
    private static final float PER_DAY_LATE_PENALTY = 0.1F;
    private final GradingContext gradingContext;

    public Scorer(GradingContext gradingContext) {
        this.gradingContext = gradingContext;
    }

    public Submission score(Rubric rubric, int numCommits) throws GradingException, DataAccessException {
        gradingContext.observer().update("Grading...");

        rubric = CanvasUtils.decimalScoreToPoints(gradingContext.phase(), rubric);
        rubric = annotateRubric(rubric);

        // skip penalties if running in admin mode
        if (gradingContext.admin() || !PhaseUtils.isPhaseGraded(gradingContext.phase())) {
            return saveResults(rubric, numCommits, 0, getScore(rubric), "");
        }

        int daysLate = new LateDayCalculator().calculateLateDays(gradingContext.phase(), gradingContext.netId());
        float thisScore = calculateScoreWithLatePenalty(rubric, daysLate);
        Submission thisSubmission;

        // prevent score from being saved to canvas if it will lower their score
        if(rubric.passed()) {
            UserDao userDao = DaoService.getUserDao();
            User user = userDao.getUser(gradingContext.netId());
            int canvasUserId = user.canvasUserId();
            int assignmentNum = PhaseUtils.getPhaseAssignmentNumber(gradingContext.phase());

            RubricConfig rubricConfig = DaoService.getRubricConfigDao().getRubricConfig(gradingContext.phase());
            float lateAdjustment = daysLate * PER_DAY_LATE_PENALTY;
            CanvasRubricAssessment assessment = CanvasUtils.convertToAssessment(rubric, rubricConfig, lateAdjustment);

            // prevent score from being saved to canvas if it will lower their score
            if (wouldLowerScore(canvasUserId, assignmentNum, assessment)) {
                String notes = "Submission did not improve current score. Score not saved to Canvas.\n";
                thisSubmission = saveResults(rubric, numCommits, daysLate, thisScore, notes);
            } else {
                thisSubmission = saveResults(rubric, numCommits, daysLate, thisScore, "");
                sendToCanvas(canvasUserId, assignmentNum, assessment, rubric.notes());
            }
        }
        else {
            thisSubmission = saveResults(rubric, numCommits, daysLate, thisScore, "");
        }
        return thisSubmission;
    }

    /**
     * Annotates the rubric with notes and passed status
     *
     * @param rubric the rubric to annotate
     * @return the annotated rubric
     */
    private Rubric annotateRubric(Rubric rubric) {
        return new Rubric(
                rubric.passoffTests(),
                rubric.unitTests(),
                rubric.quality(),
                passed(rubric),
                rubric.notes()
        );
    }

    private boolean passed(Rubric rubric) {
        boolean passed = true;

        boolean isPassoffRequired = PhaseUtils.isPassoffRequired(gradingContext.phase());
        if (isPassoffRequired && rubric.passoffTests() != null && rubric.passoffTests().results() != null)
            if (rubric.passoffTests().results().score() < rubric.passoffTests().results().possiblePoints())
                passed = false;

        return passed;
    }

    private boolean wouldLowerScore(int userId, int assignmentNum,
                                    CanvasRubricAssessment assessment) {
        try {
            CanvasSubmission submission =
                    CanvasService.getCanvasIntegration().getSubmission(userId, assignmentNum);
            float prevPoints = (submission.score() != null) ? submission.score() : 0;
            CanvasRubricAssessment compareAssessment = assessment;

            if(submission.rubric_assessment() != null) {
                prevPoints = Math.max(prevPoints, totalPoints(submission.rubric_assessment()));

                HashMap<String, CanvasSubmissionRubricItem> compareItems = new HashMap<>();
                compareItems.putAll(submission.rubric_assessment().items());
                compareItems.putAll(assessment.items());
                compareAssessment = new CanvasRubricAssessment(compareItems);
            }

            float newPoints = totalPoints(compareAssessment);
            return newPoints <= prevPoints;
        } catch (CanvasException e) {
            LOGGER.error("Exception from canvas", e);
            return true;
        }
    }

    private float totalPoints(CanvasRubricAssessment assessment) {
        float points = 0;
        if(assessment == null) return points;
        for(CanvasSubmissionRubricItem item : assessment.items().values()) {
            points += item.points();
        }
        return points;
    }

    private float calculateScoreWithLatePenalty(Rubric rubric, int numDaysLate) throws GradingException, DataAccessException {
        float score = getScore(rubric);
        score *= 1 - (numDaysLate * PER_DAY_LATE_PENALTY);
        if (score < 0) score = 0;
        return score;
    }

    /**
     * Gets the score for the phase
     *
     * @return the score
     */
    private float getScore(Rubric rubric) throws GradingException, DataAccessException {
        int totalPossiblePoints = DaoService.getRubricConfigDao().getPhaseTotalPossiblePoints(gradingContext.phase());

        if (totalPossiblePoints == 0)
            throw new GradingException("Total possible points for phase " + gradingContext.phase() + " is 0");

        float score = 0;
        if (rubric.passoffTests() != null)
            score += rubric.passoffTests().results().score();

        if (rubric.unitTests() != null)
            score += rubric.unitTests().results().score();

        if (rubric.quality() != null)
            score += rubric.quality().results().score();

        return score / totalPossiblePoints;
    }

    /**
     * Saves the results of the grading to the database if the submission passed
     *
     * @param rubric the rubric for the phase
     */
    private Submission saveResults(Rubric rubric, int numCommits, int numDaysLate, float score, String notes)
            throws GradingException, DataAccessException {
        String headHash = getHeadHash();
        String netId = gradingContext.netId();

        if (numDaysLate > 0)
            notes += numDaysLate + " days late. -" + (numDaysLate * 10) + "%";

        // FIXME: this is code duplication from calculateLateDays()
        ZonedDateTime handInDate = DaoService.getQueueDao().get(netId).timeAdded().atZone(ZoneId.of("America/Denver"));

        SubmissionDao submissionDao = DaoService.getSubmissionDao();
        Submission submission = new Submission(
                netId,
                gradingContext.repoUrl(),
                headHash,
                handInDate.toInstant(),
                gradingContext.phase(),
                rubric.passed(),
                score,
                numCommits,
                notes,
                rubric,
                gradingContext.admin()
        );

        submissionDao.insertSubmission(submission);
        return submission;
    }

    private String getHeadHash() throws GradingException {
        String headHash;
        try (Git git = Git.open(gradingContext.stageRepo())) {
            headHash = git.getRepository().findRef("HEAD").getObjectId().getName();
        } catch (IOException e) {
            throw new GradingException("Failed to get head hash: " + e.getMessage());
        }
        return headHash;
    }

    private void sendToCanvas(int userId, int assignmentNum, CanvasRubricAssessment assessment, String notes) throws GradingException {
        try {
            CanvasService.getCanvasIntegration().submitGrade(userId, assignmentNum, assessment, notes);
        } catch (CanvasException e) {
            LOGGER.error("Error submitting to canvas for user {}", gradingContext.netId(), e);
            throw new GradingException("Error contacting canvas to record scores");
        }
    }
}
