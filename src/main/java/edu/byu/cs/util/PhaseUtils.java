package edu.byu.cs.util;

import edu.byu.cs.autograder.GradingException;
import edu.byu.cs.autograder.git.CommitValidation.CommitVerificationConfig;
import edu.byu.cs.dataAccess.daoInterface.ConfigurationDao;
import edu.byu.cs.dataAccess.DaoService;
import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.model.Phase;
import edu.byu.cs.model.Rubric;
import edu.byu.cs.model.RubricConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class PhaseUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(PhaseUtils.class);

    public static boolean isPhaseEnabled(Phase phase) throws DataAccessException {
        boolean phaseEnabled;

        try {
            phaseEnabled = DaoService.getConfigurationDao()
                    .getConfiguration(ConfigurationDao.Configuration.STUDENT_SUBMISSIONS_ENABLED, String.class)
                    .contains(phase.toString());
        } catch (DataAccessException e) {
            LOGGER.error("Error getting configuration for live phase", e);
            throw e;
        }

        return phaseEnabled;
    }

    /**
     * Given a phase, returns the phase before it, or null.
     *
     * @param phase the current phase
     * @return the previous phase chronologically
     */
    public static Phase getPreviousPhase(Phase phase) {
        return switch (phase) {
            case Phase0, Quality, Commits, GitHub -> null;
            case Phase1 -> Phase.Phase0;
            case Phase3 -> Phase.Phase1;
            case Phase4 -> Phase.Phase3;
            case Phase5 -> Phase.Phase4;
            case Phase6 -> Phase.Phase5;
        };
    }

    /**
     * Gives an integer representation of the Phase enum as a string
     *
     * @param phase the phase in question
     * @return the string
     */
    public static String getPhaseAsString(Phase phase) {
        return switch (phase) {
            case Phase0 -> "0";
            case Phase1 -> "1";
            case Phase3 -> "3";
            case Phase4 -> "4";
            case Phase5 -> "5";
            case Phase6 -> "6";
            case Quality -> "Quality";
            case Commits -> "GitCommits";
            case GitHub -> "GitHub";
        };
    }

    /**
     * Produces the Canvas assignment number for this phase
     *
     * @param phase the phase in question
     * @return its assignment number in Canvas
     */
    public static int getPhaseAssignmentNumber(Phase phase) throws DataAccessException {
        if (!PhaseUtils.isPhaseGraded(phase)) {
            throw new IllegalArgumentException("No phase assignment number for " + phase);
        }
        return DaoService.getConfigurationDao().getConfiguration(
                PhaseUtils.getConfigurationAssignmentNumber(phase),
                Integer.class);
    }

    public static String getCanvasRubricId(Rubric.RubricType type, Phase phase) throws DataAccessException {
        if (!PhaseUtils.isPhaseGraded(phase)) {
            throw new IllegalArgumentException("No canvas rubric for ungraded phase: " + phase);
        }
        RubricConfig.RubricConfigItem configItem = DaoService.getRubricConfigDao()
                .getRubricConfig(phase)
                .items()
                .get(type);
        if (configItem == null) {
            throw new IllegalArgumentException(String.format("No '%s' item for phase '%s'", type, phase));
        }
        return configItem.rubric_id();
    }

    public static Set<String> passoffPackagesToTest(Phase phase) throws GradingException {
        return switch (phase) {
            case Phase0 -> Set.of("passoff.chess", "passoff.chess.piece");
            case Phase1 -> Set.of("passoff.chess.game", "passoff.chess.extracredit");
            case Phase3, Phase4, Phase6 -> Set.of("passoff.server");
            case Phase5, Quality, GitHub, Commits -> throw new GradingException("No passoff tests for this phase");
        };
    }

    public static Set<String> unitTestPackagesToTest(Phase phase) throws GradingException {
        return switch (phase) {
            case Phase0, Phase1, Phase6, Quality, GitHub, Commits -> throw new GradingException("No unit tests for this phase");
            case Phase3 -> Set.of("service");
            case Phase4 -> Set.of("dataaccess");
            case Phase5 -> Set.of("client");
        };
    }

    public static Set<String> unitTestPackagePaths(Phase phase) {
        return switch (phase) {
            case Phase0, Phase6, Quality, GitHub, Commits -> new HashSet<>();
            case Phase1 -> Set.of("shared/src/test/java/passoff/chess/game");
            case Phase3 -> Set.of("server/src/test/java/service", "server/src/test/java/passoff/server");
            case Phase4 -> Set.of("server/src/test/java/dataaccess");
            case Phase5 -> Set.of("client/src/test/java/client");
        };
    }

    public static String unitTestCodeUnderTest(Phase phase) throws GradingException {
        return switch (phase) {
            case Phase0, Phase1, Phase6, Quality, GitHub, Commits -> throw new GradingException("No unit tests for this phase");
            case Phase3 -> "service";
            case Phase4 -> "dao";
            case Phase5 -> "server facade";
        };
    }

    public static int minUnitTests(Phase phase) throws GradingException {
        return switch (phase) {
            case Phase0, Phase1, Phase6, Quality, GitHub, Commits -> throw new GradingException("No unit tests for this phase");
            case Phase3 -> 13;
            case Phase4 -> 18;
            case Phase5 -> 12;
        };
    }

    public static Set<String> unitTestModulesToCheckCoverage(Phase phase) throws GradingException {
        return switch (phase) {
            case Phase0, Phase1, Phase6, Quality, GitHub, Commits -> throw new GradingException("No unit tests for this phase");
            case Phase3, Phase4 -> Set.of("server");
            case Phase5 -> Set.of("client"); //In case anyone tries this in the future, Jacoco won't like
                                                // trying to get the server and client at the same time
        };
    }

    public static String getModuleUnderTest(Phase phase) {
        return switch (phase) {
            case Phase0, Phase1 -> "shared";
            case Phase3, Phase4, Phase6 -> "server";
            case Phase5 -> "client";
            case Quality, GitHub, Commits -> null;
        };
    }

    public static boolean isPhaseGraded(Phase phase) {
        return switch (phase) {
            case Phase0, Phase1, Phase3, Phase4, Phase5, Phase6, GitHub -> true;
            case Quality, Commits -> false;
        };
    }

    /**
     * Returns the required rubric types to submit a phase
     *
     * @param phase phase to check
     * @return the required rubric types for the provided phase
     */
    public static Collection<Rubric.RubricType> requiredRubricTypes(Phase phase) {
        return switch (phase) {
            case Phase0, Phase1, Phase3, Phase4, Phase6 -> Set.of(Rubric.RubricType.PASSOFF_TESTS);
            case GitHub -> Set.of(Rubric.RubricType.GITHUB_REPO);
            case Phase5, Quality, Commits -> Set.of();
        };
    }

    public static Set<String> extraCreditTests(Phase phase) {
        if(phase == Phase.Phase1) return Set.of("CastlingTests", "EnPassantTests");
        return new HashSet<>();
    }

    public static float extraCreditValue(Phase phase) {
        if(phase == Phase.Phase1) return .04f;
        return 0;
    }

    public static CommitVerificationConfig verificationConfig(Phase phase) throws GradingException {
        ConfigurationDao dao = DaoService.getConfigurationDao();
        int minimumLinesChanged;
        int penaltyPct;
        int forgivenessMinutesHead;
        try {
            penaltyPct = Math.round(dao.getConfiguration(ConfigurationDao.Configuration.GIT_COMMIT_PENALTY, Float.class) * 100);
            minimumLinesChanged = dao.getConfiguration(ConfigurationDao.Configuration.LINES_PER_COMMIT_REQUIRED, Integer.class);
            forgivenessMinutesHead = dao.getConfiguration(ConfigurationDao.Configuration.CLOCK_FORGIVENESS_MINUTES, Integer.class);
        } catch (DataAccessException e) {
            throw new GradingException("Error getting git commit config", e);
        }

        return switch (phase) {
            case Phase0, Phase1 -> new CommitVerificationConfig(8, 2, minimumLinesChanged, penaltyPct, forgivenessMinutesHead);
            case Phase3, Phase4, Phase5, Phase6 -> new CommitVerificationConfig(12, 3, minimumLinesChanged, penaltyPct, forgivenessMinutesHead);
            case GitHub -> new CommitVerificationConfig(2, 0, 0, 0, forgivenessMinutesHead);
            case Quality, Commits -> throw new GradingException("No commit verification for this phase");
        };
    }

    public static boolean requiresTAPassoffForCommits(Phase phase) {
        return switch (phase) {
            case Phase0, Phase1, Phase3, Phase4, Phase5, Phase6 -> true;
            case Quality, GitHub, Commits -> false;
        };
    }

    public static boolean shouldVerifyCommits(Phase phase) {
        return switch (phase) {
            case Phase0, Phase1, Phase3, Phase4, Phase5, Phase6, GitHub -> true;
            case Quality, Commits -> false;
        };
    }

    public static boolean phaseHasCommitPenalty(Phase phase) {
        return switch (phase) {
            case GitHub, Quality, Commits -> false;
            case Phase0, Phase1, Phase3, Phase4, Phase5, Phase6 -> true;
        };
    }

    public static Phase getPhaseFromString(String phaseString) throws IllegalArgumentException {
        phaseString = phaseString.toLowerCase().replaceAll("\\s", "");
        if (phaseString.contains("phase0")) {
            return Phase.Phase0;
        } else if (phaseString.contains("phase1")) {
            return Phase.Phase1;
        } else if (phaseString.contains("phase3")) {
            return Phase.Phase3;
        } else if (phaseString.contains("phase4")) {
            return Phase.Phase4;
        } else if (phaseString.contains("phase5")) {
            return Phase.Phase5;
        } else if (phaseString.contains("phase6")) {
            return Phase.Phase6;
        } else if (phaseString.contains("github")) {
            return Phase.GitHub;
        }
        throw new IllegalArgumentException("Could not convert string to phase given '" + phaseString + "'");
    }

    public static Collection<Rubric.RubricType> getRubricTypesFromPhase(Phase phase) {
        return switch (phase) {
            case GitHub -> Set.of(Rubric.RubricType.GITHUB_REPO);
            case Phase0, Phase1 -> Set.of(Rubric.RubricType.GIT_COMMITS, Rubric.RubricType.PASSOFF_TESTS);
            case Phase3, Phase4 -> Set.of(Rubric.RubricType.GIT_COMMITS, Rubric.RubricType.PASSOFF_TESTS, Rubric.RubricType.QUALITY, Rubric.RubricType.UNIT_TESTS);
            case Phase5 -> Set.of(Rubric.RubricType.QUALITY, Rubric.RubricType.GIT_COMMITS, Rubric.RubricType.UNIT_TESTS);
            case Phase6 -> Set.of(Rubric.RubricType.GIT_COMMITS, Rubric.RubricType.PASSOFF_TESTS, Rubric.RubricType.QUALITY);
            default -> Collections.emptySet();
        };
    }

    public static ConfigurationDao.Configuration getConfigurationAssignmentNumber(Phase phase) {
        return switch (phase) {
            case GitHub -> ConfigurationDao.Configuration.GITHUB_ASSIGNMENT_NUMBER;
            case Phase0 -> ConfigurationDao.Configuration.PHASE0_ASSIGNMENT_NUMBER;
            case Phase1 -> ConfigurationDao.Configuration.PHASE1_ASSIGNMENT_NUMBER;
            case Phase3 -> ConfigurationDao.Configuration.PHASE3_ASSIGNMENT_NUMBER;
            case Phase4 -> ConfigurationDao.Configuration.PHASE4_ASSIGNMENT_NUMBER;
            case Phase5 -> ConfigurationDao.Configuration.PHASE5_ASSIGNMENT_NUMBER;
            case Phase6 -> ConfigurationDao.Configuration.PHASE6_ASSIGNMENT_NUMBER;
            default -> throw new IllegalArgumentException("No configuration assignment number for " + phase);
        };
    }

}
