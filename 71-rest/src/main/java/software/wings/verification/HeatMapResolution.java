package software.wings.verification;

import io.harness.exception.WingsException;
import software.wings.common.VerificationConstants;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author Vaibhav Tulsyan
 * 18/Oct/2018
 */

public enum HeatMapResolution {
  /*
  SMALL => [0, 1] days
  MEDIUM => (1, 7] days
  LARGE => (7, 30] days
   */
  SMALL,
  MEDIUM,
  LARGE;

  private static Map<HeatMapResolution, Integer> scale;
  static {
    Map<HeatMapResolution, Integer> scaleMap = new HashMap<>();
    scaleMap.put(HeatMapResolution.SMALL, 15);
    scaleMap.put(HeatMapResolution.MEDIUM, 180);
    scaleMap.put(HeatMapResolution.LARGE, 480);
    scale = Collections.unmodifiableMap(scaleMap);
  }

  public static HeatMapResolution getResolution(long startTime, long endTime) {
    int days = (int) TimeUnit.MILLISECONDS.toDays(endTime - startTime);
    if (days <= 1) {
      return HeatMapResolution.SMALL;
    } else if (days <= 7) {
      return HeatMapResolution.MEDIUM;
    } else if (days <= 30) {
      return HeatMapResolution.LARGE;
    } else {
      throw new WingsException("Unsupported Resolution Provided");
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
    return scale.get(resolution) / VerificationConstants.CRON_POLL_INTERVAL_IN_MINUTES;
  }
}
