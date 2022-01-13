/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.intfc.splunk;

import io.harness.cvng.beans.SplunkSavedSearch;
import io.harness.cvng.beans.SplunkValidationResponse;
import io.harness.delegate.beans.connector.splunkconnector.SplunkConnectorDTO;

import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse;
import software.wings.service.impl.splunk.SplunkSetupTestNodeData;

import java.util.List;

/**
 * Interface containing API's for Splunk analysis service.
 * Created by Pranjal on 08/31/2018
 */
public interface SplunkAnalysisService {
  /**
   * API to fetch LogData based on given host.
   * @param accountId
   * @param elkSetupTestNodeData
   * @return
   */
  VerificationNodeDataSetupResponse getLogDataByHost(String accountId, SplunkSetupTestNodeData elkSetupTestNodeData);
  List<SplunkSavedSearch> getSavedSearches(
      SplunkConnectorDTO splunkConnectorDTO, String orgIdentifier, String projectIdentifier, String requestGuid);

  SplunkValidationResponse getValidationResponse(SplunkConnectorDTO splunkConnectorDTO, String orgIdentifier,
      String projectIdentifier, String query, String requestGuid);
}
