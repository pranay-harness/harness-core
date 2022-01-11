/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

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
