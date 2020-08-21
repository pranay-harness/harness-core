package software.wings.service.intfc.splunk;

import io.harness.cvng.beans.SplunkSavedSearch;
import io.harness.cvng.beans.SplunkValidationResponse;
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
      String accountId, String connectorId, String orgIdentifier, String projectIdentifier, String requestGuid);

  SplunkValidationResponse getValidationResponse(String accountId, String connectorId, String orgIdentifier,
      String projectIdentifier, String query, String requestGuid);
}
