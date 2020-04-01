package software.wings.helpers.ext.pcf;

import org.cloudfoundry.operations.applications.ApplicationDetail;
import org.cloudfoundry.operations.applications.ApplicationSummary;
import software.wings.beans.PcfConfig;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.helpers.ext.pcf.request.PcfAppAutoscalarRequestData;
import software.wings.helpers.ext.pcf.request.PcfCreateApplicationRequestData;

import java.util.List;

public interface PcfDeploymentManager {
  List<String> getOrganizations(PcfRequestConfig pcfRequestConfig) throws PivotalClientApiException;

  List<String> getSpacesForOrganization(PcfRequestConfig pcfRequestConfig) throws PivotalClientApiException;

  ApplicationDetail createApplication(PcfCreateApplicationRequestData requestData,
      ExecutionLogCallback executionLogCallback) throws PivotalClientApiException;

  ApplicationDetail resizeApplication(PcfRequestConfig pcfRequestConfig) throws PivotalClientApiException;

  void deleteApplication(PcfRequestConfig pcfRequestConfig) throws PivotalClientApiException;

  String stopApplication(PcfRequestConfig pcfRequestConfig) throws PivotalClientApiException;

  ApplicationDetail getApplicationByName(PcfRequestConfig pcfRequestConfig) throws PivotalClientApiException;

  void unmapRouteMapForApplication(PcfRequestConfig pcfRequestConfig, List<String> paths,
      ExecutionLogCallback logCallback) throws PivotalClientApiException;

  void mapRouteMapForApplication(PcfRequestConfig pcfRequestConfig, List<String> paths,
      ExecutionLogCallback logCallback) throws PivotalClientApiException;

  List<ApplicationSummary> getDeployedServicesWithNonZeroInstances(PcfRequestConfig pcfRequestConfig, String prefix)
      throws PivotalClientApiException;

  List<ApplicationSummary> getPreviousReleases(PcfRequestConfig pcfRequestConfig, String prefix)
      throws PivotalClientApiException;

  List<String> getRouteMaps(PcfRequestConfig pcfRequestConfig) throws PivotalClientApiException;

  String checkConnectivity(PcfConfig pcfConfig);

  String createRouteMap(PcfRequestConfig pcfRequestConfig, String host, String domain, String path, boolean tcpRoute,
      boolean useRandomPort, Integer port) throws PivotalClientApiException, InterruptedException;

  void performConfigureAutoscalar(PcfAppAutoscalarRequestData appAutoscalarRequestData,
      ExecutionLogCallback executionLogCallback) throws PivotalClientApiException;
  boolean changeAutoscalarState(PcfAppAutoscalarRequestData appAutoscalarRequestData,
      ExecutionLogCallback executionLogCallback, boolean enable) throws PivotalClientApiException;
  boolean checkIfAppAutoscalarInstalled() throws PivotalClientApiException;
  boolean checkIfAppHasAutoscalarAttached(PcfAppAutoscalarRequestData appAutoscalarRequestData,
      ExecutionLogCallback executionLogCallback) throws PivotalClientApiException;
  String resolvePcfPluginHome();
  ApplicationDetail upsizeApplicationWithSteadyStateCheck(
      PcfRequestConfig pcfRequestConfig, ExecutionLogCallback executionLogCallback) throws PivotalClientApiException;

  boolean isActiveApplication(PcfRequestConfig pcfRequestConfig, ExecutionLogCallback executionLogCallback)
      throws PivotalClientApiException;

  void setEnvironmentVariableForAppStatus(PcfRequestConfig pcfRequestConfig, boolean activeStatus,
      ExecutionLogCallback executionLogCallback) throws PivotalClientApiException;

  void unsetEnvironmentVariableForAppStatus(
      PcfRequestConfig pcfRequestConfig, ExecutionLogCallback executionLogCallback) throws PivotalClientApiException;
}
