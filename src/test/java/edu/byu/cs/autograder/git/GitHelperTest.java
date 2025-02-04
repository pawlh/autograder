package edu.byu.cs.autograder.git;

import edu.byu.cs.analytics.CommitThreshold;
import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.*;
import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

class GitHelperTest {
    GitHelperUtils utils;

    @AfterAll
    static void cleanUp() {
        GitHelperUtils.cleanUpTests();
    }

    @BeforeEach
    void beforeEach() {
        utils = new GitHelperUtils();
    }

    /**
     * This test is designed to help with manual debugging.
     * <br>
     * Given a pre-organized git repository, by its full path name,
     * it evaluates the git repo and prints out the results.
     * <br>
     * If the repository doesn't exist, this test does nothing.
     */
    @Test
    @Disabled
    void arbitraryRepoFileTest() {
        String repoPath;
        repoPath = "/Users/frozenfrank/Documents/College/Spring_2024/CS_240_TA/student_repos/dant329";
        File repo = new File(repoPath);
        if (!repo.exists()) return;

        System.out.printf("Evaluating repo at path:\n\t%s%n\n", repoPath);
        CommitThreshold minThreshold = null;
//        minThreshold = new CommitThreshold(Instant.MIN, "6ec3b15af512f42cfad177388b6422b60309cc5e");
        var result = utils.withTestRepo(repo, utils.evaluateRepo(minThreshold));
        System.out.println("Finished results:");
        System.out.println(result);
    }

    @Test
    void multiPhaseSuccessfullPassoff() {
        utils.evaluateTest("multi-phase-successful-passoff", List.of(
                new VerificationCheckpoint(
                        repoContext -> {
                            utils.makeCommit(repoContext, "Change 1", 24, 39, 20);
                            utils.makeCommit(repoContext, "Change 2", 24, 38, 10);
                            utils.makeCommit(repoContext, "Change 3", 24, 37, 10);
                            utils.makeCommit(repoContext, "Change 4", 24, 36, 10);
                            utils.makeCommit(repoContext, "Change 5", 23, 35, 10);
                            utils.makeCommit(repoContext, "Change 6", 22, 34, 10);
                            utils.makeCommit(repoContext, "Change 7", 22, 33, 10);
                            utils.makeCommit(repoContext, "Change 8", 22, 32, 10);
                            utils.makeCommit(repoContext, "Change 9", 21, 31, 10);
                            utils.makeCommit(repoContext, "Change 10", 20, 30, 10);
                        },
                        utils.generalCommitVerificationResult(true, 10, 5, 0)
                ),
                new VerificationCheckpoint(
                        repoContext -> {
                            utils.makeCommit(repoContext, "Change 11", 14, 29, 10);
                            utils.makeCommit(repoContext, "Change 12", 14, 28, 10);
                            utils.makeCommit(repoContext, "Change 13", 14, 27, 10);
                            utils.makeCommit(repoContext, "Change 14", 14, 26, 10);
                            utils.makeCommit(repoContext, "Change 15", 13, 25, 10);
                            utils.makeCommit(repoContext, "Change 16", 12, 24, 10);
                            utils.makeCommit(repoContext, "Change 17", 12, 23, 10);
                            utils.makeCommit(repoContext, "Change 18", 12, 22, 10);
                            utils.makeCommit(repoContext, "Change 19", 11, 21, 10);
                            utils.makeCommit(repoContext, "Change 20", 10, 20, 10);
                        },
                        utils.generalCommitVerificationResult(true, 10, 5, 0)
                ),
                new VerificationCheckpoint(
                        repoContext -> {
                            utils.makeCommit(repoContext, "Change 31", 4, 19, 10);
                            utils.makeCommit(repoContext, "Change 32", 4, 18, 10);
                            utils.makeCommit(repoContext, "Change 33", 4, 17, 10);
                            utils.makeCommit(repoContext, "Change 34", 4, 16, 10);
                            utils.makeCommit(repoContext, "Change 35", 3, 15, 10);
                            utils.makeCommit(repoContext, "Change 36", 2, 14, 10);
                            utils.makeCommit(repoContext, "Change 37", 2, 13, 10);
                            utils.makeCommit(repoContext, "Change 38", 2, 12, 10);
                            utils.makeCommit(repoContext, "Change 39", 1, 11, 10);
                            utils.makeCommit(repoContext, "Change 40", 0, 10, 10);
                        },
                        utils.generalCommitVerificationResult(true, 10, 5, 0)
                )
        ));
    }

