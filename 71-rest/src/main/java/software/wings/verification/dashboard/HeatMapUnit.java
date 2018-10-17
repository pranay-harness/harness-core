package software.wings.verification.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.wings.verification.TimeSeriesDataPoint;

import java.util.List;

/**
 * @author Vaibhav Tulsyan
 * 11/Oct/2018
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HeatMapUnit {
  private long startTime;
  private long endTime;
  private int highRisk;
  private int mediumRisk;
  private int lowRisk;
  private int NA;
  private List<TimeSeriesDataPoint> timeSeries;
}
