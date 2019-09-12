package software.wings.helpers.ext.pcf;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.operations.applications.ApplicationDetail;
import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.cloudfoundry.operations.organizations.OrganizationSummary;
import org.cloudfoundry.operations.routes.Route;
import software.wings.beans.command.ExecutionLogCallback;

import java.util.List;
import java.util.Optional;

public interface PcfClient {
  CloudFoundryClient getCloudFoundryClient(PcfRequestConfig pcfRequestConfig) throws PivotalClientApiException;

  List<OrganizationSummary> getOrganizations(PcfRequestConfig pcfRequestConfig)
      throws PivotalClientApiException, InterruptedException;

  List<String> getSpacesForOrganization(PcfRequestConfig pcfRequestConfig)
      throws PivotalClientApiException, InterruptedException;

  List<ApplicationSummary> getApplications(PcfRequestConfig pcfRequestConfig)
      throws PivotalClientApiException, InterruptedException;

  ApplicationDetail getApplicationByName(PcfRequestConfig pcfRequestConfige)
      throws PivotalClientApiException, InterruptedException;

  void pushApplicationUsingManifest(PcfRequestConfig pcfRequestConfigs, String filePath, String configPathVar,
      ExecutionLogCallback executionLogCallback) throws PivotalClientApiException, InterruptedException;

  void stopApplication(PcfRequestConfig pcfRequestConfig) throws PivotalClientApiException, InterruptedException;

  void createRouteMap(PcfRequestConfig pcfRequestConfig, String host, String domain, String path, boolean tcpRoute,
      boolean useRandomPort, Integer port) throws PivotalClientApiException, InterruptedException;

  void deleteApplication(PcfRequestConfig pcfRequestConfig) throws PivotalClientApiException, InterruptedException;

  void scaleApplications(PcfRequestConfig pcfRequestConfig) throws PivotalClientApiException, InterruptedException;

  void unmapRoutesForApplication(PcfRequestConfig pcfRequestConfig, List<String> routes)
      throws PivotalClientApiException, InterruptedException;

  void unmapRouteMapForApp(PcfRequestConfig pcfRequestConfig, Route route)
      throws PivotalClientApiException, InterruptedException;

  void mapRoutesForApplication(PcfRequestConfig pcfRequestConfig, List<String> routes)
      throws PivotalClientApiException, InterruptedException;

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
}