    @Test
    void insufficientCommits() {
        utils.evaluateTest("insufficient-commits", new VerificationCheckpoint(
                repoContext -> {
                    utils.makeCommit(repoContext, "Change 1", 24, 39, 20);
                    utils.makeCommit(repoContext, "Change 2", 24, 38, 10);
                    utils.makeCommit(repoContext, "Change 3", 24, 37, 10);
                    utils.makeCommit(repoContext, "Change 4", 24, 36, 10);
                    utils.makeCommit(repoContext, "Change 5", 23, 35, 10);
                    utils.makeCommit(repoContext, "Change 6", 22, 34, 10);
                    utils.makeCommit(repoContext, "Change 7", 22, 33, 10);
                    utils.makeCommit(repoContext, "Change 8", 22, 32, 10);
                    utils.makeCommit(repoContext, "Change 9", 21, 31, 10);
                    // Notice only nine commits
                },
                utils.generalCommitVerificationResult(false, 9, 4)
        ));
    }

    @Test
    void sufficientCommitsOnInsufficientDays() {
        utils.evaluateTest("sufficient-commits-insufficient-days", new VerificationCheckpoint(
                repoContext -> {
                    // Four commits two days ago
                    utils.makeCommit(repoContext, "Change 1", 2, 39, 20);
                    utils.makeCommit(repoContext, "Change 2", 2, 38, 10);
                    utils.makeCommit(repoContext, "Change 3", 2, 37, 10);
                    utils.makeCommit(repoContext, "Change 4", 2, 36, 10);
                    // Six commits today
                    utils.makeCommit(repoContext, "Change 5", 0, 35, 10);
                    utils.makeCommit(repoContext, "Change 6", 0, 34, 10);
                    utils.makeCommit(repoContext, "Change 7", 0, 33, 10);
                    utils.makeCommit(repoContext, "Change 8", 0, 32, 10);
                    utils.makeCommit(repoContext, "Change 9", 0, 31, 10);
                    utils.makeCommit(repoContext, "Change 10", 0, 30, 10);
                },
                utils.generalCommitVerificationResult(false, 10, 2)
        ));
    }

    @Test
    void commitsOutOfOrder() {
        utils.evaluateTest("commits-out-of-order", new VerificationCheckpoint(
                repoContext -> {
                    utils.makeCommit(repoContext, "Change 1", 2, 39, 20);
                    utils.makeCommit(repoContext, "Change 5", 1, 35, 10); // Cherry-picked!
                    utils.makeCommit(repoContext, "Change 2", 2, 38, 10);
                    utils.makeCommit(repoContext, "Change 3", 2, 37, 10);
                    utils.makeCommit(repoContext, "Change 4", 2, 36, 10);
                    utils.makeCommit(repoContext, "Change 6", 0, 34, 10);
                    utils.makeCommit(repoContext, "Change 7", 0, 33, 10);
                    utils.makeCommit(repoContext, "Change 8", 0, 32, 10);
                    utils.makeCommit(repoContext, "Change 9", 0, 31, 10);
                    utils.makeCommit(repoContext, "Change 10", 0, 30, 10);
                },
                utils.generalCommitVerificationResult(true, 10, 3, 2)
        ));
    }

