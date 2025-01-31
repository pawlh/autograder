package edu.byu.cs.autograder.score;

import edu.byu.cs.autograder.GradingException;
import edu.byu.cs.canvas.CanvasException;
import edu.byu.cs.canvas.CanvasService;
import edu.byu.cs.dataAccess.ConfigurationDao;
import edu.byu.cs.dataAccess.DaoService;
import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.model.Phase;
import edu.byu.cs.properties.ApplicationProperties;
import edu.byu.cs.util.PhaseUtils;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.annotations.Nullable;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.logging.Logger;

/**
 * Calculates late days.
 * <br>
 * <b>Performance discussion:</b>
 * The calculation of the late/early days requires additional information which must be requested from a network.
 * This information is fetched and cached on a per student-phase basis so that subsequent requests requiring
 * the details do not create a performance bottleneck.
 * <br>
 * It is intended that an instance of this class will be shared and reused for at least an entire grading session.
 * Since the simple cache preserves all metadata, it would be efficient to restart the object for each new grading
 * session to avoid a memory leak.
 * <br>
 * An alternative design would prepare this object to be a long-lived object by implementing a max-time
 * limit on information in the cache. This alternative design is more complex than is necessary since
 * the object only lives for a single submission evaluation.
 */
public class LateDayCalculator {

    private static final Logger LOGGER = Logger.getLogger(LateDayCalculator.class.getName());

    private Set<LocalDate> publicHolidays;
    private final Map<String, LateDayContext> lateDayContextCache = new HashMap<>();

    public LateDayCalculator() {
        initializePublicHolidays(getEncodedPublicHolidays());
    }

    /**
     * Contains several pieces of information which must be queried from online sources.
     * This information is cached to respond quickly to repeated requests for the same information.
     *
     * @param dueDate
     * @param handInDate
     * @param maxLateDaysToPenalize
     */
    protected record LateDayContext(
            ZonedDateTime dueDate,
            ZonedDateTime handInDate,
            int maxLateDaysToPenalize
    ) { }

    /**
     * Retrieves and returns the {@link LateDayContext}.
     * <br>
     * If this results in a cache-miss, then multiple calls external to the process
     * will be made. The first lookup for each unique set of input parameters is slow.
     * <br>
     * After the context is retrieved, it is sufficient for all future analysis to
     * be computed without requiring any additional external information.
     *
     * @param phase A primary key to looking up the context.
     * @param netId Another primary key to looking up the context.
     * @return The {@link LateDayContext} associated with the input parameters.
     * @throws DataAccessException When the internal database experiences issues.
     * @throws GradingException When other business rules are violated during the preparation of information.
     */
    private LateDayContext getLateDayContext(Phase phase, String netId) throws DataAccessException, GradingException {
        // Read from local cache
        String keyHash = hashCacheKeys(phase, netId);
        if (lateDayContextCache.containsKey(keyHash)) {
            return lateDayContextCache.get(keyHash);
        }

        // Fetch, cache, and return
        LateDayContext lateDayContext = fetchLateDayContext(phase, netId);
        lateDayContextCache.put(keyHash, lateDayContext);
        return lateDayContext;
    }

    /**
     * Hashes together the primary keys to identify a result in the cache.
     *
     * @param phase A primary key to looking up the context.
     * @param netId Another primary key to looking up the context.
     * @return A string that can be used to uniquely identify the composite primary key.
     */
    private String hashCacheKeys(Phase phase, String netId) {
        return phase.name() + "---" + netId;
    }

    /** Called when experiencing a cache miss. Expected to run slowly. */
    private LateDayContext fetchLateDayContext(Phase phase, String netId) throws GradingException, DataAccessException {
        // Skip network calls when configured
        if (!ApplicationProperties.useCanvas()) {
            return new LateDayContext(null, null, 0);
        }

        // Request from network (expensive)
        return doFetchLateDayContext(phase, netId);
    }

    /** Intended to be overridden for testing or otherwise. Actually fetches the information and assembles the results. Slow response. */
    protected LateDayContext doFetchLateDayContext(Phase phase, String netId) throws GradingException, DataAccessException {
        ZonedDateTime dueDate = getPhaseDueDateZoned(phase, netId);
        ZonedDateTime handInDate = ScorerHelper.getHandInDateZoned(netId);
        int maxLateDaysToPenalize = DaoService.getConfigurationDao().getConfiguration(ConfigurationDao.Configuration.MAX_LATE_DAYS_TO_PENALIZE, Integer.class);
        return new LateDayContext(dueDate, handInDate, maxLateDaysToPenalize);
    }

