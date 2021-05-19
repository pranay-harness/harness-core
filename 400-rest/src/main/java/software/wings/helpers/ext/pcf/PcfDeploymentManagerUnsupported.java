package software.wings.helpers.ext.pcf;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pcf.PivotalClientApiException;
import io.harness.pcf.model.CfAppAutoscalarRequestData;
import io.harness.pcf.model.CfCreateApplicationRequestData;
import io.harness.pcf.model.CfRequestConfig;

import software.wings.beans.PcfConfig;
import software.wings.beans.command.ExecutionLogCallback;

import com.google.inject.Singleton;
import java.util.List;
import org.cloudfoundry.operations.applications.ApplicationDetail;
import org.cloudfoundry.operations.applications.ApplicationSummary;

@Singleton
@OwnedBy(CDP)
public class PcfDeploymentManagerUnsupported implements PcfDeploymentManager {
  public static final String DELIMITER = "__";

  @Override
  public List<String> getOrganizations(CfRequestConfig cfRequestConfig) throws PivotalClientApiException {
    throw new PivotalClientApiException("PCF operations not supported.");
  }

  @Override
  public List<String> getSpacesForOrganization(CfRequestConfig cfRequestConfig) throws PivotalClientApiException {
    throw new PivotalClientApiException("PCF operations not supported by this API.");
  }

  @Override
  public ApplicationDetail createApplication(CfCreateApplicationRequestData requestData,
      ExecutionLogCallback executionLogCallback) throws PivotalClientApiException {
    throw new PivotalClientApiException("PCF operations not supported by this API.");
  }

  @Override
  public ApplicationDetail resizeApplication(CfRequestConfig cfRequestConfig) throws PivotalClientApiException {
    throw new PivotalClientApiException("PCF operations not supported by this API.");
  }

  @Override
  public void deleteApplication(CfRequestConfig cfRequestConfig) throws PivotalClientApiException {
    throw new PivotalClientApiException("PCF operations not supported by this API.");
  }

  @Override
  public String stopApplication(CfRequestConfig cfRequestConfig) throws PivotalClientApiException {
    throw new PivotalClientApiException("PCF operations not supported by this API.");
  }

  @Override
  public ApplicationDetail getApplicationByName(CfRequestConfig cfRequestConfig) throws PivotalClientApiException {
    throw new PivotalClientApiException("PCF operations not supported by this API.");
  }

  @Override
  public void unmapRouteMapForApplication(CfRequestConfig cfRequestConfig, List<String> paths,
      ExecutionLogCallback logCallback) throws PivotalClientApiException {
    throw new PivotalClientApiException("PCF operations not supported by this API.");
  }

  @Override
  public void mapRouteMapForApplication(CfRequestConfig cfRequestConfig, List<String> paths,
      ExecutionLogCallback logCallback) throws PivotalClientApiException {
    throw new PivotalClientApiException("PCF operations not supported by this API.");
  }

  @Override
  public List<ApplicationSummary> getDeployedServicesWithNonZeroInstances(
      CfRequestConfig cfRequestConfig, String prefix) throws PivotalClientApiException {
    throw new PivotalClientApiException("PCF operations not supported by this API.");
  }

  @Override
  public List<ApplicationSummary> getPreviousReleases(CfRequestConfig cfRequestConfig, String prefix)
      throws PivotalClientApiException {
    throw new PivotalClientApiException("PCF operations not supported by this API.");
  }

  @Override
  public List<String> getRouteMaps(CfRequestConfig cfRequestConfig) throws PivotalClientApiException {
    throw new PivotalClientApiException("PCF operations not supported by this API.");
  }

  @Override
  public String checkConnectivity(
      PcfConfig pcfConfig, boolean limitPcfThreads, boolean ignorePcfConnectionContextCache) {
    return "FAILED: connection timed out";
  }

  @Override
  public String createRouteMap(CfRequestConfig cfRequestConfig, String host, String domain, String path,
      boolean tcpRoute, boolean useRandomPort, Integer port) throws PivotalClientApiException, InterruptedException {
    throw new PivotalClientApiException("PCF operations not supported by this API.");
  }

  @Override
  public void performConfigureAutoscalar(CfAppAutoscalarRequestData appAutoscalarRequestData,
      ExecutionLogCallback executionLogCallback) throws PivotalClientApiException {
    throw new PivotalClientApiException("PCF operations not supported by this API.");
  }

  @Override
  public boolean changeAutoscalarState(CfAppAutoscalarRequestData appAutoscalarRequestData,
      ExecutionLogCallback executionLogCallback, boolean enable) throws PivotalClientApiException {
    throw new PivotalClientApiException("PCF operations not supported by this API.");
  }

  @Override
  public boolean checkIfAppHasAutoscalarAttached(CfAppAutoscalarRequestData appAutoscalarRequestData,
      ExecutionLogCallback executionLogCallback) throws PivotalClientApiException {
    throw new PivotalClientApiException("PCF operations not supported by this API.");
  }

  @Override
  public ApplicationDetail upsizeApplicationWithSteadyStateCheck(
      CfRequestConfig cfRequestConfig, ExecutionLogCallback executionLogCallback) throws PivotalClientApiException {
    throw new PivotalClientApiException("PCF operations not supported by this API.");
  }

  @Override
  public boolean isActiveApplication(CfRequestConfig cfRequestConfig, ExecutionLogCallback executionLogCallback)
      throws PivotalClientApiException {
    throw new PivotalClientApiException("PCF operations not supported by this API.");
  }

  @Override
  public void setEnvironmentVariableForAppStatus(CfRequestConfig cfRequestConfig, boolean activeStatus,
      ExecutionLogCallback executionLogCallback) throws PivotalClientApiException {
    throw new PivotalClientApiException("PCF operations not supported by this API.");
  }

  @Override
  public void unsetEnvironmentVariableForAppStatus(
      CfRequestConfig cfRequestConfig, ExecutionLogCallback executionLogCallback) throws PivotalClientApiException {
    throw new PivotalClientApiException("PCF operations not supported by this API.");
  }
}
