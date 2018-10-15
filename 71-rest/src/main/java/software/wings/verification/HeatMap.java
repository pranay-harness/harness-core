package software.wings.verification;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.wings.verification.dashboard.HeatMapUnit;

import java.util.List;

/**
 * @author Vaibhav Tulsyan
 * 11/Oct/2018
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HeatMap {
  private CVConfiguration cvConfiguration;
  private List<HeatMapUnit> riskLevelSummary;
  private List<TimeSeriesDataPoint> observedTimeSeries;
  private List<TimeSeriesDataPoint> predictedTimeSeries;
}
