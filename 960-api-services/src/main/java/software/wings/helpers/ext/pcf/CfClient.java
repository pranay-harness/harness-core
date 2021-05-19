package software.wings.helpers.ext.pcf;

import io.harness.logging.LogCallback;
import io.harness.pcf.PivotalClientApiException;
import io.harness.pcf.model.CfAppAutoscalarRequestData;
import io.harness.pcf.model.CfCreateApplicationRequestData;
import io.harness.pcf.model.CfRequestConfig;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.cloudfoundry.operations.applications.ApplicationDetail;
import org.cloudfoundry.operations.applications.ApplicationEnvironments;
import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.cloudfoundry.operations.organizations.OrganizationSummary;
import org.cloudfoundry.operations.routes.Route;
import org.zeroturnaround.exec.StartedProcess;

public interface CfClient {
  List<OrganizationSummary> getOrganizations(CfRequestConfig cfRequestConfig);

  List<String> getSpacesForOrganization(CfRequestConfig cfRequestConfig);

  void pushApplicationUsingManifest(CfCreateApplicationRequestData requestData, LogCallback logCallback);

  ApplicationDetail getApplicationByName(CfRequestConfig cfRequestConfig);

  void scaleApplications(CfRequestConfig cfRequestConfig);

  void startApplication(CfRequestConfig cfRequestConfig);

  void stopApplication(CfRequestConfig cfRequestConfig);

  void deleteApplication(CfRequestConfig cfRequestConfig);

  void unmapRoutesForApplicationUsingCli(CfRequestConfig cfRequestConfig, List<String> paths, LogCallback logCallback);

  void unmapRoutesForApplication(CfRequestConfig cfRequestConfig, List<String> paths);

  void mapRoutesForApplicationUsingCli(CfRequestConfig cfRequestConfig, List<String> paths, LogCallback logCallback);

  void mapRoutesForApplication(CfRequestConfig cfRequestConfig, List<String> paths);

  List<ApplicationSummary> getApplications(CfRequestConfig cfRequestConfig);

  List<String> getRoutesForSpace(CfRequestConfig pcfRequestConfig);

  void createRouteMap(CfRequestConfig cfRequestConfig, String host, String domain, String path, boolean tcpRoute,
      boolean useRandomPort, Integer port);

  Optional<Route> getRouteMap(CfRequestConfig pcfRequestConfig, String routePath);

  boolean checkIfAppHasAutoscalarAttached(
      io.harness.pcf.model.CfAppAutoscalarRequestData appAutoscalarRequestData, LogCallback logCallback);

  void performConfigureAutoscalar(
      io.harness.pcf.model.CfAppAutoscalarRequestData appAutoscalarRequestData, LogCallback logCallback);

  boolean checkIfAppHasAutoscalarWithExpectedState(
      io.harness.pcf.model.CfAppAutoscalarRequestData appAutoscalarRequestData, LogCallback logCallback);

  void changeAutoscalarState(
      CfAppAutoscalarRequestData appAutoscalarRequestData, LogCallback logCallback, boolean enable);

  StartedProcess tailLogsForPcf(CfRequestConfig cfRequestConfig, LogCallback logCallback)
      throws PivotalClientApiException;

  ApplicationEnvironments getApplicationEnvironmentsByName(CfRequestConfig pcfRequestConfig);

  void setEnvVariablesForApplication(Map<String, String> map, CfRequestConfig cfRequestConfig, LogCallback logCallback);

  void unsetEnvVariablesForApplication(List<String> map, CfRequestConfig cfRequestConfig, LogCallback logCallback);
}