    @Test
    void insignificantCommits() {
        utils.setGradingContext(utils.generateGradingContext(10, 0, 0, 10));
        utils.evaluateTest("insignificant-commits", new VerificationCheckpoint(
                repoContext -> {
                    utils.makeCommit(repoContext, "Change 1", 24, 39, 10); // Significant
                    utils.makeCommit(repoContext, "Change 2", 24, 38, 1); // Also significant
                    utils.makeCommit(repoContext, "Change 3", 24, 37, 4);
                    utils.makeCommit(repoContext, "Change 4", 24, 36, 1);
                    utils.makeCommit(repoContext, "Change 5", 23, 35, 4);
                    utils.makeCommit(repoContext, "Change 6", 22, 34, 6); // Significant
                    utils.makeCommit(repoContext, "Change 7", 22, 33, 1);
                    utils.makeCommit(repoContext, "Change 8", 22, 32, 1);
                    utils.makeCommit(repoContext, "Change 9", 21, 31, 1);
                    utils.makeCommit(repoContext, "Change 10", 20, 30, 900); // Significant
                },
                utils.generalCommitVerificationResult(false, 4, 10, 5, false)
        ));
    }

    @Test
    void simpleBackdatingCommits() {
        utils.setGradingContext(utils.generateGradingContext(3, 0, 10, 0));
        utils.evaluateTest("simple-backdating-commits", new VerificationCheckpoint(
                repoContext -> {
                    utils.makeCommit(repoContext, "Change 1", 2, 20, 10, false);
                    utils.makeCommit(repoContext, "Change 2", 1, 0, 10, false); // Simply backdated!
                    utils.makeCommit(repoContext, "Change 3", 0, 10, 10, false);
                },
                utils.generalCommitVerificationResult(false, 3, 3)
        ));
    }

    /**
     * <h1>Important Note!</h1>
     * This test is not actually evaluating the logic in the app that performs this test.
     * <br>
     * The logic currently occurs in <code>GitHelper#constructCurrentThreshold(Git)</code> which is bypassed when we directly
     * call {@link GitHelper#verifyRegularCommits(Git, CommitThreshold, CommitThreshold)}.
     * Normal calls to {@link GitHelper#verifyCommitRequirements(File)} will evaluate the rules.
     * <br>
     * Note that we cannot easily call the <code>constructCurrentThreshold(Git)</code> method since it relies
     * on data configured in the QueueTable and other complex dependencies that are tricky to set up.
     * <br>
     * Instead, we independently implemented the (simple) logic in {@link GitHelperUtils#evaluateRepo(GitHelper, CommitThreshold)}
     * method which demonstrates that the technique does work.
     */
    @Test
    void extendForgivenessMinutes() {
        utils.setGradingContext(utils.generateGradingContext(1, 0, 10, 0, 3));
        utils.evaluateTest("extend-forgiveness-minutes", List.of(
                new VerificationCheckpoint(
                        repoContext -> utils.makeCommit(repoContext, "Change 1", 0, -1, 10), // Minor clock issue
                        utils.generalCommitVerificationResult(true, 1, 1)),
                new VerificationCheckpoint(
                        repoContext -> utils.makeCommit(repoContext, "Change 2", 0, -6, 10), // Major clock issue
                        utils.generalCommitVerificationResult(false, 1, 1))
        ));
    }

    @Test
    void amendedCommitsCountedOnce() {
        utils.setGradingContext(utils.generateGradingContext(3, 0, 0, 0));
        utils.evaluateTest("amended-commits-counted-once", new VerificationCheckpoint(repoContext -> {
            Instant now = Instant.now();

            Instant commitTime1 = now.minus(Duration.ofMinutes(5));
            utils.makeCommit(repoContext, "Change 1 (initial)", commitTime1, 10, true);
            utils.makeCommit(repoContext, "Change 1 (amend 1)", commitTime1, 10, true);
            utils.makeCommit(repoContext, "Change 1 (amend 2)", commitTime1, 10, true);

            Instant commitTime2 = now.minus(Duration.ofMinutes(4));
            utils.makeCommit(repoContext, "Change 2 (initial)", commitTime2, 10, true);
            utils.makeCommit(repoContext, "Change 2 (amend 1)", commitTime2, 10, true);
            utils.makeCommit(repoContext, "Change 2 (amend 2)", commitTime2, 10, true);

            Instant commitTime3 = now.minus(Duration.ofMinutes(3));
            utils.makeCommit(repoContext, "Change 3 (initial)", commitTime3, 10, true);
            utils.makeCommit(repoContext, "Change 3 (amend 1)", commitTime3, 10, true);
            utils.makeCommit(repoContext, "Change 3 (amend 2)", commitTime3, 10, true);
            utils.makeCommit(repoContext, "Change 3 (amend 3)", commitTime3, 10, true);
        }, utils.generalCommitVerificationResult(true, 3, 1, 2)));
    }

