package software.wings.service.intfc.analysis;

import software.wings.APMFetchConfig;
import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse;

/**
 * @author Praveen 9/6/18
 */
public interface APMVerificationService {
  VerificationNodeDataSetupResponse getMetricsWithDataForNode(
      String accountId, String serverConfigId, APMFetchConfig url);
}
