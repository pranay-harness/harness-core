package software.wings.helpers.ext.pcf;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pcf.PivotalClientApiException;
import io.harness.pcf.model.CfRequestConfig;
import io.harness.pcf.model.CfAppAutoscalarRequestData;
import io.harness.pcf.model.CfCreateApplicationRequestData;

import software.wings.beans.PcfConfig;
import software.wings.beans.command.ExecutionLogCallback;

import java.util.List;
import org.cloudfoundry.operations.applications.ApplicationDetail;
import org.cloudfoundry.operations.applications.ApplicationSummary;

@OwnedBy(CDP)
public interface PcfDeploymentManager {
  List<String> getOrganizations(CfRequestConfig cfRequestConfig) throws PivotalClientApiException;

  List<String> getSpacesForOrganization(CfRequestConfig cfRequestConfig) throws PivotalClientApiException;

  ApplicationDetail createApplication(CfCreateApplicationRequestData requestData,
      ExecutionLogCallback executionLogCallback) throws PivotalClientApiException;

  ApplicationDetail resizeApplication(CfRequestConfig cfRequestConfig) throws PivotalClientApiException;

  void deleteApplication(CfRequestConfig cfRequestConfig) throws PivotalClientApiException;

  String stopApplication(CfRequestConfig cfRequestConfig) throws PivotalClientApiException;

  ApplicationDetail getApplicationByName(CfRequestConfig cfRequestConfig) throws PivotalClientApiException;

  void unmapRouteMapForApplication(CfRequestConfig cfRequestConfig, List<String> paths,
      ExecutionLogCallback logCallback) throws PivotalClientApiException;

  void mapRouteMapForApplication(CfRequestConfig cfRequestConfig, List<String> paths, ExecutionLogCallback logCallback)
      throws PivotalClientApiException;

  List<ApplicationSummary> getDeployedServicesWithNonZeroInstances(CfRequestConfig cfRequestConfig, String prefix)
      throws PivotalClientApiException;

  List<ApplicationSummary> getPreviousReleases(CfRequestConfig cfRequestConfig, String prefix)
      throws PivotalClientApiException;

  List<String> getRouteMaps(CfRequestConfig cfRequestConfig) throws PivotalClientApiException;

  String checkConnectivity(PcfConfig pcfConfig, boolean limitPcfThreads, boolean ignorePcfConnectionContextCache);

  String createRouteMap(CfRequestConfig cfRequestConfig, String host, String domain, String path, boolean tcpRoute,
      boolean useRandomPort, Integer port) throws PivotalClientApiException, InterruptedException;

  void performConfigureAutoscalar(CfAppAutoscalarRequestData appAutoscalarRequestData,
      ExecutionLogCallback executionLogCallback) throws PivotalClientApiException;
  boolean changeAutoscalarState(CfAppAutoscalarRequestData appAutoscalarRequestData,
      ExecutionLogCallback executionLogCallback, boolean enable) throws PivotalClientApiException;
  boolean checkIfAppHasAutoscalarAttached(CfAppAutoscalarRequestData appAutoscalarRequestData,
      ExecutionLogCallback executionLogCallback) throws PivotalClientApiException;
  ApplicationDetail upsizeApplicationWithSteadyStateCheck(
      CfRequestConfig cfRequestConfig, ExecutionLogCallback executionLogCallback) throws PivotalClientApiException;

  boolean isActiveApplication(CfRequestConfig cfRequestConfig, ExecutionLogCallback executionLogCallback)
      throws PivotalClientApiException;

  void setEnvironmentVariableForAppStatus(CfRequestConfig cfRequestConfig, boolean activeStatus,
      ExecutionLogCallback executionLogCallback) throws PivotalClientApiException;

  void unsetEnvironmentVariableForAppStatus(CfRequestConfig cfRequestConfig, ExecutionLogCallback executionLogCallback)
      throws PivotalClientApiException;
}
