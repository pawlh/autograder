package edu.byu.cs.dataAccess;

import edu.byu.cs.model.Phase;
import edu.byu.cs.model.Submission;

import java.util.Collection;

public interface SubmissionDao {

    /**
     * Inserts a new submission into the database
     *
     * @param submission the submission to insert
     */
    void insertSubmission(Submission submission);

    /**
     * Gets all submissions for the given netId and phase
     *
     * @param netId the netId to get submissions for
     * @param phase the phase to get submissions for
     * @return all submissions for the given netId and phase
     */
    Collection<Submission> getSubmissionsForPhase(String netId, Phase phase);

    /**
     * Gets all submissions for the given netId
     *
     * @param netId the netId to get submissions for
     * @return all submissions for the given netId
     */
    Collection<Submission> getSubmissionsForUser(String netId);

    /**
     * Gets all latest submissions
     *
     * @return all latest submissions
     */
    Collection<Submission> getAllLatestSubmissions();

    /**
     * Gets the X most recent latest submissions
     *
     * @param batchSize defines how many submissions to return. Set batchSize to a negative int to get All submissions
     * @return the most recent X submissions
     */
    Collection<Submission> getAllLatestSubmissions(int batchSize);

    /**
     * Removes all submissions for the given netId
     * <br/><strong>Note: this will likely only be used for the test student</strong>
     *
     * @param netId the netId to remove submissions for
     */
    void removeSubmissionsByNetId(String netId);

    /**
     * Gets the first passing submission chronologically for the given phase
     *
     * @param netId the student's netId
     * @param phase the phase
     * @return the submission object, or null
     */
    Submission getFirstPassingSubmission(String netId, Phase phase);

    float getBestScoreForPhase(String netId, Phase phase);

    /**
     * <p>Clears the entire DAO of data.</p>
     * <p><b>Method only to be used for internal testing.</b></p>
     * <p>Should not be exposed outside of this class.</p>
     */
    void clear();
}
