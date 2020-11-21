package software.wings.helpers.ext.pcf;

import software.wings.beans.PcfConfig;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.helpers.ext.pcf.request.PcfAppAutoscalarRequestData;
import software.wings.helpers.ext.pcf.request.PcfCreateApplicationRequestData;

import com.google.inject.Singleton;
import java.util.List;
import org.cloudfoundry.operations.applications.ApplicationDetail;
import org.cloudfoundry.operations.applications.ApplicationSummary;

@Singleton
public class PcfDeploymentManagerUnsupported implements PcfDeploymentManager {
  public static final String DELIMITER = "__";

  @Override
  public List<String> getOrganizations(PcfRequestConfig pcfRequestConfig) throws PivotalClientApiException {
    throw new PivotalClientApiException("PCF operations not supported.");
  }

  @Override
  public List<String> getSpacesForOrganization(PcfRequestConfig pcfRequestConfig) throws PivotalClientApiException {
    throw new PivotalClientApiException("PCF operations not supported by this API.");
  }

  @Override
  public ApplicationDetail createApplication(PcfCreateApplicationRequestData requestData,
      ExecutionLogCallback executionLogCallback) throws PivotalClientApiException {
    throw new PivotalClientApiException("PCF operations not supported by this API.");
  }

  @Override
  public ApplicationDetail resizeApplication(PcfRequestConfig pcfRequestConfig) throws PivotalClientApiException {
    throw new PivotalClientApiException("PCF operations not supported by this API.");
  }

  @Override
  public void deleteApplication(PcfRequestConfig pcfRequestConfig) throws PivotalClientApiException {
    throw new PivotalClientApiException("PCF operations not supported by this API.");
  }

  @Override
  public String stopApplication(PcfRequestConfig pcfRequestConfig) throws PivotalClientApiException {
    throw new PivotalClientApiException("PCF operations not supported by this API.");
  }

  @Override
  public ApplicationDetail getApplicationByName(PcfRequestConfig pcfRequestConfig) throws PivotalClientApiException {
    throw new PivotalClientApiException("PCF operations not supported by this API.");
  }

  @Override
  public void unmapRouteMapForApplication(PcfRequestConfig pcfRequestConfig, List<String> paths,
      ExecutionLogCallback logCallback) throws PivotalClientApiException {
    throw new PivotalClientApiException("PCF operations not supported by this API.");
  }

  @Override
  public void mapRouteMapForApplication(PcfRequestConfig pcfRequestConfig, List<String> paths,
      ExecutionLogCallback logCallback) throws PivotalClientApiException {
    throw new PivotalClientApiException("PCF operations not supported by this API.");
  }

  @Override
  public List<ApplicationSummary> getDeployedServicesWithNonZeroInstances(
      PcfRequestConfig pcfRequestConfig, String prefix) throws PivotalClientApiException {
    throw new PivotalClientApiException("PCF operations not supported by this API.");
  }

  @Override
  public List<ApplicationSummary> getPreviousReleases(PcfRequestConfig pcfRequestConfig, String prefix)
      throws PivotalClientApiException {
    throw new PivotalClientApiException("PCF operations not supported by this API.");
  }

  @Override
  public List<String> getRouteMaps(PcfRequestConfig pcfRequestConfig) throws PivotalClientApiException {
    throw new PivotalClientApiException("PCF operations not supported by this API.");
  }

  @Override
  public String checkConnectivity(
      PcfConfig pcfConfig, boolean limitPcfThreads, boolean ignorePcfConnectionContextCache) {
    return "FAILED: connection timed out";
  }

  @Override
  public String createRouteMap(PcfRequestConfig pcfRequestConfig, String host, String domain, String path,
      boolean tcpRoute, boolean useRandomPort, Integer port) throws PivotalClientApiException, InterruptedException {
    throw new PivotalClientApiException("PCF operations not supported by this API.");
  }

  @Override
  public void performConfigureAutoscalar(PcfAppAutoscalarRequestData appAutoscalarRequestData,
      ExecutionLogCallback executionLogCallback) throws PivotalClientApiException {
    throw new PivotalClientApiException("PCF operations not supported by this API.");
  }

  @Override
  public boolean changeAutoscalarState(PcfAppAutoscalarRequestData appAutoscalarRequestData,
      ExecutionLogCallback executionLogCallback, boolean enable) throws PivotalClientApiException {
    throw new PivotalClientApiException("PCF operations not supported by this API.");
  }

  @Override
  public boolean checkIfAppAutoscalarInstalled() throws PivotalClientApiException {
    throw new PivotalClientApiException("PCF operations not supported by this API.");
  }

  @Override
  public boolean checkIfAppHasAutoscalarAttached(PcfAppAutoscalarRequestData appAutoscalarRequestData,
      ExecutionLogCallback executionLogCallback) throws PivotalClientApiException {
    throw new PivotalClientApiException("PCF operations not supported by this API.");
  }

  @Override
  public String resolvePcfPluginHome() {
    return null;
  }

  @Override
  public ApplicationDetail upsizeApplicationWithSteadyStateCheck(
      PcfRequestConfig pcfRequestConfig, ExecutionLogCallback executionLogCallback) throws PivotalClientApiException {
    throw new PivotalClientApiException("PCF operations not supported by this API.");
  }

  @Override
  public boolean isActiveApplication(PcfRequestConfig pcfRequestConfig, ExecutionLogCallback executionLogCallback)
      throws PivotalClientApiException {
    throw new PivotalClientApiException("PCF operations not supported by this API.");
  }

  @Override
  public void setEnvironmentVariableForAppStatus(PcfRequestConfig pcfRequestConfig, boolean activeStatus,
      ExecutionLogCallback executionLogCallback) throws PivotalClientApiException {
    throw new PivotalClientApiException("PCF operations not supported by this API.");
  }

  @Override
  public void unsetEnvironmentVariableForAppStatus(
      PcfRequestConfig pcfRequestConfig, ExecutionLogCallback executionLogCallback) throws PivotalClientApiException {
    throw new PivotalClientApiException("PCF operations not supported by this API.");
  }
}