    /** Intended to be overridden for testing or otherwise. Actually fetches the information and assembles the results. Slow response. */
    protected ZonedDateTime getPhaseDueDateZoned(Phase phase, String netId) throws GradingException, DataAccessException {
        int assignmentNum = PhaseUtils.getPhaseAssignmentNumber(phase);
        int canvasUserId = DaoService.getUserDao().getUser(netId).canvasUserId();

        try {
            return CanvasService.getCanvasIntegration().getAssignmentDueDateForStudent(canvasUserId, assignmentNum);
        } catch (CanvasException e) {
            throw new GradingException("Failed to get due date for assignment " + assignmentNum + " for user " + netId, e);
        }
    }

    /**
     * For the phase in the {@link edu.byu.cs.autograder.GradingContext}, calculates
     * the number of days late from the student-specific due date hand-in date.
     * <br>
     * This computation considers specific policies like:
     * <ul>
     *     <li>Configured holidays</li>
     *     <li>Maximum number of late days penalized</li>
     * </ul>
     * <br>
     * <b>Performance:</b> The first time this is run for a netId-phase combination will result in a cache-miss
     * which requires a slow lookup process from multiple external sources. After the context is available, the
     * computation is a <pre>O(n)</pre> iterative algorithm based on the number of days late.
     *
     * @param phase The current phase to consider.
     * @param netId The student netId being evaluated.
     * @return A non-negative integer indicating the number of days the assignment was submitted past the due date.
     * @throws GradingException When other business rules are violated during processing.
     * @throws DataAccessException When our internal database experiences issues.
     */
    public int calculateLateDays(Phase phase, String netId) throws GradingException, DataAccessException {
        var context = getLateDayContext(phase, netId);
        return Math.min(getNumDaysLate(context.handInDate, context.dueDate), context.maxLateDaysToPenalize);
    }

    /**
     * For the phase in the {@link edu.byu.cs.autograder.GradingContext}, calculates
     * the number of days early from the hand-in date to the student-specific due date.
     * <br>
     * This does not count holidays.
     * <br>
     * <b>Performance:</b> The first time this is run for a netId-phase combination will result in a cache-miss
     * which requires a slow lookup process from multiple external sources. After the context is available, the
     * computation is a <pre>O(1)</pre> constant time operation.
     *
     * @param phase The current phase to consider.
     * @param netId The student netId being evaluated.
     * @return A non-negative integer representing the number of days the assignment was submitted early.
     * @throws GradingException When other business rules are violated during processing.
     * @throws DataAccessException When our internal database experiences issues.
     */
    public int calculateEarlyDays(Phase phase, String netId) throws GradingException, DataAccessException {
        var context = getLateDayContext(phase, netId);
        return getNumDaysEarly(context.handInDate, context.dueDate);
    }

    /**
     * Gets the number of days late the submission is. This excludes weekends and public holidays.
     * <br>
     * In the event that the due date also happens to be a holiday, the assignment not receive
     * a late penalty until AFTER the holiday and weekends following it. While this behavior
     * may be surprising, it can be controlled by professors being careful to never assign a
     * due date on a holiday. This behavior is illustrated in the provided test cases.
     * <br>
     * <b>Performance:</b> <pre>O(n)</pre> where n is the number of days between the dates provided.
     * The constant factor is minimized by performing the holiday parsing and interpretation
     * during construction of the object.
     *
     * @param handInDate the date the submission was handed in
     * @param dueDate    the due date of the phase
     * @return the number of days late or 0 if the submission is not late
     */
    public int getNumDaysLate(@Nullable ZonedDateTime handInDate, @Nullable ZonedDateTime dueDate) {
        if (publicHolidays == null) {
            throw new RuntimeException("Public Holidays have not yet been initialized. "
                    + "Call `dateTimeUtils.initializePublicHolidays()` before attempting to count the days late.");
        }
        if (handInDate == null || dueDate == null) {
            return 0;
        }

        int daysLate = 0;

        while (handInDate.isAfter(dueDate)) {
            if (dueDate.getDayOfWeek().getValue() < 6 && !isPublicHoliday(dueDate)) {
                daysLate++;
            }
            dueDate = dueDate.plusDays(1);
        }

        return daysLate;
    }

    /**
     * Gets the number of full 24-hour periods that fit between the hand-in date and the due date.
     * <br>
     * This method does not respect holidays; any 24-hour period counts as an early day.
     * <b>Performance:</b> <pre>O(1)</pre> A constant operation no matter the interval between the dates.
     *
     * @param handInDate the date the submission was handed in
     * @param dueDate    the due date of the phase
     * @return the number of days early or 0 if the submission is not early
     */
    public int getNumDaysEarly(@Nullable ZonedDateTime handInDate, @Nullable ZonedDateTime dueDate) {
        if (handInDate == null || dueDate == null) return 0; // Missing values
        if (handInDate.isAfter(dueDate)) return 0; // Turned in late
        return (int) (Duration.between(handInDate, dueDate).toHours() / 24);
    }

