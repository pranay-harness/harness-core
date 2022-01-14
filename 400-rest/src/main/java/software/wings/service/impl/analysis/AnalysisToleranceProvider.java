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
    rv.put(AnalysisTolerance.LOW.name(), "Very sensitive - even small deviations are flagged as anomalies");
    rv.put(AnalysisTolerance.MEDIUM.name(),
        "Moderately sensitive - only moderate deviations are flagged as anomalies (Recommended)");
    rv.put(AnalysisTolerance.HIGH.name(), "Least sensitive - only major deviations are flagged as anomalies");
    return rv;
  }
}
