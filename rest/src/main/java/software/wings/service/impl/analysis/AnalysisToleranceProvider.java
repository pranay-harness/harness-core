package software.wings.service.impl.analysis;

import software.wings.stencils.DataProvider;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by sriram_parthasarathy on 10/26/17.
 */
public class AnalysisToleranceProvider implements DataProvider {
  @Override
  public Map<String, String> getData(String appId, Map<String, String> params) {
    final Map<String, String> rv = new HashMap<>();
    rv.put(AnalysisTolerance.LOW.name(), "All anomalies");
    rv.put(AnalysisTolerance.MEDIUM.name(), "Anomalies with medium risk or higher");
    rv.put(AnalysisTolerance.HIGH.name(), "Anomalies with high risk");
    return rv;
  }
}
