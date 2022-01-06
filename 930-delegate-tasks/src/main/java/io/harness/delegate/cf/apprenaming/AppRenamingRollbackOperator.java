/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.cf.apprenaming;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static software.wings.beans.LogColor.White;
import static software.wings.beans.LogHelper.color;
import static software.wings.beans.LogWeight.Bold;

import io.harness.delegate.beans.pcf.CfAppSetupTimeDetails;
import io.harness.delegate.beans.pcf.CfRouteUpdateRequestConfigData;
import io.harness.delegate.cf.PcfCommandTaskBaseHelper;
import io.harness.logging.LogCallback;
import io.harness.pcf.CfDeploymentManager;
import io.harness.pcf.PivotalClientApiException;
import io.harness.pcf.model.CfRequestConfig;
import io.harness.pcf.model.PcfConstants;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import org.cloudfoundry.operations.applications.ApplicationSummary;

/**
 * Fetch the current apps present in PCF cluster
 * Based on guid match the previous saved apps details for Active, InActive & NewApp
 * If for any of these apps the name has been changed we rename it back with the old name
 *
 * The app should be renamed in these order --> NEW --> ACTIVE --> INACTIVE
 */
public class AppRenamingRollbackOperator implements AppRenamingOperator {
  @Override
  public void renameApp(CfRouteUpdateRequestConfigData cfRouteUpdateConfigData, CfRequestConfig cfRequestConfig,
      LogCallback executionLogCallback, CfDeploymentManager pcfDeploymentManager,
      PcfCommandTaskBaseHelper pcfCommandTaskBaseHelper) throws PivotalClientApiException {
    AppNamingStrategy existingStrategy = AppNamingStrategy.get(cfRouteUpdateConfigData.getExistingAppNamingStrategy());
    if (AppNamingStrategy.VERSIONING == existingStrategy && !cfRouteUpdateConfigData.isNonVersioning()) {
      // versioning to versioning does not require renaming
      return;
    }

    executionLogCallback.saveExecutionLog(color("# Reverting app names", White, Bold));

    String cfAppNamePrefix = cfRouteUpdateConfigData.getCfAppNamePrefix();
    List<ApplicationSummary> allReleases = pcfDeploymentManager.getPreviousReleases(cfRequestConfig, cfAppNamePrefix);

    SortedMap<AppType, AppRenamingData> appTypeApplicationSummaryMap =
        getAppsInTheRenamingOrder(cfRouteUpdateConfigData, allReleases);

    Set<Map.Entry<AppType, AppRenamingData>> entries = appTypeApplicationSummaryMap.entrySet();
    for (Map.Entry<AppType, AppRenamingData> entry : entries) {
      AppRenamingData appRenamingData = entry.getValue();
      pcfCommandTaskBaseHelper.renameApp(
          appRenamingData.getAppSummary(), cfRequestConfig, executionLogCallback, appRenamingData.getNewName());
    }
  }

  @Override
  public void populateAppDetailsForRenaming(CfAppSetupTimeDetails inActiveApp, CfAppSetupTimeDetails activeApp,
      CfAppSetupTimeDetails newApp, List<ApplicationSummary> allReleases,
      TreeMap<AppType, AppRenamingData> appTypeApplicationSummaryMap, String cfAppNamePrefix) {
    for (ApplicationSummary appSummary : allReleases) {
      String appGuid = appSummary.getId();
      String appName = appSummary.getName();

      if (isValidAppDetails(inActiveApp, appGuid)) {
        String oldName = inActiveApp.getOldName();
        if (isNotEmpty(oldName) && !appName.equalsIgnoreCase(oldName)) {
          appTypeApplicationSummaryMap.put(
              AppType.INACTIVE, populateRenamingData(appSummary, appGuid, appName, oldName));
        }
      } else if (isValidAppDetails(activeApp, appGuid)) {
        String oldName = activeApp.getOldName();
        if (isNotEmpty(oldName) && !appName.equalsIgnoreCase(oldName)) {
          appTypeApplicationSummaryMap.put(AppType.ACTIVE, populateRenamingData(appSummary, appGuid, appName, oldName));
        }
      } else if (isValidAppDetails(newApp, appGuid)) {
        String newName = PcfConstants.generateInterimAppName(cfAppNamePrefix);
        if (!appName.equalsIgnoreCase(newName)) {
          appTypeApplicationSummaryMap.put(AppType.NEW, populateRenamingData(appSummary, appGuid, appName, newName));
        }
      }
    }
  }

  @Override
  public Comparator<AppType> getComparatorForRenamingOrder() {
    return Collections.reverseOrder(Comparator.comparingInt(Enum::ordinal));
  }
}
