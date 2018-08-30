package software.wings.service.intfc.sumo;

import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse;
import software.wings.service.impl.sumo.SumoLogicSetupTestNodedata;
import software.wings.service.intfc.analysis.AnalysisService;

/**
 * Created by Pranjal on 08/21/2018
 */
public interface SumoLogicAnalysisService extends AnalysisService {
  /**
   * Method to get log data based on host provided.
   * @param accountId
   * @param sumoLogicSetupTestNodedata
   * @return
   */
  VerificationNodeDataSetupResponse getLogDataByHost(
      String accountId, SumoLogicSetupTestNodedata sumoLogicSetupTestNodedata);
}
