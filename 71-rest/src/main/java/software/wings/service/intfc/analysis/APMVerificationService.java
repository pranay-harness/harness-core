package software.wings.service.intfc.analysis;

import software.wings.APMFetchConfig;
import software.wings.api.MetricDataAnalysisResponse;
import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse;
import software.wings.sm.StateType;

/**
 * @author Praveen 9/6/18
 */
public interface APMVerificationService {
  VerificationNodeDataSetupResponse getMetricsWithDataForNode(
      String accountId, String serverConfigId, APMFetchConfig url);
  boolean sendNotifyForMetricAnalysis(String correlationId, MetricDataAnalysisResponse response);
  boolean collect247Data(
      String cvConfigId, StateType stateType, long startTime, long endTime, int lastDataCollectionMinute);
}