    /**
     * Initializes the public holidays to an empty value.
     * Useful for testing when it's recognized that no holidays exist.
     */
    public void initializePublicHolidays() {
        publicHolidays = new HashSet<>();
    }
    /**
     * Initializes our public holidays with a common formatting string
     * that will accept strings matching this example: "9/16/2024"
     *
     * @see #initializePublicHolidays(String, String)
     *
     * @param encodedPublicHolidays A string representing the encoded data.
     */
    public Set<LocalDate> initializePublicHolidays(@NonNull String encodedPublicHolidays) {
        return initializePublicHolidays(encodedPublicHolidays, false);
    }
    /**
     * Initializes our public holidays with a common formatting string
     * that will accept strings matching this example: "2025-12-25"
     *
     * @see #initializePublicHolidays(String, String, boolean)
     *
     * @param encodedPublicHolidays A string representing the encoded data.
     */
    public Set<LocalDate> initializePublicHolidays(@NonNull String encodedPublicHolidays, boolean quietWarnings) {
        return initializePublicHolidays(encodedPublicHolidays, "yyyy-MM-dd", quietWarnings);
    }

    /**
     * Initializes our internal public holidays data set with the given string and a given date format.
     * This method also returns the constructed set of dates.
     * <br>
     * This must be called before calls to {@code getNumDaysLate()} will properly account for the holidays.
     *
     * @see #interpretPublicHolidays(String, String, boolean)
     *
     * @param encodedPublicHolidays @non-nullable A string representing the configured data
     * @param dateEncodingFormat A string representing the intended date format within the data
     */
    public Set<LocalDate> initializePublicHolidays(@NonNull String encodedPublicHolidays, @NonNull String dateEncodingFormat) {
        return initializePublicHolidays(encodedPublicHolidays, dateEncodingFormat, false);
    }
    /**
     * Initializes our internal public holidays data set with the given string and a given date format.
     * This method also returns the constructed set of dates.
     * <br>
     * This must be called before calls to {@code getNumDaysLate()} will properly account for the holidays.
     *
     * @see #interpretPublicHolidays(String, String, boolean)
     *
     * @param encodedPublicHolidays @non-nullable A string representing the configured data
     * @param dateEncodingFormat A string representing the intended date format within the data
     */
    public Set<LocalDate> initializePublicHolidays(@NonNull String encodedPublicHolidays, @NonNull String dateEncodingFormat, boolean quietWarnings) {
        if (encodedPublicHolidays == null || dateEncodingFormat == null) {
            throw new RuntimeException("Error initializing public holidays. Received null as a parameter. " +
                    "If some data isn't available, explicitly call the no argument initializer instead.");
        }

        publicHolidays = interpretPublicHolidays(encodedPublicHolidays, dateEncodingFormat, quietWarnings);
        assertFutureHolidaysConfigured();
        return publicHolidays;
    }

    /**
     * Runs after interpreting public holidays to provide some sanity checks that the process works.
     * <br>
     * Intentionally configured to throw {@link RuntimeException}'s which will break things at startup
     * time if proper configuration maintenance has not occurred.
     *
     * @throws RuntimeException When the configuration for the class is detected as improper.
     */
    protected void assertFutureHolidaysConfigured() throws RuntimeException {
        // Throw errors when not initialized
        if (publicHolidays == null) {
            throw new RuntimeException("Public holidays were not properly configured and appear as null. " +
                    "This is likely an issue with the coding and initialization of classes.");
        }

        // Do not throw errors when configured with an empty list. Possibly configured this way.
        if (publicHolidays.isEmpty()) return;

        // Throw errors when configured with dates, but none in the future.
        // This represents stale data that needs to be maintained.
        LocalDate maxDate = publicHolidays.stream().max(LocalDate::compareTo).get();
        LocalDate now = LocalDate.now();
        if (maxDate.isBefore(now)) {
            throw new RuntimeException("There are public holidays configured, but none of them are in the future. " +
                    "This likely represents a stale configuration error which results in holidays not being respected.");
        }
    }

