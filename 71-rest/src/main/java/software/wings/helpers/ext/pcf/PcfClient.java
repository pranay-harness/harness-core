package software.wings.helpers.ext.pcf;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.doppler.LogMessage;
import org.cloudfoundry.operations.applications.ApplicationDetail;
import org.cloudfoundry.operations.applications.ApplicationEnvironments;
import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.cloudfoundry.operations.domains.Domain;
import org.cloudfoundry.operations.organizations.OrganizationSummary;
import org.cloudfoundry.operations.routes.Route;
import org.cloudfoundry.reactor.ConnectionContext;
import org.zeroturnaround.exec.StartedProcess;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.helpers.ext.pcf.request.PcfAppAutoscalarRequestData;
import software.wings.helpers.ext.pcf.request.PcfCreateApplicationRequestData;
import software.wings.helpers.ext.pcf.request.PcfRunPluginScriptRequestData;

import java.util.List;
import java.util.Map;
import java.util.Optional;
public interface PcfClient {
  CloudFoundryClient getCloudFoundryClient(PcfRequestConfig pcfRequestConfig, ConnectionContext connectionContext)
      throws PivotalClientApiException;
  List<OrganizationSummary> getOrganizations(PcfRequestConfig pcfRequestConfig)
      throws PivotalClientApiException, InterruptedException;
  List<String> getSpacesForOrganization(PcfRequestConfig pcfRequestConfig)
      throws PivotalClientApiException, InterruptedException;
  List<ApplicationSummary> getApplications(PcfRequestConfig pcfRequestConfig)
      throws PivotalClientApiException, InterruptedException;
  ApplicationDetail getApplicationByName(PcfRequestConfig pcfRequestConfige)
      throws PivotalClientApiException, InterruptedException;

  void pushApplicationUsingManifest(PcfCreateApplicationRequestData requestData,
      ExecutionLogCallback executionLogCallback) throws PivotalClientApiException, InterruptedException;

  void stopApplication(PcfRequestConfig pcfRequestConfig) throws PivotalClientApiException, InterruptedException;
  void createRouteMap(PcfRequestConfig pcfRequestConfig, String host, String domain, String path, boolean tcpRoute,
      boolean useRandomPort, Integer port) throws PivotalClientApiException, InterruptedException;
  void deleteApplication(PcfRequestConfig pcfRequestConfig) throws PivotalClientApiException, InterruptedException;
  void scaleApplications(PcfRequestConfig pcfRequestConfig) throws PivotalClientApiException, InterruptedException;
  void unmapRoutesForApplication(PcfRequestConfig pcfRequestConfig, List<String> routes)
      throws PivotalClientApiException, InterruptedException;
  void unmapRoutesForApplicationUsingCli(PcfRequestConfig pcfRequestConfig, List<String> routes,
      ExecutionLogCallback logCallback) throws PivotalClientApiException, InterruptedException;
  void unmapRouteMapForApp(PcfRequestConfig pcfRequestConfig, Route route)
      throws PivotalClientApiException, InterruptedException;
  void mapRoutesForApplication(PcfRequestConfig pcfRequestConfig, List<String> routes)
      throws PivotalClientApiException, InterruptedException;
  void mapRoutesForApplicationUsingCli(PcfRequestConfig pcfRequestConfig, List<String> routes,
      ExecutionLogCallback logCallback) throws PivotalClientApiException, InterruptedException;
  void mapRouteMapForApp(PcfRequestConfig pcfRequestConfig, Route route)
      throws PivotalClientApiException, InterruptedException;
  List<Route> getRouteMapsByNames(List<String> paths, PcfRequestConfig pcfRequestConfig)
      throws PivotalClientApiException, InterruptedException;
  void getTasks(PcfRequestConfig pcfRequestConfig) throws PivotalClientApiException, InterruptedException;
  void startApplication(PcfRequestConfig pcfRequestConfig) throws PivotalClientApiException, InterruptedException;
  List<String> getRoutesForSpace(PcfRequestConfig pcfRequestConfig)
      throws PivotalClientApiException, InterruptedException;
  Optional<Route> getRouteMap(PcfRequestConfig pcfRequestConfig, String route)
      throws PivotalClientApiException, InterruptedException;

  void runPcfPluginScript(PcfRunPluginScriptRequestData pcfRunPluginScriptRequestData,
      ExecutionLogCallback executionLogCallback) throws PivotalClientApiException;
  List<Domain> getAllDomainsForSpace(PcfRequestConfig pcfRequestConfig)
      throws PivotalClientApiException, InterruptedException;
  void performConfigureAutoscalar(PcfAppAutoscalarRequestData appAutoscalarRequestData,
      ExecutionLogCallback executionLogCallback) throws PivotalClientApiException;
  void changeAutoscalarState(PcfAppAutoscalarRequestData appAutoscalarRequestData,
      ExecutionLogCallback executionLogCallback, boolean enable) throws PivotalClientApiException;
  boolean checkIfAppAutoscalarInstalled() throws PivotalClientApiException;
  boolean checkIfAppHasAutoscalarAttached(PcfAppAutoscalarRequestData appAutoscalarRequestData,
      ExecutionLogCallback executionLogCallback) throws PivotalClientApiException;
  boolean checkIfAppHasAutoscalarWithExpectedState(PcfAppAutoscalarRequestData appAutoscalarRequestData,
      ExecutionLogCallback logCallback) throws PivotalClientApiException;
  String resolvePcfPluginHome();

  List<LogMessage> getRecentLogs(PcfRequestConfig pcfRequestConfig, long logsAfterTsNs)
      throws PivotalClientApiException;
  StartedProcess tailLogsForPcf(PcfRequestConfig pcfRequestConfig, ExecutionLogCallback executionLogCallback)
      throws PivotalClientApiException;

  void setEnvVariablesForApplication(Map<String, Object> envVars, PcfRequestConfig pcfRequestConfig,
      ExecutionLogCallback logCallback) throws PivotalClientApiException;
  void unsetEnvVariablesForApplication(List<String> varNames, PcfRequestConfig pcfRequestConfig,
      ExecutionLogCallback logCallback) throws PivotalClientApiException;
  ApplicationEnvironments getApplicationEnvironmentsByName(PcfRequestConfig pcfRequestConfig)
      throws PivotalClientApiException;
}