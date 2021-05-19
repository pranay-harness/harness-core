package software.wings.helpers.ext.pcf;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.logging.LogCallback;
import io.harness.pcf.PivotalClientApiException;
import io.harness.pcf.model.CfAppAutoscalarRequestData;
import io.harness.pcf.model.CfConfig;
import io.harness.pcf.model.CfCreateApplicationRequestData;
import io.harness.pcf.model.CfRequestConfig;

import java.util.List;
import org.cloudfoundry.operations.applications.ApplicationDetail;
import org.cloudfoundry.operations.applications.ApplicationSummary;

@OwnedBy(CDP)
public interface CfDeploymentManager {
  List<String> getOrganizations(CfRequestConfig cfRequestConfig) throws PivotalClientApiException;

  List<String> getSpacesForOrganization(CfRequestConfig cfRequestConfig) throws PivotalClientApiException;

  ApplicationDetail createApplication(CfCreateApplicationRequestData requestData, LogCallback logCallback)
      throws PivotalClientApiException;

  ApplicationDetail resizeApplication(CfRequestConfig cfRequestConfig) throws PivotalClientApiException;

  void deleteApplication(CfRequestConfig cfRequestConfig) throws PivotalClientApiException;

  String stopApplication(CfRequestConfig cfRequestConfig) throws PivotalClientApiException;

  ApplicationDetail getApplicationByName(CfRequestConfig cfRequestConfig) throws PivotalClientApiException;

  void unmapRouteMapForApplication(CfRequestConfig cfRequestConfig, List<String> paths, LogCallback logCallback)
      throws PivotalClientApiException;

  void mapRouteMapForApplication(CfRequestConfig cfRequestConfig, List<String> paths, LogCallback logCallback)
      throws PivotalClientApiException;

  List<ApplicationSummary> getDeployedServicesWithNonZeroInstances(CfRequestConfig cfRequestConfig, String prefix)
      throws PivotalClientApiException;

  List<ApplicationSummary> getPreviousReleases(CfRequestConfig cfRequestConfig, String prefix)
      throws PivotalClientApiException;

  List<String> getRouteMaps(CfRequestConfig cfRequestConfig) throws PivotalClientApiException;

  String checkConnectivity(CfConfig cfConfig, boolean limitPcfThreads, boolean ignorePcfConnectionContextCache);

  String createRouteMap(CfRequestConfig cfRequestConfig, String host, String domain, String path, boolean tcpRoute,
      boolean useRandomPort, Integer port) throws PivotalClientApiException, InterruptedException;

  void performConfigureAutoscalar(io.harness.pcf.model.CfAppAutoscalarRequestData appAutoscalarRequestData,
      LogCallback logCallback) throws PivotalClientApiException;

  boolean changeAutoscalarState(io.harness.pcf.model.CfAppAutoscalarRequestData appAutoscalarRequestData,
      LogCallback logCallback, boolean enable) throws PivotalClientApiException;

  boolean checkIfAppHasAutoscalarAttached(CfAppAutoscalarRequestData appAutoscalarRequestData, LogCallback logCallback)
      throws PivotalClientApiException;

  ApplicationDetail upsizeApplicationWithSteadyStateCheck(CfRequestConfig cfRequestConfig, LogCallback logCallback)
      throws PivotalClientApiException;

  boolean isActiveApplication(CfRequestConfig cfRequestConfig, LogCallback logCallback)
      throws PivotalClientApiException;

  void setEnvironmentVariableForAppStatus(
      CfRequestConfig cfRequestConfig, boolean activeStatus, LogCallback logCallback) throws PivotalClientApiException;

  void unsetEnvironmentVariableForAppStatus(CfRequestConfig cfRequestConfig, LogCallback logCallback)
      throws PivotalClientApiException;
}
