package software.wings.service.intfc.appdynamics;

import io.harness.cvng.beans.AppdynamicsValidationResponse;
import io.harness.cvng.core.services.entities.MetricPack;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse;
import software.wings.service.impl.appdynamics.AppdynamicsSetupTestNodeData;
import software.wings.service.impl.appdynamics.AppdynamicsTier;
import software.wings.service.impl.newrelic.NewRelicApplication;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import javax.validation.constraints.NotNull;

/**
 * Created by rsingh on 4/17/17.
 */
public interface AppdynamicsService {
  List<NewRelicApplication> getApplications(@NotNull String settingId) throws IOException;

  Set<AppdynamicsTier> getTiers(String settingId, long appdynamicsAppId) throws IOException;
  Set<AppdynamicsTier> getTiers(String settingId, long appdynamicsAppId, ThirdPartyApiCallLog apiCallLog)
      throws IOException;

  Set<AppdynamicsTier> getDependentTiers(String settingId, long appdynamicsAppId, AppdynamicsTier tier)
      throws IOException;
  Set<AppdynamicsTier> getDependentTiers(String settingId, long appdynamicsAppId, AppdynamicsTier tier,
      ThirdPartyApiCallLog apiCallLog) throws IOException;

  /**
   * Api to fetch metric data for given node.
   * @param appdynamicsSetupTestNodeData
   * @return
   */
  VerificationNodeDataSetupResponse getMetricsWithDataForNode(
      AppdynamicsSetupTestNodeData appdynamicsSetupTestNodeData);

  NewRelicApplication getAppDynamicsApplication(String connectorId, String appDynamicsApplicationId);
  AppdynamicsTier getTier(String connectorId, long appdynamicsAppId, String tierId);
  AppdynamicsTier getTier(String connectorId, long appdynamicsAppId, String tierId, ThirdPartyApiCallLog apiCallLog);

  String getAppDynamicsApplicationByName(String analysisServerConfigId, String applicationName);

  String getTierByName(
      String analysisServerConfigId, String applicationId, String tierName, ThirdPartyApiCallLog apiCallLog);

  Set<AppdynamicsValidationResponse> getMetricPackData(String accountId, String projectId, String connectorId,
      long appdAppId, long appdTierId, String requestGuid, List<MetricPack> metricPacks);
}
