package software.wings.service.intfc.dynatrace;

import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse;
import software.wings.service.impl.dynatrace.DynaTraceSetupTestNodeData;

/**
 * Interface for DynaTrace Service.
 * Created by Pranjal on 09/12/2018
 */
public interface DynaTraceService {
  /**
   * Method to fetch metric data based on Given Service Methods
   * @param setupTestNodeData
   * @return
   */
  VerificationNodeDataSetupResponse getMetricsWithDataForNode(DynaTraceSetupTestNodeData setupTestNodeData);
}
