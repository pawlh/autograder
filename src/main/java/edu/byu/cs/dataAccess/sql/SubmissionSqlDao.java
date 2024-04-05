package edu.byu.cs.dataAccess.sql;

import com.google.gson.Gson;
import edu.byu.cs.dataAccess.sql.helpers.ColumnDefinition;
import edu.byu.cs.dataAccess.sql.helpers.SqlReader;
import edu.byu.cs.model.Rubric;
import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.dataAccess.SubmissionDao;
import edu.byu.cs.model.Phase;
import edu.byu.cs.model.Submission;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

import static java.sql.Types.NULL;

public class SubmissionSqlDao implements SubmissionDao {
    private static final ColumnDefinition[] COLUMN_DEFINITIONS = {
            new ColumnDefinition<Submission>("net_id", Submission::netId),
            new ColumnDefinition<Submission>("repo_url", Submission::repoUrl),
            new ColumnDefinition<Submission>("timestamp", s -> Timestamp.from(s.timestamp())),
            new ColumnDefinition<Submission>("phase", s -> s.phase().toString()),
            new ColumnDefinition<Submission>("passed", Submission::passed),
            new ColumnDefinition<Submission>("score", Submission::score),
            new ColumnDefinition<Submission>("num_commits", Submission::numCommits),
            new ColumnDefinition<Submission>("head_hash", Submission::headHash),
            new ColumnDefinition<Submission>("notes", Submission::notes),
            new ColumnDefinition<Submission>("rubric", s -> new Gson().toJson(s.rubric())),
            new ColumnDefinition<Submission>("admin", Submission::admin)
    };
    private static Submission readSubmission(ResultSet rs) throws SQLException {
        String netId = rs.getString("net_id");
        String repoUrl = rs.getString("repo_url");
        String headHash = rs.getString("head_hash");
        Instant timestamp = rs.getTimestamp("timestamp").toInstant();
        Phase phase = Phase.valueOf(rs.getString("phase"));
        Boolean passed = rs.getBoolean("passed");
        float score = rs.getFloat("score");
        Integer numCommits = rs.getInt("num_commits");
        String notes = rs.getString("notes");
        Rubric rubric = new Gson().fromJson(rs.getString("rubric"), Rubric.class);
        Boolean admin = rs.getBoolean("admin");

        return new Submission(netId, repoUrl, headHash, timestamp, phase, passed, score, numCommits, notes, rubric, admin);
    }

    private final SqlReader<Submission> sqlReader = new SqlReader<Submission>(
            "submission", COLUMN_DEFINITIONS, SubmissionSqlDao::readSubmission);

    @Override
    public void insertSubmission(Submission submission) {
        sqlReader.insertItem(submission);
    }
    @Override
    public Collection<Submission> getSubmissionsForPhase(String netId, Phase phase) {
        return sqlReader.executeQuery(
                "WHERE net_id = ? AND phase = ?",
                ps -> {
                    ps.setString(1, netId);
                    ps.setString(2, phase.toString());
                }
        );
    }

    @Override
    public Collection<Submission> getSubmissionsForUser(String netId) {
        return sqlReader.executeQuery(
                "WHERE net_id = ?",
                ps -> ps.setString(1, netId));
    }

    @Override
    public Collection<Submission> getAllLatestSubmissions() {
        return getAllLatestSubmissions(-1);
    }

    @Override
    public Collection<Submission> getAllLatestSubmissions(int batchSize) {
        return sqlReader.executeQuery(
                """
                    WHERE timestamp IN (
                        SELECT MAX(timestamp)
                        FROM %s
                        GROUP BY net_id, phase
                    )
                    ORDER BY timestamp DESC
                    """.formatted(sqlReader.getTableName()) +
                (batchSize >= 0 ? "LIMIT ?" : ""),
                ps -> {
                    if (batchSize >= 0) {
                        ps.setInt(1, batchSize);
                    }
                });
    }

    @Override
    public void removeSubmissionsByNetId(String netId) {
        sqlReader.executeUpdate(
                """
                    DELETE FROM %s
                    WHERE net_id = ?
                    """.formatted(sqlReader.getTableName()),
                ps -> ps.setString(1, netId)
        );
    }

    @Override
    public Submission getFirstPassingSubmission(String netId, Phase phase) {
        var submissions = sqlReader.executeQuery(
                """
                        WHERE net_id = ? AND phase = ? AND passed = 1
                        ORDER BY timestamp
                        LIMIT 1
                        """,
                ps -> {
                    ps.setString(1, netId);
                    ps.setString(2, phase.toString());
                }
        );
        return sqlReader.expectOneItem(submissions);
    }

    @Override
    public float getBestScoreForPhase(String netId, Phase phase) {
        return sqlReader.executeQuery(
                """
                    SELECT max(score) as highestScore
                    FROM %s
                    WHERE net_id = ? AND phase = ?
                    """.formatted(sqlReader.getTableName()),
                ps -> {
                    ps.setString(1, netId);
                    ps.setString(2, phase.toString());
                },
                rs -> {
                    rs.next();
                    return rs.getFloat("highestScore");
                }
        );
    }

    @Override
    public void clear() {
        sqlReader.executeUpdate("TRUNCATE TABLE %s".formatted(sqlReader.getTableName()));
    }
}
