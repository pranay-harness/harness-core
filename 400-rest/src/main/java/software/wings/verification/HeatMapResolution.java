package software.wings.verification;

import static software.wings.common.VerificationConstants.CRON_POLL_INTERVAL_IN_MINUTES;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author Vaibhav Tulsyan
 * 18/Oct/2018
 */

public enum HeatMapResolution {
  TWELVE_HOURS,
  ONE_DAY,
  SEVEN_DAYS,
  THIRTY_DAYS;

  private static Map<HeatMapResolution, Integer> scale;
  static {
    Map<HeatMapResolution, Integer> scaleMap = new HashMap<>();
    scaleMap.put(HeatMapResolution.TWELVE_HOURS, CRON_POLL_INTERVAL_IN_MINUTES); // 4 per hour
    scaleMap.put(HeatMapResolution.ONE_DAY, CRON_POLL_INTERVAL_IN_MINUTES * 2); // 2 per hour
    scaleMap.put(HeatMapResolution.SEVEN_DAYS, CRON_POLL_INTERVAL_IN_MINUTES * 16); // 6 per day <=> 1 per 4hrs
    scaleMap.put(HeatMapResolution.THIRTY_DAYS, CRON_POLL_INTERVAL_IN_MINUTES * 48); // 2 per day <=> 1 per 12hrs
    scale = Collections.unmodifiableMap(scaleMap);
  }

  public static HeatMapResolution getResolution(long startTime, long endTime) {
    int hours = (int) TimeUnit.MILLISECONDS.toHours(endTime - startTime);
    if (hours <= 12) {
      return HeatMapResolution.TWELVE_HOURS;
    } else if (hours <= 24) {
      return HeatMapResolution.ONE_DAY;
    } else if (hours <= 168) {
      return HeatMapResolution.SEVEN_DAYS;
    } else {
      return HeatMapResolution.THIRTY_DAYS;
    }
  }

  public int getDurationOfHeatMapUnit(HeatMapResolution resolution) {
    return scale.get(resolution);
  }

  /**
   * We get the number of minutes that span the resolution size (duration).
   * Using the polling interval, we find the no. of data points that we can
   * represent in each square.
   * @param resolution
   * @return Number of events to be shown within a heat map unit
   */
  public int getEventsPerHeatMapUnit(HeatMapResolution resolution) {
    return scale.get(resolution) / CRON_POLL_INTERVAL_IN_MINUTES;
  }
}
