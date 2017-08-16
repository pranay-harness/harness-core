package software.wings.service.impl.analysis;

import com.google.inject.Singleton;

import software.wings.stencils.DataProvider;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by rsingh on 08/16/17©.
 */
@Singleton
public class AnalysisComparisonStrategyProvider implements DataProvider {
  @Override
  public Map<String, String> getData(String appId, String... params) {
    final Map<String, String> rv = new HashMap<>();
    rv.put(AnalysisComparisonStrategy.COMPARE_WITH_PREVIOUS.name(), "Previous Execution Logs");
    rv.put(AnalysisComparisonStrategy.COMPARE_WITH_CURRENT.name(), "Logs from older version nodes");
    return rv;
  }
}
