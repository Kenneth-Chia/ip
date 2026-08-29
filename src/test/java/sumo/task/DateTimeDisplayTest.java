package sumo.task;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

/**
 * Tests the user-facing date and time formatting provided by {@link DateTimeDisplay}.
 */
public class DateTimeDisplayTest {
    @Test
    public void format_dateOnly_timeOmitted() {
        LocalDateTime dateTime = LocalDateTime.of(2026, 8, 29, 14, 5);

        assertEquals("Aug 29 2026", DateTimeDisplay.format(dateTime, false));
    }

    @Test
    public void format_dateOnlyWithSingleDigitDay_dayIsZeroPadded() {
        LocalDateTime dateTime = LocalDateTime.of(2026, 2, 3, 14, 5);

        assertEquals("Feb 03 2026", DateTimeDisplay.format(dateTime, false));
    }

    @Test
    public void format_dateAndMorningTime_timeShownWithAm() {
        LocalDateTime dateTime = LocalDateTime.of(2026, 1, 2, 9, 5);

        assertEquals("Jan 02 2026 9:05 AM", DateTimeDisplay.format(dateTime, true));
    }

    @Test
    public void format_midnight_timeShownAsTwelveAm() {
        LocalDateTime midnight = LocalDateTime.of(2026, 1, 2, 0, 0);

        assertEquals("Jan 02 2026 12:00 AM", DateTimeDisplay.format(midnight, true));
    }

    @Test
    public void format_noon_timeShownAsTwelvePm() {
        LocalDateTime noon = LocalDateTime.of(2026, 1, 2, 12, 0);

        assertEquals("Jan 02 2026 12:00 PM", DateTimeDisplay.format(noon, true));
    }

    @Test
    public void format_eveningTime_timeShownWithPm() {
        LocalDateTime dateTime = LocalDateTime.of(2026, 12, 31, 23, 59);

        assertEquals("Dec 31 2026 11:59 PM", DateTimeDisplay.format(dateTime, true));
    }
}
