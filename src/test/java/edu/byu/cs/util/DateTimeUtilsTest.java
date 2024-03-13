package edu.byu.cs.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DateTimeUtilsTest {

    @Test
    void getNumDaysLateWithoutHolidays() {
        // Initialize without holidays
        DateTimeUtils dateTimeUtils = new DateTimeUtils();
        dateTimeUtils.initializePublicHolidays();

        // See images: days-late-without-holidays (1&2)
        String dueDateStr = "2024-03-07 11:59:00 PM -07:00";
        ExpectedDaysLate[] expectedDaysLate = {
                // On time submissions
                // Notice the timezone testing and edge case testing
                new ExpectedDaysLate("2024-02-03 02:00:00 PM -07:00", 0),
                new ExpectedDaysLate("2024-03-03 02:00:00 PM -17:00", 0),
                new ExpectedDaysLate("2024-03-06 11:59:00 AM -07:00", 0),
                new ExpectedDaysLate("2024-03-07 03:00:00 PM -07:00", 0),
                new ExpectedDaysLate("2024-03-08 01:12:00 AM -05:00", 0),

                // Edge case, late submissions
                new ExpectedDaysLate("2024-03-07 11:59:13 PM -07:00", 1),
                new ExpectedDaysLate("2024-03-07 11:59:45 PM -07:00", 1),

                // Late submissions (1 day to 1 week)
                new ExpectedDaysLate("2024-03-08 12:15:00 AM -07:00", 1),
                new ExpectedDaysLate("2024-03-08 08:15:00 AM -07:00", 1),
                new ExpectedDaysLate("2024-03-08 02:15:00 PM -07:00", 1),
                new ExpectedDaysLate("2024-03-08 11:58:45 PM -07:00", 1),

                new ExpectedDaysLate("2024-03-08 11:59:03 PM -07:00", 2),
                new ExpectedDaysLate("2024-03-09 02:15:00 PM -07:00", 2),
                new ExpectedDaysLate("2024-03-10 02:15:00 PM -07:00", 2),
                new ExpectedDaysLate("2024-03-11 02:15:00 PM -07:00", 2),

                new ExpectedDaysLate("2024-03-12 02:15:00 PM -07:00", 3),
                new ExpectedDaysLate("2024-03-13 02:15:00 PM -07:00", 4),
                new ExpectedDaysLate("2024-03-14 02:15:00 PM -07:00", 5),

                // Late submissions (1 week to 3 weeks)
                new ExpectedDaysLate("2024-03-15 02:15:00 PM -07:00", 6),
                new ExpectedDaysLate("2024-03-16 02:15:00 PM -07:00", 7),
                new ExpectedDaysLate("2024-03-17 02:15:00 PM -07:00", 7),
                new ExpectedDaysLate("2024-03-18 02:15:00 PM -07:00", 7),
                new ExpectedDaysLate("2024-03-19 02:15:00 PM -07:00", 8),
                new ExpectedDaysLate("2024-03-20 02:15:00 PM -07:00", 9),
                new ExpectedDaysLate("2024-03-21 02:15:00 PM -07:00", 10),
                new ExpectedDaysLate("2024-03-22 02:15:00 PM -07:00", 11),
                new ExpectedDaysLate("2024-03-23 02:15:00 PM -07:00", 12),
                new ExpectedDaysLate("2024-03-24 02:15:00 PM -07:00", 12),
                new ExpectedDaysLate("2024-03-25 02:15:00 PM -07:00", 12),

                // Late submissions (1+ months)
                new ExpectedDaysLate("2024-04-15 02:15:00 PM -07:00", 27),
        };

        // Validate
        validateExpectedDaysLate(dueDateStr, expectedDaysLate, dateTimeUtils);
    }
    @Test
    void getNumDaysLateWithHolidays() {
        // Initialize with holidays
        DateTimeUtils standardDateTimeUtils = new DateTimeUtils();
        standardDateTimeUtils.initializePublicHolidays(getMultilinePublicHolidaysConfiguration());

        // See image: days-late-with-holidays-common
        String commonDueDate = "2024-03-07 11:59:00 PM -07:00";
        ExpectedDaysLate[] commonExpectedDaysLate = {
                // Early & on-time
                new ExpectedDaysLate("2024-03-03 02:15:00 PM -07:00", 0),
                new ExpectedDaysLate("2024-03-04 02:15:00 PM -07:00", 0),
                new ExpectedDaysLate("2024-03-05 02:15:00 PM -07:00", 0),
                new ExpectedDaysLate("2024-03-06 02:15:00 PM -07:00", 0),
                new ExpectedDaysLate("2024-03-07 02:15:00 PM -07:00", 0), // Due date

                // Late
                new ExpectedDaysLate("2024-03-08 02:15:00 PM -07:00", 1),
                new ExpectedDaysLate("2024-03-09 02:15:00 PM -07:00", 2),
                new ExpectedDaysLate("2024-03-10 02:15:00 PM -07:00", 2),
                new ExpectedDaysLate("2024-03-11 02:15:00 PM -07:00", 2),
                new ExpectedDaysLate("2024-03-12 02:15:00 PM -07:00", 3),
                new ExpectedDaysLate("2024-03-13 02:15:00 PM -07:00", 4),
                new ExpectedDaysLate("2024-03-14 02:15:00 PM -07:00", 5),
                new ExpectedDaysLate("2024-03-15 02:15:00 PM -07:00", 6), // Holiday
                new ExpectedDaysLate("2024-03-16 02:15:00 PM -07:00", 6),
                new ExpectedDaysLate("2024-03-17 02:15:00 PM -07:00", 6),
                new ExpectedDaysLate("2024-03-18 02:15:00 PM -07:00", 6),
                new ExpectedDaysLate("2024-03-19 02:15:00 PM -07:00", 7),
                new ExpectedDaysLate("2024-03-20 02:15:00 PM -07:00", 8),
                new ExpectedDaysLate("2024-03-21 02:15:00 PM -07:00", 9),
                new ExpectedDaysLate("2024-03-22 02:15:00 PM -07:00", 10),
                new ExpectedDaysLate("2024-03-23 02:15:00 PM -07:00", 11),
                new ExpectedDaysLate("2024-03-24 02:15:00 PM -07:00", 11),
                new ExpectedDaysLate("2024-03-25 02:15:00 PM -07:00", 11),
        };
        validateExpectedDaysLate(commonDueDate, commonExpectedDaysLate, standardDateTimeUtils);

        // See image: days-late-with-holidays-due-on-holiday
        // This edge case is professor approved
        String holidayDueDate = "2024-06-19 11:59:00 PM -07:00";
        ExpectedDaysLate[] holidayExpectedDaysLate = {
                // Early & on-time
                new ExpectedDaysLate("2024-06-16 02:15:00 PM -07:00", 0),
                new ExpectedDaysLate("2024-06-17 02:15:00 PM -07:00", 0),
                new ExpectedDaysLate("2024-06-18 02:15:00 PM -07:00", 0),
                new ExpectedDaysLate("2024-06-19 02:15:00 PM -07:00", 0), // Due date, holiday

                // Edge Case
                // If the due date is a holiday,
                // then the assignment will be counted on time the following day
                new ExpectedDaysLate("2024-06-20 02:15:00 PM -07:00", 0),

                // Late
                new ExpectedDaysLate("2024-06-21 02:15:00 PM -07:00", 1),
                new ExpectedDaysLate("2024-06-22 02:15:00 PM -07:00", 2), // Weekend
                new ExpectedDaysLate("2024-06-23 02:15:00 PM -07:00", 2), // Weekend
                new ExpectedDaysLate("2024-06-24 02:15:00 PM -07:00", 2),
                new ExpectedDaysLate("2024-06-25 02:15:00 PM -07:00", 3),
                new ExpectedDaysLate("2024-06-26 02:15:00 PM -07:00", 4),
                new ExpectedDaysLate("2024-06-27 02:15:00 PM -07:00", 5),
                new ExpectedDaysLate("2024-06-28 02:15:00 PM -07:00", 6),
                new ExpectedDaysLate("2024-06-29 02:15:00 PM -07:00", 7), // Weekend
                new ExpectedDaysLate("2024-06-30 02:15:00 PM -07:00", 7), // Weekend
                new ExpectedDaysLate("2024-07-01 02:15:00 PM -07:00", 7),
                new ExpectedDaysLate("2024-07-02 02:15:00 PM -07:00", 8),
                new ExpectedDaysLate("2024-07-03 02:15:00 PM -07:00", 9),
                new ExpectedDaysLate("2024-07-04 02:15:00 PM -07:00", 10), // Holiday
                new ExpectedDaysLate("2024-07-05 02:15:00 PM -07:00", 10),
                new ExpectedDaysLate("2024-07-06 02:15:00 PM -07:00", 11), // Weekend
        };
        validateExpectedDaysLate(holidayDueDate, holidayExpectedDaysLate, standardDateTimeUtils);


        // See image: days-late-with-holidays-friday-holiday-and-consecutive-holidays
        DateTimeUtils customDateTimeUtils = new DateTimeUtils();
        customDateTimeUtils.initializePublicHolidays("12/20/2024;12/24/2024;12/25/2024;12/31/2024;1/1/2025");
        String fridayHolidayDueDate = "2024-12-20 11:59:00 PM -07:00";
        ExpectedDaysLate[] fridayHolidayAndConsecutiveHolidays = {
                // On Time
                new ExpectedDaysLate("2024-12-15 02:15:00 PM -07:00", 0),
                new ExpectedDaysLate("2024-12-16 02:15:00 PM -07:00", 0),
                new ExpectedDaysLate("2024-12-17 02:15:00 PM -07:00", 0),
                new ExpectedDaysLate("2024-12-18 02:15:00 PM -07:00", 0),
                new ExpectedDaysLate("2024-12-19 02:15:00 PM -07:00", 0),
                new ExpectedDaysLate("2024-12-20 02:15:00 PM -07:00", 0), // Due date, holiday

                // Approved edge case (same as above)
                new ExpectedDaysLate("2024-12-21 02:15:00 PM -07:00", 0), // Weekend
                new ExpectedDaysLate("2024-12-22 02:15:00 PM -07:00", 0), // Weekend
                new ExpectedDaysLate("2024-12-23 02:15:00 PM -07:00", 0),

                // Late
                new ExpectedDaysLate("2024-12-24 02:15:00 PM -07:00", 1), // Holiday
                new ExpectedDaysLate("2024-12-25 02:15:00 PM -07:00", 1), // Holiday
                new ExpectedDaysLate("2024-12-26 02:15:00 PM -07:00", 1),
                new ExpectedDaysLate("2024-12-27 02:15:00 PM -07:00", 2),
                new ExpectedDaysLate("2024-12-28 02:15:00 PM -07:00", 3), // Weekend
                new ExpectedDaysLate("2024-12-29 02:15:00 PM -07:00", 3), // Weekend
                new ExpectedDaysLate("2024-12-30 02:15:00 PM -07:00", 3),
                new ExpectedDaysLate("2024-12-31 02:15:00 PM -07:00", 4), // Holiday
                new ExpectedDaysLate("2025-01-01 02:15:00 PM -07:00", 4), // Holiday
                new ExpectedDaysLate("2025-01-02 02:15:00 PM -07:00", 4),
                new ExpectedDaysLate("2025-01-03 02:15:00 PM -07:00", 5),
                new ExpectedDaysLate("2025-01-04 02:15:00 PM -07:00", 6), // Weekend
                new ExpectedDaysLate("2025-01-05 02:15:00 PM -07:00", 6), // Weekend
                new ExpectedDaysLate("2025-01-06 02:15:00 PM -07:00", 6),
                new ExpectedDaysLate("2025-01-07 02:15:00 PM -07:00", 7),
                new ExpectedDaysLate("2025-01-08 02:15:00 PM -07:00", 8),
                new ExpectedDaysLate("2025-01-09 02:15:00 PM -07:00", 9),
                new ExpectedDaysLate("2025-01-10 02:15:00 PM -07:00", 10),
                new ExpectedDaysLate("2025-01-11 02:15:00 PM -07:00", 11), // Weekend
        };
        validateExpectedDaysLate(fridayHolidayDueDate, fridayHolidayAndConsecutiveHolidays, customDateTimeUtils);


        // See image: days-late-with-holidays-holidays-on-weekends
        DateTimeUtils customDateTimeUtils2 = new DateTimeUtils();
        customDateTimeUtils2.initializePublicHolidays("09/16/2028;09/17/2028;09/18/2028;");
        String holidaysOnWeekendsDueDate = "2028-09-14 02:15:00 PM -07:00";
        ExpectedDaysLate[] holidaysOnWeekends = {
                new ExpectedDaysLate("2028-09-14 02:15:00 PM -07:00", 0), // Due date
                new ExpectedDaysLate("2028-09-15 02:15:00 PM -07:00", 1),
                new ExpectedDaysLate("2028-09-16 02:15:00 PM -07:00", 2), // Weekend, holiday
                new ExpectedDaysLate("2028-09-17 02:15:00 PM -07:00", 2), // Weekend, holiday
                new ExpectedDaysLate("2028-09-18 02:15:00 PM -07:00", 2), // Holiday
                new ExpectedDaysLate("2028-09-19 02:15:00 PM -07:00", 2),
                new ExpectedDaysLate("2028-09-20 02:15:00 PM -07:00", 3),
                new ExpectedDaysLate("2028-09-21 02:15:00 PM -07:00", 4),
        };
        validateExpectedDaysLate(holidaysOnWeekendsDueDate, holidaysOnWeekends, customDateTimeUtils2);
    }
    private void validateExpectedDaysLate(String dueDateStr, ExpectedDaysLate[] expectedDaysLate, DateTimeUtils dateTimeUtils) {

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss a z");
        ZonedDateTime dueDate = ZonedDateTime.parse(dueDateStr, formatter);

        // Evaluate all the test cases above
        ZonedDateTime handInTime;
        for (var expectedResult : expectedDaysLate) {
            handInTime = ZonedDateTime.parse(expectedResult.handInDate, formatter);
            Assertions.assertEquals(expectedResult.daysLate, dateTimeUtils.getNumDaysLate(handInTime, dueDate));
        }
    }

    @Test
    void initializePublicHolidaysSingleLine() {
        String encodedPublicHolidays = getSinglelinePublicHolidaysConfiguration();
        validateExpectedHolidays(encodedPublicHolidays);
    }
    private String getSinglelinePublicHolidaysConfiguration() {
        return " " // Leading whitespace
                + "1/1/2024;1/15/2024;2/19/2024;3/15/2024;4/25/2024;5/27/2024;6/19/2024;" // Delimited with ";"
                + "This isn't a date, you silly goose! " // Invalid date should be skipped
                + "7/4/2024,7/24/2024,9/2/2024,11/27/2024,11/28/2024,11/29/2024," // Delimited with ","
                + "16 sep 2024," // This date shouldn't be accepted since it's in the wrong format
                + ";, " // Multiple consecutive delimiters
                + "12/24/2024 12/25/2024 12/31/2024 " // Delimited with " "
                + "1/1/2025;"; // Has trailing delimiter
    }
    @Test
    void initializePublicHolidaysMultiLine() {
        String encodedPublicHolidays = getMultilinePublicHolidaysConfiguration();
        validateExpectedHolidays(encodedPublicHolidays);
    }
    private String getMultilinePublicHolidaysConfiguration() {
        return
                """
                1/1/2024 New Years
                1/15/2024 MLK Jr's day
                2/19/2024 President's Day
                3/15/2024 Spring day
                        4/25/2024 Commencement          Extra indentation shouldn't mess anything up
                5/27/2024 Memorial day
                6/19/2024 Juneteenth
                
                This is just a comment.
                The following date should not be accepted: 7/1/2024
                7/4/2024 July 4th
                7/24/2024 Pioneer day
                # Comments beginning with '#' should be specifically ignored
                
                The following dates have comments following a tab character "\t"
                9/2/2024        Labor day
                # 9/16/2024     My birthday doesn't count as a holiday
                11/27/2024      No classes
                11/28/2024      Thanksgiving
                11/29/2024      Thanksgiving holiday
                12/24/2024      Christmas Eve
                12/25/2024      Christmas Day
                12/31/2024      New Years Holiday
                1/1/2025        New Years Holiday
                """;
    }
    private void validateExpectedHolidays(String encodedPublicHolidays) {
        DateTimeUtils dateTimeUtils = new DateTimeUtils();
        var initializedPublicHolidays = dateTimeUtils.initializePublicHolidays(encodedPublicHolidays);

        Assertions.assertEquals(17, initializedPublicHolidays.size(),
                "Set does not have the right number of public holidays");

        LocalDate[] sampleExpectedHolidays = {
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 15),
                LocalDate.of(2024, 4, 25),
                LocalDate.of(2024, 6, 19),
                LocalDate.of(2024, 9, 2),
                LocalDate.of(2024, 12, 25),
                LocalDate.of(2025, 1, 1),
        };
        for (var expectedHoliday : sampleExpectedHolidays) {
            Assertions.assertTrue(initializedPublicHolidays.contains(expectedHoliday),
                    "Expected holiday not found in resulting set: " + expectedHoliday.toString());
        }
    }

    private record ExpectedDaysLate(String handInDate, int daysLate){}
}