    /**
     * <h1>Public Holiday Decoding</h1>
     * Interprets a string of encoded public holidays and returns a set of objects that
     * can be used to efficiently query against the dataset.
     * <br>
     *
     * <h2>Encoding Approaches</h2>
     * The input should be only one of the two approaches. It should either be a single-line entry with
     * zero or more dates, or it should be a multi-line string which can include comments following the date string.
     * <br>
     *
     * <h3>Single Line Encoding</h3>
     * Dates can be stored on a single line, separated by any combination of the following:
     * <ul>
     *  <li>spaces " ", or</li>
     *  <li>commas ",", or </li>
     *  <li>semicolon ";" </li>
     * </ul>
     * <br>
     *
     * Each word will be interpreted individually, and any that do not parse into a date properly
     * will be excluded individually.
     * <br>
     *
     * <h3>Multi-line Encoding</h3>
     * Additionally, the dates can each appear on a separate line.
     * Only the first word of the line will be interpreted as a date,
     * and the rest of the line will be ignored as a comment.
     * Any line that does not contain a date format at the beginning
     * (i.e. an empty line) will be ignored and not considered for interpretation.
     * Lines beginning with {@code #} are always ignored as a comment; however, a comment
     * need not begin with a special symbol. Lines where a date cannot be interpreted
     * from the first word will simply be skipped.
     * <br>
     *
     * <h2>Encoding Date Formats</h2>
     * Since both approaches use whitespace to delimit words, no acceptable date format can ever contain
     * a whitespace character. This is the only restraint for the multi-line approach; however,
     * however, in the single line approach, the format cannot contain any of the delimiting characters.
     * <br>
     *
     * @param encodedPublicHolidays A string representing the public holidays.
     *                              This could be read in from a file or from a cell in a table
     *                              depending on the most convenient form of maintaining this information.
     * @param dateEncodingFormat A string representing the date format.
     *                           This will be interpreted by {@link DateTimeFormatter}.
     * @param quietWarnings Option to quiet warnings instead of sending them to STDERR.
     * @return A {@code Set<Date>} that will be used to efficiently comparing against this dataset
     */
    private Set<LocalDate> interpretPublicHolidays(@Nullable String encodedPublicHolidays, @NonNull String dateEncodingFormat, boolean quietWarnings) {
        String[] dateStrings;
        if (encodedPublicHolidays == null) {
            dateStrings = new String[]{};
        } else if (encodedPublicHolidays.contains("\n")) {
            dateStrings = extractPublicHolidaysMultiline(encodedPublicHolidays);
        } else {
            dateStrings = extractPublicHolidaysSingleline(encodedPublicHolidays);
        }

        return parsePublicHolidayStrings(dateEncodingFormat, dateStrings, quietWarnings);
    }
    private String[] extractPublicHolidaysSingleline(String singleLineEncodedHolidays) {
        String DELIMITERS = " ,;";
        return singleLineEncodedHolidays.split("["+DELIMITERS+"]");
    }
    private String[] extractPublicHolidaysMultiline(String multilineEncodedHolidays) {
        ArrayList<String> holidays = new ArrayList<>();
        Scanner scanner = new Scanner(multilineEncodedHolidays);
        while (scanner.hasNext()) {
            holidays.add(scanner.next()); // The first word in interpreted as a date
            scanner.nextLine(); // Skip the rest of the line
        }
        return holidays.toArray(new String[0]);
    }
    private Set<LocalDate> parsePublicHolidayStrings(@NonNull String dateFormat, String[] holidayDateStrings, boolean quietWarnings) {
        Set<LocalDate> publicHolidays = new HashSet<>();
        var parser = DateTimeFormatter.ofPattern(dateFormat);
        for (var holidayDateString : holidayDateStrings) {
            if (holidayDateString.isEmpty()) {
                continue; // Empty line
            }
            if (holidayDateString.charAt(0) == '#') {
                continue; // Explicitly marked as a comment
            }
            try {
                publicHolidays.add(parser.parse(holidayDateString, LocalDate::from));
            } catch (DateTimeParseException e) {
                if (!quietWarnings) LOGGER.warning("Skipping unrecognized date string: " + holidayDateString);
            }
        }
        return publicHolidays;
    }

    /**
     * Checks if the given date is a public holiday
     *
     * @param zonedDateTime the date to check
     * @return true if the date is a public holiday, false otherwise
     */
    private boolean isPublicHoliday(@NonNull ZonedDateTime zonedDateTime) {
        if (publicHolidays == null) {
//            return false; // Holidays have not been initialized
            throw new RuntimeException("Holiday settings have not been initialized properly");
        }

        LocalDate date = LocalDate.of(zonedDateTime.getYear(), zonedDateTime.getMonthValue(), zonedDateTime.getDayOfMonth());
        return publicHolidays.contains(date);
    }

    private String getEncodedPublicHolidays() {
        String encodedHolidays;
        try {
            encodedHolidays = DaoService.getConfigurationDao().getConfiguration(ConfigurationDao.Configuration.HOLIDAY_LIST, String.class);
        } catch (DataAccessException e) {
            throw new RuntimeException("Error getting encoded holiday list from configuration", e);
        }

        return encodedHolidays;
    }
}
