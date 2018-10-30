package software.wings.verification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

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

  public static List<TimeSeriesDataPoint> initializeTimeSeriesDataPointsList(
      long startTime, long endTime, long period, int initialValue) {
    List<TimeSeriesDataPoint> timeSeriesDataPointList = new ArrayList<>();
    for (long i = startTime; i + period <= endTime; i += period) {
      timeSeriesDataPointList.add(TimeSeriesDataPoint.builder().timestamp(i).value(initialValue).build());
    }
    return timeSeriesDataPointList;
  }
}
