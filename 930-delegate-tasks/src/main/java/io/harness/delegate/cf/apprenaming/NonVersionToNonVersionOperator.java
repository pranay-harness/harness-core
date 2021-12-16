package io.harness.delegate.cf.apprenaming;

import io.harness.delegate.beans.pcf.CfRouteUpdateRequestConfigData;
import io.harness.delegate.cf.PcfCommandTaskBaseHelper;
import io.harness.logging.LogCallback;
import io.harness.pcf.CfDeploymentManager;
import io.harness.pcf.PivotalClientApiException;
import io.harness.pcf.model.CfRequestConfig;
import io.harness.pcf.model.PcfConstants;

import java.util.List;
import java.util.TreeMap;
import org.cloudfoundry.operations.applications.ApplicationSummary;

/**
 * Before renaming the system will have following apps:-
 * OrderService_0
 * OrderService_INACTIVE
 * OrderService
 * OrderService_STAGE
 * <p>
 * After renaming
 * --------------
 * OrderService_0           -->   OrderService_0
 * OrderService_1           -->   OrderService_1
 * OrderService             -->   OrderService_INACTIVE
 * OrderService_INACTIVE    -->   OrderService
 * <p>
 * The app should be renamed in these order
 * OrderService             --> OrderService_interim
 * OrderService_INACTIVE    --> OrderService
 * OrderService_interim     --> OrderService_INACTIVE
 */
public class NonVersionToNonVersionOperator implements AppRenamingOperator {
  @Override
  public void renameApp(CfRouteUpdateRequestConfigData cfRouteUpdateConfigData, CfRequestConfig cfRequestConfig,
      LogCallback executionLogCallback, CfDeploymentManager pcfDeploymentManager,
      PcfCommandTaskBaseHelper pcfCommandTaskBaseHelper) throws PivotalClientApiException {
    String cfAppNamePrefix = cfRouteUpdateConfigData.getCfAppNamePrefix();
    List<ApplicationSummary> allReleases = pcfDeploymentManager.getPreviousReleases(cfRequestConfig, cfAppNamePrefix);

    TreeMap<AppType, AppRenamingData> appTypeApplicationSummaryMap =
        getAppsInTheRenamingOrder(cfRouteUpdateConfigData, allReleases);

    if (!appTypeApplicationSummaryMap.containsKey(AppType.ACTIVE)) {
      // first deployment in non-version -> non-version
      ApplicationSummary applicationSummary = appTypeApplicationSummaryMap.get(AppType.NEW).getAppSummary();
      pcfCommandTaskBaseHelper.renameApp(applicationSummary, cfRequestConfig, executionLogCallback, cfAppNamePrefix);
      return;
    }

    ApplicationSummary currentActiveApplicationSummary =
        appTypeApplicationSummaryMap.get(AppType.ACTIVE).getAppSummary();
    String intermediateName = PcfConstants.generateInterimAppName(cfAppNamePrefix);
    pcfCommandTaskBaseHelper.renameApp(
        currentActiveApplicationSummary, cfRequestConfig, executionLogCallback, intermediateName);

    ApplicationSummary newApplicationSummary = appTypeApplicationSummaryMap.get(AppType.NEW).getAppSummary();
    pcfCommandTaskBaseHelper.renameApp(newApplicationSummary, cfRequestConfig, executionLogCallback, cfAppNamePrefix);

    String inActiveName = cfAppNamePrefix + PcfConstants.INACTIVE_APP_NAME_SUFFIX;
    pcfCommandTaskBaseHelper.renameApp(
        currentActiveApplicationSummary, cfRequestConfig, executionLogCallback, inActiveName, intermediateName);
  }
}
