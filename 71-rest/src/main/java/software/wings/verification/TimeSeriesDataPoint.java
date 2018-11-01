package software.wings.verification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.SortedMap;
import java.util.TreeMap;

/**
 * @author Vaibhav Tulsyan
 * 13/Oct/2018
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimeSeriesDataPoint {
  private long timestamp;
  private double value;

  public static SortedMap<Long, TimeSeriesDataPoint> initializeTimeSeriesDataPointsList(
      long startTime, long endTime, long period, int initialValue) {
    SortedMap<Long, TimeSeriesDataPoint> timeSeriesDataPoints = new TreeMap<>();
    for (long i = startTime; i + period <= endTime; i += period) {
      timeSeriesDataPoints.put(i, TimeSeriesDataPoint.builder().timestamp(i).value(initialValue).build());
    }
    return timeSeriesDataPoints;
  }
}
