package edu.byu.cs.dataAccess;

public interface ConfigurationDao {
    <T> void setConfiguration(Configuration key, T value, Class<T> type) throws DataAccessException;
    <T> T getConfiguration(Configuration key, Class<T> type) throws DataAccessException;

    enum Configuration {
        STUDENT_SUBMISSIONS_ENABLED,
        GRADER_SHUTDOWN_DATE,
        GRADER_SHUTDOWN_WARNING_MILLISECONDS,
        BANNER_MESSAGE,
        BANNER_LINK,
        BANNER_COLOR,
        BANNER_EXPIRATION,
        GITHUB_ASSIGNMENT_NUMBER,
        PHASE0_ASSIGNMENT_NUMBER,
        PHASE1_ASSIGNMENT_NUMBER,
        PHASE3_ASSIGNMENT_NUMBER,
        PHASE4_ASSIGNMENT_NUMBER,
        PHASE5_ASSIGNMENT_NUMBER,
        PHASE6_ASSIGNMENT_NUMBER,
        COURSE_NUMBER,
        MAX_LATE_DAYS_TO_PENALIZE,
        PER_DAY_LATE_PENALTY,
        GIT_COMMIT_PENALTY,
        LINES_PER_COMMIT_REQUIRED,
        CLOCK_FORGIVENESS_MINUTES,
        HOLIDAY_LIST
    }
}
