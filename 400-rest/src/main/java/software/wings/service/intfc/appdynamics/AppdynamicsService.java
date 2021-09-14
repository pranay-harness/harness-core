/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.intfc.appdynamics;

import static io.harness.annotations.dev.HarnessTeam.CV;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse;
import software.wings.service.impl.appdynamics.AppdynamicsSetupTestNodeData;
import software.wings.service.impl.appdynamics.AppdynamicsTier;
import software.wings.service.impl.newrelic.NewRelicApplication;

import java.util.List;
import java.util.Set;
import javax.validation.constraints.NotNull;

/**
 * Created by rsingh on 4/17/17.
 */
@OwnedBy(CV)
@TargetModule(HarnessModule._360_CG_MANAGER)
public interface AppdynamicsService {
  List<NewRelicApplication> getApplications(@NotNull String settingId);
  List<NewRelicApplication> getApplications(@NotNull String settingId, String appId, String workflowExecutionId);

  Set<AppdynamicsTier> getTiers(String settingId, long appdynamicsAppId);
  Set<AppdynamicsTier> getTiers(String settingId, long appdynamicsAppId, ThirdPartyApiCallLog apiCallLog);
  Set<AppdynamicsTier> getTiers(String settingId, long appdynamicsAppId, String appId, String workflowExecutionId,
      ThirdPartyApiCallLog apiCallLog);

  /**
   * Api to fetch metric data for given node.
   * @param appdynamicsSetupTestNodeData
   * @return
   */
  VerificationNodeDataSetupResponse getMetricsWithDataForNode(
      AppdynamicsSetupTestNodeData appdynamicsSetupTestNodeData);

  NewRelicApplication getAppDynamicsApplication(String connectorId, String appDynamicsApplicationId);
  NewRelicApplication getAppDynamicsApplication(
      String connectorId, String appDynamicsApplicationId, String appId, String workflowExecutionId);
  AppdynamicsTier getTier(String connectorId, long appdynamicsAppId, String tierId);
  AppdynamicsTier getTier(String connectorId, long appdynamicsAppId, String tierId, String appId,
      String workflowExecutionId, ThirdPartyApiCallLog apiCallLog);

  String getAppDynamicsApplicationByName(
      String analysisServerConfigId, String applicationName, String appId, String workflowExecutionId);

  String getTierByName(String analysisServerConfigId, String applicationId, String tierName, String appId,
      String workflowExecutionId, ThirdPartyApiCallLog apiCallLog);
}
