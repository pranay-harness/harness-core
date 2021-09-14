/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.cf.apprenaming;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.delegate.beans.pcf.CfAppSetupTimeDetails;
import io.harness.delegate.beans.pcf.CfRouteUpdateRequestConfigData;
import io.harness.delegate.cf.PcfCommandTaskBaseHelper;
import io.harness.logging.LogCallback;
import io.harness.pcf.CfDeploymentManager;
import io.harness.pcf.PivotalClientApiException;
import io.harness.pcf.model.CfRequestConfig;

import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;
import org.cloudfoundry.operations.applications.ApplicationSummary;

public interface AppRenamingOperator {
  enum NamingTransition {
    VERSION_TO_NON_VERSION,
    NON_VERSION_TO_NON_VERSION,
    NON_VERSION_TO_VERSION,
    ROLLBACK_OPERATOR
  }

  void renameApp(CfRouteUpdateRequestConfigData cfRouteUpdateConfigData, CfRequestConfig cfRequestConfig,
      LogCallback executionLogCallback, CfDeploymentManager pcfDeploymentManager,
      PcfCommandTaskBaseHelper pcfCommandTaskBaseHelper) throws PivotalClientApiException;

  static AppRenamingOperator of(NamingTransition transition) throws PivotalClientApiException {
    switch (transition) {
      case VERSION_TO_NON_VERSION:
        return new VersionToNonVersionOperator();
      case NON_VERSION_TO_VERSION:
        return new NonVersionToVersionOperator();
      case NON_VERSION_TO_NON_VERSION:
        return new NonVersionToNonVersionOperator();
      case ROLLBACK_OPERATOR:
        return new AppRenamingRollbackOperator();
      default:
        throw new PivotalClientApiException(String.format("%s is not supported", transition.name()));
    }
  }

  default boolean isValidAppDetails(CfAppSetupTimeDetails appDetails, String appGuid) {
    return appDetails != null && isNotEmpty(appDetails.getApplicationGuid())
        && isNotEmpty(appDetails.getApplicationName()) && appGuid.equalsIgnoreCase(appDetails.getApplicationGuid());
  }

  default AppRenamingData populateRenamingData(
      ApplicationSummary appSummary, String appGuid, String currentName, String newName) {
    return AppRenamingData.builder()
        .newName(newName)
        .guid(appGuid)
        .currentName(currentName)
        .appSummary(appSummary)
        .build();
  }

  default TreeMap<AppType, AppRenamingData> getAppsInTheRenamingOrder(
      CfRouteUpdateRequestConfigData cfRouteUpdateConfigData, List<ApplicationSummary> allReleases) {
    CfAppSetupTimeDetails inActiveAppBeforeThisDeployment =
        cfRouteUpdateConfigData.getExistingInActiveApplicationDetails();
    CfAppSetupTimeDetails activeAppBeforeThisDeployment =
        isNotEmpty(cfRouteUpdateConfigData.getExistingApplicationDetails())
        ? cfRouteUpdateConfigData.getExistingApplicationDetails().get(0)
        : CfAppSetupTimeDetails.builder().build();
    CfAppSetupTimeDetails newApplicationDetails = cfRouteUpdateConfigData.getNewApplicationDetails();

    TreeMap<AppType, AppRenamingData> appTypeApplicationSummaryMap = new TreeMap<>(getComparatorForRenamingOrder());

    populateAppDetailsForRenaming(inActiveAppBeforeThisDeployment, activeAppBeforeThisDeployment, newApplicationDetails,
        allReleases, appTypeApplicationSummaryMap);

    return appTypeApplicationSummaryMap;
  }

  default void populateAppDetailsForRenaming(CfAppSetupTimeDetails inActiveApp, CfAppSetupTimeDetails activeApp,
      CfAppSetupTimeDetails newApp, List<ApplicationSummary> allReleases,
      TreeMap<AppType, AppRenamingData> appTypeApplicationSummaryMap) {
    for (ApplicationSummary appSummary : allReleases) {
      String appGuid = appSummary.getId();

      if (isValidAppDetails(inActiveApp, appGuid)) {
        appTypeApplicationSummaryMap.put(
            AppType.INACTIVE, populateRenamingData(appSummary, appGuid, appSummary.getName(), null));

      } else if (isValidAppDetails(activeApp, appGuid)) {
        appTypeApplicationSummaryMap.put(
            AppType.ACTIVE, populateRenamingData(appSummary, appGuid, appSummary.getName(), null));

      } else if (isValidAppDetails(newApp, appGuid)) {
        appTypeApplicationSummaryMap.put(
            AppType.STAGE, populateRenamingData(appSummary, appGuid, appSummary.getName(), null));
      }
    }
  }

  default Comparator<AppType> getComparatorForRenamingOrder() {
    return Comparator.comparingInt(Enum::ordinal);
  }
}
