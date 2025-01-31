package edu.byu.cs.service;

import edu.byu.cs.canvas.CanvasException;
import edu.byu.cs.canvas.CanvasIntegrationImpl;
import edu.byu.cs.canvas.model.CanvasAssignment;
import edu.byu.cs.dataAccess.ConfigurationDao;
import edu.byu.cs.dataAccess.ConfigurationDao.Configuration;
import edu.byu.cs.dataAccess.DaoService;
import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.model.*;
import edu.byu.cs.model.request.ConfigPenaltyUpdateRequest;
import edu.byu.cs.util.PhaseUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

import static edu.byu.cs.util.PhaseUtils.isPhaseEnabled;
import static edu.byu.cs.util.PhaseUtils.isPhaseGraded;

public class ConfigService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigService.class);
    private static final ConfigurationDao dao = DaoService.getConfigurationDao();

    public static PublicConfig getPublicConfig() throws DataAccessException {
        String phasesString = dao.getConfiguration(Configuration.STUDENT_SUBMISSIONS_ENABLED, String.class);

        List<Phase> phases;
        if (phasesString != null && !phasesString.isEmpty() && !phasesString.equals("[]")) {
            String cleanString = phasesString
                    .replace("[", "")
                    .replace("]", "");

            if (!cleanString.isEmpty()) {
                phases = Arrays.stream(cleanString.split(","))
                        .map(String::trim)
                        .map(Phase::valueOf)
                        .toList();
            } else {
                phases = new ArrayList<>();
            }
        } else {
            phases = new ArrayList<>();
        }

        return new PublicConfig(
                generateBannerConfig(),
                generateShutdownConfig(),
                phases
        );
    }

    public static PrivateConfig getPrivateConfig() throws DataAccessException {
        return new PrivateConfig(
                generatePenaltyConfig(),
                dao.getConfiguration(Configuration.COURSE_NUMBER, Integer.class),
                generateAssignmentsConfig(),
                generateHolidayConfig()
        );
    }

    //
    // PUBLIC CONFIG GENERATORS
    //
    private static PublicConfig.BannerConfig generateBannerConfig() throws DataAccessException {
        Instant bannerExpiration = dao.getConfiguration(ConfigurationDao.Configuration.BANNER_EXPIRATION, Instant.class);
        if (bannerExpiration.isBefore(Instant.now())) { //Banner has expired
            clearBannerConfig();
        }

        return new PublicConfig.BannerConfig(
                dao.getConfiguration(Configuration.BANNER_MESSAGE, String.class),
                dao.getConfiguration(Configuration.BANNER_LINK, String.class),
                dao.getConfiguration(Configuration.BANNER_COLOR, String.class),
                translateInstantForFrontEnd(bannerExpiration)
        );
    }

    private static PublicConfig.ShutdownConfig generateShutdownConfig() throws DataAccessException {
        checkForShutdown();

        return new PublicConfig.ShutdownConfig(
                translateInstantForFrontEnd(dao.getConfiguration(Configuration.GRADER_SHUTDOWN_DATE, Instant.class)),
                dao.getConfiguration(Configuration.GRADER_SHUTDOWN_WARNING_MILLISECONDS, Integer.class)
        );
    }

    //
    // PRIVATE CONFIG GENERATORS
    //
    private static PrivateConfig.PenaltyConfig generatePenaltyConfig() throws DataAccessException {
        return new PrivateConfig.PenaltyConfig(
                dao.getConfiguration(Configuration.PER_DAY_LATE_PENALTY, Float.class),
                dao.getConfiguration(Configuration.GIT_COMMIT_PENALTY, Float.class),
                dao.getConfiguration(Configuration.MAX_LATE_DAYS_TO_PENALIZE, Integer.class),
                dao.getConfiguration(Configuration.LINES_PER_COMMIT_REQUIRED, Integer.class),
                dao.getConfiguration(Configuration.CLOCK_FORGIVENESS_MINUTES, Integer.class)
        );
    }

    public static ArrayList<PrivateConfig.AssignmentConfig> generateAssignmentsConfig() throws DataAccessException {
        ArrayList<PrivateConfig.AssignmentConfig> assignments = new ArrayList<>();
        for (Phase phase : Phase.values()) {
            if (!isPhaseGraded(phase)) continue;

            int assignmentId = PhaseUtils.getPhaseAssignmentNumber(phase);
            EnumMap<Rubric.RubricType, RubricConfig.RubricConfigItem> rubricConfigItems = DaoService.getRubricConfigDao().getRubricConfig(phase).items();

            assignments.add(new PrivateConfig.AssignmentConfig(phase, assignmentId, rubricConfigItems));
        }
        return assignments;
    }

    public static String[] generateHolidayConfig() throws DataAccessException {
        String encodedDates = dao.getConfiguration(Configuration.HOLIDAY_LIST, String.class);

        if (Objects.equals(encodedDates, "")) {
            return new String[0];
        }
        return encodedDates.split(";");
    }

    //
    // CONFIG SETTERS
    //
    private static void clearBannerConfig() throws DataAccessException {
        dao.setConfiguration(Configuration.BANNER_MESSAGE, "", String.class);
        dao.setConfiguration(Configuration.BANNER_LINK, "", String.class);
        dao.setConfiguration(Configuration.BANNER_COLOR, "", String.class);
        dao.setConfiguration(Configuration.BANNER_EXPIRATION, Instant.MAX, Instant.class);

        logAutomaticConfigChange("Banner message has expired");
    }

    public static void updateLivePhases(ArrayList phasesArray, User user) throws DataAccessException {
        dao.setConfiguration(Configuration.STUDENT_SUBMISSIONS_ENABLED, phasesArray, ArrayList.class);

        logConfigChange("set the following phases as live: %s".formatted(phasesArray), user.netId());
    }

    public static void setShutdownWarningDuration(User user, Integer warningMilliseconds) throws DataAccessException {
        if (warningMilliseconds < 0) {
            throw new IllegalArgumentException("warningMilliseconds must be non-negative");
        }

        dao.setConfiguration(ConfigurationDao.Configuration.GRADER_SHUTDOWN_WARNING_MILLISECONDS, warningMilliseconds, Integer.class);

        logConfigChange("set the shutdown warning duration to %s milliseconds".formatted(warningMilliseconds), user.netId());
    }

    public static void scheduleShutdown(User user, String shutdownTimestampString) throws DataAccessException {
        if (shutdownTimestampString.isEmpty()) {
            clearShutdownSchedule(user);
            return;
        }

        Instant shutdownTimestamp;
        try {
            shutdownTimestamp = getInstantFromUnzonedTime(shutdownTimestampString);
        } catch (Exception e) {
            throw new IllegalArgumentException("Incomplete timestamp. Send a full timestamp {YYYY-MM-DDTHH:MM:SS} or send none at all", e);
        }

        if (shutdownTimestamp.isBefore(Instant.now())) {
            throw new IllegalArgumentException("You tried to schedule the shutdown in the past");
        }

        dao.setConfiguration(ConfigurationDao.Configuration.GRADER_SHUTDOWN_DATE, shutdownTimestamp, Instant.class);
        logConfigChange("scheduled a grader shutdown for %s".formatted(shutdownTimestampString), user.netId());
    }

    public static void triggerShutdown() {
        try {
            ArrayList<Phase> phases = new ArrayList<>();
            for (Phase phase: Phase.values()) {
                if (!isPhaseGraded(phase) && isPhaseEnabled(phase)) {
                    phases.add(phase);
                }
            }
            dao.setConfiguration(ConfigurationDao.Configuration.STUDENT_SUBMISSIONS_ENABLED, phases, ArrayList.class);
            logAutomaticConfigChange("Student submissions have shutdown. These phases remain active: " + phases);
        } catch (DataAccessException e) {
            LOGGER.error("Something went wrong while shutting down graded phases: " + e.getMessage());
        }
    }

    public static void clearShutdownSchedule() throws DataAccessException {
        clearShutdownSchedule(null);
    }

    public static void clearShutdownSchedule(User user) throws DataAccessException {
        dao.setConfiguration(ConfigurationDao.Configuration.GRADER_SHUTDOWN_DATE, Instant.MAX, Instant.class);
        if (user == null) {
            logAutomaticConfigChange("Grader Shutdown Schedule was cleared");
        } else {
            logConfigChange("cleared the Grader Shutdown Schedule", user.netId() );
        }
    }

    public static void updateBannerMessage(User user, String expirationString, String message, String link, String color) throws DataAccessException {
        Instant expirationTimestamp = Instant.MAX;
        if (!expirationString.isEmpty()) {
            try {
                expirationTimestamp = getInstantFromUnzonedTime(expirationString);
            } catch (Exception e) {
                throw new IllegalArgumentException("Incomplete timestamp. Send a full timestamp {YYYY-MM-DDTHH:MM:SS} or send none at all", e);
            }
        }

        if (expirationTimestamp.isBefore(Instant.now())) {
            throw new IllegalArgumentException("You tried to set the banner to expire in the past");
        }

        // If they give us a color, and it's not long enough or is missing #
        if (!color.isEmpty() && ((color.length() != 7) || !color.startsWith("#"))) {
            throw new IllegalArgumentException("Invalid hex color code. Must provide a hex code starting with a # symbol, followed by 6 hex digits");
        }

        dao.setConfiguration(Configuration.BANNER_MESSAGE, message, String.class);
        dao.setConfiguration(Configuration.BANNER_LINK, link, String.class);
        dao.setConfiguration(Configuration.BANNER_COLOR, color, String.class);
        dao.setConfiguration(Configuration.BANNER_EXPIRATION, expirationTimestamp, Instant.class);

        if (message.isEmpty()) {
            logConfigChange("cleared the banner message", user.netId());
        } else {
            expirationString = !expirationString.isEmpty() ? expirationString : "never";
            logConfigChange("set the banner message to: '%s' with link: {%s} to expire at %s".formatted(message, link, expirationString), user.netId());
        }

    }

    public static void setCourseId(User user, Integer newCourseId) throws DataAccessException, CanvasException {
        Integer oldCourseJustInCase = dao.getConfiguration(Configuration.COURSE_NUMBER, Integer.class);
        dao.setConfiguration(Configuration.COURSE_NUMBER, newCourseId, Integer.class);
        logConfigChange("changed the Canvas course ID to %d".formatted(newCourseId), user.netId());

        try {
            // if this line fails, we need to revert back to the previous course id
            updateCourseIdsUsingCanvas(user);
        } catch (CanvasException e) {
            dao.setConfiguration(Configuration.COURSE_NUMBER, oldCourseJustInCase, Integer.class);
            logAutomaticConfigChange("While fetching new course assignment info for course %d, an error occurred. Reverting course id to %d".formatted(newCourseId, oldCourseJustInCase));
            throw e;
        }
    }

    public static void updateCourseIdsUsingCanvas(User user) throws CanvasException, DataAccessException {
        var retriever = new CanvasIntegrationImpl.CourseInfoRetriever();
        retriever.useCourseRelatedInfoFromCanvas();

        logConfigChange(
                "updated course info (assignment IDs, rubric IDs, rubric points) " +
                        "in the database using Canvas",
                user.netId()
        );
    }

    public static void processPenaltyUpdates(User user, ConfigPenaltyUpdateRequest request) throws DataAccessException {
        validateValidPercentFloat(request.gitCommitPenalty(), "Git Commit Penalty");
        validateValidPercentFloat(request.perDayLatePenalty(), "Per Day Late Penalty");
        validateNonNegativeInt(request.clockForgivenessMinutes(), "Clock Forgiveness Minutes");
        validateNonNegativeInt(request.maxLateDaysPenalized(), "Max Late Days Penalized");
        validateNonNegativeInt(request.linesChangedPerCommit(), "Lines Changed Per Commit");

        setConfigItem(user, Configuration.GIT_COMMIT_PENALTY, request.gitCommitPenalty(), Float.class);
        setConfigItem(user, Configuration.PER_DAY_LATE_PENALTY, request.perDayLatePenalty(), Float.class);
        setConfigItem(user, Configuration.CLOCK_FORGIVENESS_MINUTES, request.clockForgivenessMinutes(), Integer.class);
        setConfigItem(user, Configuration.MAX_LATE_DAYS_TO_PENALIZE, request.maxLateDaysPenalized(), Integer.class);
        setConfigItem(user, Configuration.LINES_PER_COMMIT_REQUIRED, request.linesChangedPerCommit(), Integer.class);
    }

    public static void updateHolidays(User user, List<LocalDate> holidays) throws DataAccessException {
        StringBuilder stringBuilder = new StringBuilder();

        holidays.sort(LocalDate::compareTo);

        for (LocalDate holiday : holidays) {
            stringBuilder.append(holiday).append(";");
        }

        String encodedHolidays = stringBuilder.toString();

        setConfigItem(user, Configuration.HOLIDAY_LIST, encodedHolidays, String.class);
    }

    //
    // GENERAL HELPER FUNCTIONS
    //
    private static String translateInstantForFrontEnd(Instant timestamp) {
        String translatedTime;
        if (timestamp.equals(Instant.MAX)) {
            translatedTime = "never";
        } else {
            translatedTime = timestamp.toString();
        }
        return translatedTime;
    }
    /**
     * Checks that the date for a scheduled shutdown has passed. If it has, it triggers the shutdown method
     */
    public static void checkForShutdown() {
        try {
            Instant shutdownInstant = dao.getConfiguration(ConfigurationDao.Configuration.GRADER_SHUTDOWN_DATE, Instant.class);
            if (shutdownInstant.isBefore(Instant.now())) {
                triggerShutdown();
            }
        } catch (DataAccessException e) {
            LOGGER.error("Something went wrong while checking for shutdown: " + e.getMessage());
        }
    }
    private static Instant getInstantFromUnzonedTime(String timestampString) throws DateTimeParseException {
        ZoneId utahZone = ZoneId.of("America/Denver");

        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        LocalDateTime localDateTime = LocalDateTime.parse(timestampString, formatter);
        ZonedDateTime utahTime = localDateTime.atZone(utahZone);

        return utahTime.toInstant();
    }
    /**
     * throws IllegalArgumentException if percent is not valid
     * @param percent a float that should be between 0 and 1 (inclusive)
     * @param name the name of the value, only used when throwing the exception
     */
    private static void validateValidPercentFloat(float percent, String name) {
        if ((percent < 0) || (percent > 1)) {
            throw new IllegalArgumentException(name + " must be 0-1");
        }
    }

    /**
     * throws IllegalArgumentException if value is negative
     * @param value number we're checking is >= 0
     * @param name name of the value, used when throwing the exception
     */
    private static void validateNonNegativeInt(int value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must be non-negative");
        }
    }

    private static <T> void setConfigItem(User admin, Configuration configKey, T value, Class<T> type) throws DataAccessException {
        ConfigurationDao dao = DaoService.getConfigurationDao();

        T current = dao.getConfiguration(configKey, type);
        if (current.equals(value)) return;

        dao.setConfiguration(configKey, value, type);
        logConfigChange("changed %s to [%s]".formatted(configKey.name(), value.toString()), admin.netId());
    }


    //
    // LOGGING
    //
    private static void logConfigChange(String changeMessage, String adminNetId) {
        LOGGER.info("[CONFIG] Admin {} has {}", adminNetId, changeMessage);
    }

    private static void logAutomaticConfigChange(String changeMessage) {
        LOGGER.info("[CONFIG] Automatic change: {}", changeMessage);
    }
}
