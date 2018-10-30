package software.wings.verification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.wings.verification.dashboard.HeatMapUnit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Vaibhav Tulsyan
 * 11/Oct/2018
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HeatMap {
  private CVConfiguration cvConfiguration;
  @Default private List<HeatMapUnit> riskLevelSummary = new ArrayList<>();

  // txn name -> metric name -> time series
  private Map<String, Map<String, List<TimeSeriesDataPoint>>> observedTimeSeries;
  private Map<String, Map<String, List<TimeSeriesDataPoint>>> predictedTimeSeries;
}
