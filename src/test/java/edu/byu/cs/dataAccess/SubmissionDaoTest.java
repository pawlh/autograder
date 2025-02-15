package edu.byu.cs.dataAccess;

import edu.byu.cs.dataAccess.sql.SqlDb;
import edu.byu.cs.dataAccess.sql.SubmissionSqlDao;
import edu.byu.cs.model.Phase;
import edu.byu.cs.model.Submission;
import org.junit.jupiter.api.*;

import java.time.Instant;
import java.util.Random;

class SubmissionDaoTest {
    static Random random;
    SubmissionDao dao;

    Submission firstSubmission;


    @BeforeAll
    static void prepareDatabase() throws DataAccessException {
        random = new Random();
        SqlDb.setUpDb();
    }

    @BeforeEach
    void setup() {
        dao = new SubmissionSqlDao();

        firstSubmission = generateSubmission();
        Assertions.assertDoesNotThrow(() -> dao.insertSubmission(firstSubmission));
    }

    @Test
    void submissionSerialization() throws DataAccessException {
        var readSubmission = dao.getFirstPassingSubmission(firstSubmission.netId(), firstSubmission.phase());
        Assertions.assertEquals(firstSubmission, readSubmission);
    }

    @Test
    @Disabled
    void insertSubmission() {
    }

    @Test
    @Disabled
    void getSubmissionsForPhase() {
    }

    private Submission generateSubmission() {
        int cosmo_id = random.nextInt();
        return new Submission(
                "cosmo_" + cosmo_id,
                "https://github.com/cosmo_%s/chess".formatted(cosmo_id),
                "fc80e76ee5bfa331840bc75d1a5efec2cc7a874c",
                Instant.now().minusSeconds(60L * random.nextInt(1, 100)),
                Phase.Phase0,
                true,
                1.618f,
                3.1415f,
                "This is only a testing submission for Cosmo Cougar (#%s).".formatted(cosmo_id),
                null,
                true,
                Submission.VerifiedStatus.ApprovedManually,
                null, null,
                new Submission.ScoreVerification(100.1f, "cosmo_boss", Instant.now(),10)
        );
    }
}
