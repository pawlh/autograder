package edu.byu.cs.model;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record PrivateConfig(
    PenaltyConfig penalty,
    int courseNumber,
    List<AssignmentConfig> assignments,
    String[] holidays
) {
    public record PenaltyConfig(
        float perDayLatePenalty,
        float gitCommitPenalty,
        int maxLateDaysPenalized,
        int linesChangedPerCommit,
        int clockForgivenessMinutes
    ){}

    public record AssignmentConfig(
            Phase phase,
            int assignmentId,
            Map<Rubric.RubricType, RubricConfig.RubricConfigItem> rubricItems
    ){}
}
