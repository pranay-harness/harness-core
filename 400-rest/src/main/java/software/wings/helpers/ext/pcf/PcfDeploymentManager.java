package software.wings.helpers.ext.pcf;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pcf.PivotalClientApiException;
import io.harness.pcf.model.PcfAppAutoscalarRequestData;
import io.harness.pcf.model.PcfRequestConfig;

import software.wings.beans.PcfConfig;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.helpers.ext.pcf.request.PcfCreateApplicationRequestData;

import java.util.List;
import org.cloudfoundry.operations.applications.ApplicationDetail;
import org.cloudfoundry.operations.applications.ApplicationSummary;

@OwnedBy(CDP)
public interface PcfDeploymentManager {
  List<String> getOrganizations(io.harness.pcf.model.PcfRequestConfig pcfRequestConfig)
      throws PivotalClientApiException;

  List<String> getSpacesForOrganization(io.harness.pcf.model.PcfRequestConfig pcfRequestConfig)
      throws PivotalClientApiException;

  ApplicationDetail createApplication(PcfCreateApplicationRequestData requestData,
      ExecutionLogCallback executionLogCallback) throws PivotalClientApiException;

  ApplicationDetail resizeApplication(io.harness.pcf.model.PcfRequestConfig pcfRequestConfig)
      throws PivotalClientApiException;

  void deleteApplication(io.harness.pcf.model.PcfRequestConfig pcfRequestConfig) throws PivotalClientApiException;

  String stopApplication(io.harness.pcf.model.PcfRequestConfig pcfRequestConfig) throws PivotalClientApiException;

  ApplicationDetail getApplicationByName(io.harness.pcf.model.PcfRequestConfig pcfRequestConfig)
      throws PivotalClientApiException;

  void unmapRouteMapForApplication(io.harness.pcf.model.PcfRequestConfig pcfRequestConfig, List<String> paths,
      ExecutionLogCallback logCallback) throws PivotalClientApiException;

  void mapRouteMapForApplication(io.harness.pcf.model.PcfRequestConfig pcfRequestConfig, List<String> paths,
      ExecutionLogCallback logCallback) throws PivotalClientApiException;

  List<ApplicationSummary> getDeployedServicesWithNonZeroInstances(
      io.harness.pcf.model.PcfRequestConfig pcfRequestConfig, String prefix) throws PivotalClientApiException;

  List<ApplicationSummary> getPreviousReleases(io.harness.pcf.model.PcfRequestConfig pcfRequestConfig, String prefix)
      throws PivotalClientApiException;

  List<String> getRouteMaps(io.harness.pcf.model.PcfRequestConfig pcfRequestConfig) throws PivotalClientApiException;

  String checkConnectivity(PcfConfig pcfConfig, boolean limitPcfThreads, boolean ignorePcfConnectionContextCache);

  String createRouteMap(io.harness.pcf.model.PcfRequestConfig pcfRequestConfig, String host, String domain, String path,
      boolean tcpRoute, boolean useRandomPort, Integer port) throws PivotalClientApiException, InterruptedException;

  void performConfigureAutoscalar(PcfAppAutoscalarRequestData appAutoscalarRequestData,
      ExecutionLogCallback executionLogCallback) throws PivotalClientApiException;
  boolean changeAutoscalarState(PcfAppAutoscalarRequestData appAutoscalarRequestData,
      ExecutionLogCallback executionLogCallback, boolean enable) throws PivotalClientApiException;
  boolean checkIfAppHasAutoscalarAttached(PcfAppAutoscalarRequestData appAutoscalarRequestData,
      ExecutionLogCallback executionLogCallback) throws PivotalClientApiException;
  ApplicationDetail upsizeApplicationWithSteadyStateCheck(io.harness.pcf.model.PcfRequestConfig pcfRequestConfig,
      ExecutionLogCallback executionLogCallback) throws PivotalClientApiException;

  boolean isActiveApplication(io.harness.pcf.model.PcfRequestConfig pcfRequestConfig,
      ExecutionLogCallback executionLogCallback) throws PivotalClientApiException;

  void setEnvironmentVariableForAppStatus(io.harness.pcf.model.PcfRequestConfig pcfRequestConfig, boolean activeStatus,
      ExecutionLogCallback executionLogCallback) throws PivotalClientApiException;

  void unsetEnvironmentVariableForAppStatus(
      PcfRequestConfig pcfRequestConfig, ExecutionLogCallback executionLogCallback) throws PivotalClientApiException;
}
