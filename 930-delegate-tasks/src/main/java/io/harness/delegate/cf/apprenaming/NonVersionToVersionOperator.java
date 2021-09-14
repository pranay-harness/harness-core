/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.cf.apprenaming;

import io.harness.delegate.beans.pcf.CfRouteUpdateRequestConfigData;
import io.harness.delegate.cf.PcfCommandTaskBaseHelper;
import io.harness.logging.LogCallback;
import io.harness.pcf.CfDeploymentManager;
import io.harness.pcf.PivotalClientApiException;
import io.harness.pcf.model.CfRequestConfig;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.cloudfoundry.operations.applications.ApplicationSummary;

/**
 * Before renaming the system will have following apps:-
 * OrderService_0
 * OrderService_INACTIVE
 * OrderService
 * OrderService_STAGE
 *
 * After renaming
 * --------------
 * OrderService_0           -->   OrderService_0
 * OrderService_INACTIVE    -->   OrderService_1
 * OrderService             -->   OrderService_2
 * OrderService_STAGE       -->   OrderService_3
 *
 * The app should be renamed in these order --> INACTIVE --> ACTIVE --> STAGE
 */

public class NonVersionToVersionOperator implements AppRenamingOperator {
  @Override
  public void renameApp(CfRouteUpdateRequestConfigData cfRouteUpdateConfigData, CfRequestConfig cfRequestConfig,
      LogCallback executionLogCallback, CfDeploymentManager pcfDeploymentManager,
      PcfCommandTaskBaseHelper pcfCommandTaskBaseHelper) throws PivotalClientApiException {
    String cfAppNamePrefix = cfRouteUpdateConfigData.getCfAppNamePrefix();
    List<ApplicationSummary> allReleases = pcfDeploymentManager.getPreviousReleases(cfRequestConfig, cfAppNamePrefix);
    int maxVersion = PcfCommandTaskBaseHelper.getMaxVersion(allReleases);

    TreeMap<AppType, AppRenamingData> appTypeApplicationSummaryMap =
        getAppsInTheRenamingOrder(cfRouteUpdateConfigData, allReleases);

    int versionNumber = maxVersion;
    Set<Map.Entry<AppType, AppRenamingData>> entries = appTypeApplicationSummaryMap.entrySet();
    for (Map.Entry<AppType, AppRenamingData> entry : entries) {
      ApplicationSummary applicationSummary = entry.getValue().getAppSummary();
      String newAppName = cfAppNamePrefix + PcfCommandTaskBaseHelper.DELIMITER + ++versionNumber;
      pcfCommandTaskBaseHelper.renameApp(applicationSummary, cfRequestConfig, executionLogCallback, newAppName);
    }
  }
}