    @Test
    @Disabled
    void verifyCommitRequirements() {
        // Verify status preservation on repeat submissions
        // Fails when submitting new phase with same head hash
        // Works when a non-graded phase has already been submitted
    }

    @Test
    @Disabled
    void verifyRegularCommits() {
        // Counts commits from merges properly
        // Commits authored after the head timestamp trigger failure
        // Commits authored before the tail timestamp trigger failure
    }

    @Test
    void passoffMissingTailHash() {
        utils.setGradingContext(utils.generateGradingContext(1, 1, 10,  1));
        Collection<String> submissionWarnings = List.of("Missing tail hash", "Additional warning");
        utils.evaluateTest("missing-tail-commit", List.of(
                new VerificationCheckpoint(
                    repoContext -> {
                        utils.makeCommit(repoContext, "Change 1", 1, 9, 20);
                    },
                    utils.generalCommitVerificationResult(true, 1, 1)),
                new VerificationCheckpoint(
                    repoContext -> {
                        utils.makeCommit(repoContext, "Change 2", 0, 3, 10);

                        // NOTE: I tried putting in an *obviously* incorrect head hash for testing, but it JGit rejected
                        // it with an InvalidObjectIdException. Apparently the ObjectIds cannot be any alphanumeric string.
                        utils.setPrevSubmissionHeadHash("f6fbf36bd4f932177df1bc70fbd5a32da288c6d7"); // Commit doesn't exist
                        utils.setPrevSubmissionTimestamp(Instant.now().minus(Duration.ofMinutes(30))); // Submitted between the two phases
                    },
                    // Since the tail hash doesn't exist, it will evaluate the entire repository.
                    // Only the commits since the last pass-off are counted, and the submission will not be blocked.
                    utils.generalCommitVerificationResult(true, 1, 1, 1, true, submissionWarnings))
        ));
    }

    @Test
    void passoffEarlyInsufficientDays() {
        utils.setGradingContext(utils.generateGradingContext(1, 3, 10, 1));
        utils.setSubmitDaysEarly(1);
        utils.evaluateTest("passoff-early-insufficient-days", new VerificationCheckpoint(
                repoContext -> {
                        utils.makeCommit(repoContext, "Change 1", 5, 2, 20);
                        utils.makeCommit(repoContext, "Change 2", 5, 1, 20);
                        utils.makeCommit(repoContext, "Change 3", 4, 2, 20);
                        utils.makeCommit(repoContext, "Change 4", 4, 1, 20);
                },
                utils.generalCommitVerificationResult(true, 4, 2, 2)) // Has warnings
        );
    }

    @Test
    void earlyCommitsExcludedButDoNotError() {
        utils.setGradingContext(utils.generateGradingContext(1, 1, 10, 1));
        utils.evaluateTest("commits-before-previous-submission", List.of(
                new VerificationCheckpoint(
                        repoContext -> utils.makeCommit(repoContext, "Change 1", 0, 6, 10),
                        utils.generalCommitVerificationResult(true, 1, 1)),
                new VerificationCheckpoint(
                        repoContext -> {
                            utils.setPrevSubmissionTimestamp(Instant.now().minusSeconds(5 * 60));
                            utils.makeCommit(repoContext, "Change 2", 0, 9, 10); // Before the prev commit
                            utils.makeCommit(repoContext, "Change 3", 0, 4, 10);
                        },
                        utils.generalCommitVerificationResult(true, 1, 1, 2))
        ));
    }
}
