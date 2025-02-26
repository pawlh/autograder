package edu.byu.cs.model;

import java.util.EnumMap;

/**
 * Represents the rubric for a Canvas assignment. Some rubrics may have null values for some fields.
 *
 * @param items
 * @param passed
 * @param notes
 */
public record Rubric(
        EnumMap<RubricType, RubricItem> items,
        boolean passed,
        String notes
) {

    /**
     * Represents a single rubric item
     *
     * @param category
     * @param results  The results of the rubric item
     * @param criteria The criteria of the rubric item
     */
    public record RubricItem(
            String category,
            Results results,
            String criteria
    ) { }

    /**
     * Represents the results of a rubric item. textResults or testResults may be null, but not both
     *
     * @param notes
     * @param score
     * @param testResults
     * @param textResults
     * @param possiblePoints
     */
    public record Results(
            String notes,
            Float score,
            Float rawScore,
            Integer possiblePoints,
            TestOutput testResults,
            String textResults
    ) {
        public Results(String notes, Float score, Integer possiblePoints, TestOutput testResults, String textResults) {
            this(notes, score, score, possiblePoints, testResults, textResults);
        }

        public static Results testError(String notes, TestOutput testResults) {
            return new Results(notes, 0f, 0, testResults, null);
        }

        public static Results textError(String notes, String textResults) {
            return new Results(notes, 0f, 0, null, textResults);
        }
    }

    public enum RubricType {
        PASSOFF_TESTS,
        UNIT_TESTS,
        QUALITY,
        GIT_COMMITS,
        GITHUB_REPO,
        GRADING_ISSUE
    }
}
