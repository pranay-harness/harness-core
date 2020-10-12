package software.wings.utils;

import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.SUNDAY;

import org.joda.time.DateTimeConstants;
import software.wings.graphql.schema.type.aggregation.audit.QLTimeUnit;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoField;

/**
 * @author vardan
 */
public final class TimeUtils {
  private TimeUtils() {}
  /***
   * Get equivalent milliseconds in N weeks/days/hours/minutes
   * @param timeUnit
   * @param noOfUnits
   * @return
   */
  public static long getMillisFromTime(QLTimeUnit timeUnit, Long noOfUnits) {
    switch (timeUnit) {
      case WEEKS:
        return noOfUnits * DateTimeConstants.MILLIS_PER_WEEK;
      case DAYS:
        return noOfUnits * DateTimeConstants.MILLIS_PER_DAY;
      case HOURS:
        return noOfUnits * DateTimeConstants.MILLIS_PER_HOUR;
      case MINUTES:
        return noOfUnits * DateTimeConstants.MILLIS_PER_MINUTE;
      default:
        return 0;
    }
  }

  public static boolean isWeekend() {
    LocalDate currentDate = LocalDate.now(ZoneId.of(ZoneId.SHORT_IDS.get("PST")));
    DayOfWeek currentDay = DayOfWeek.of(currentDate.get(ChronoField.DAY_OF_WEEK));

    return currentDay == SATURDAY || currentDay == SUNDAY;
  }
}
