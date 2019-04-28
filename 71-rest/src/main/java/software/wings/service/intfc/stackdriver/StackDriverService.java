package software.wings.service.intfc.stackdriver;

import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse;
import software.wings.service.impl.stackdriver.StackDriverMetric;
import software.wings.service.impl.stackdriver.StackDriverSetupTestNodeData;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Created by Pranjal on 11/27/2018
 */
public interface StackDriverService {
  /**
   * Api to fetch metric data for given node.
   * @param setupTestNodeData
   * @return
   */
  VerificationNodeDataSetupResponse getMetricsWithDataForNode(StackDriverSetupTestNodeData setupTestNodeData)
      throws IOException;

  /**
   * Api to fetch all the metrics support by Harness for StackDriver
   * @return
   */
  Map<String, List<StackDriverMetric>> getMetrics();

  List<String> listRegions(String settingId) throws IOException;

  Map<String, String> listForwardingRules(String settingId, String region) throws IOException;
}
