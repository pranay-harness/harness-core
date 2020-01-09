package software.wings.utils;

import org.joda.time.DateTimeConstants;
import software.wings.graphql.schema.type.aggregation.audit.QLTimeUnit;

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
}
