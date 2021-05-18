package io.harness.pcf;

import io.harness.pcf.model.PcfRequestConfig;

import software.wings.beans.command.ExecutionLogCallback;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.cloudfoundry.doppler.LogMessage;
import org.cloudfoundry.operations.applications.ApplicationDetail;
import org.cloudfoundry.operations.applications.ApplicationEnvironments;
import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.cloudfoundry.operations.domains.Domain;
import org.cloudfoundry.operations.organizations.OrganizationSummary;
import org.cloudfoundry.operations.routes.Route;

public interface CfSdkClient {
  /**
   * Get organizations.
   *
   * @param pcfRequestConfig PcfRequestConfig
   * @return List<OrganizationSummary>
   * @throws PivotalClientApiException
   * @throws InterruptedException
   */
  List<OrganizationSummary> getOrganizations(PcfRequestConfig pcfRequestConfig)
      throws PivotalClientApiException, InterruptedException;

  /**
   * Get space for organization.
   *
   * @param pcfRequestConfig PcfRequestConfig
   * @return List<String>
   * @throws PivotalClientApiException
   * @throws InterruptedException
   */
  List<String> getSpacesForOrganization(PcfRequestConfig pcfRequestConfig)
      throws PivotalClientApiException, InterruptedException;

  /**
   * Get applications.
   *
   * @param pcfRequestConfig PcfRequestConfig
   * @return List<ApplicationSummary>
   * @throws PivotalClientApiException
   * @throws InterruptedException
   */
  List<ApplicationSummary> getApplications(PcfRequestConfig pcfRequestConfig)
      throws PivotalClientApiException, InterruptedException;

  /**
   * Get application by name.
   *
   * @param pcfRequestConfig PcfRequestConfig
   * @return ApplicationDetail
   * @throws PivotalClientApiException
   * @throws InterruptedException
   */
  ApplicationDetail getApplicationByName(PcfRequestConfig pcfRequestConfig)
      throws PivotalClientApiException, InterruptedException;

  /**
   * Start applications.
   *
   * @param pcfRequestConfig
   * @throws PivotalClientApiException
   * @throws InterruptedException
   */
  void startApplication(PcfRequestConfig pcfRequestConfig) throws PivotalClientApiException, InterruptedException;

  /**
   * Scale application.
   *
   * @param pcfRequestConfig
   * @throws PivotalClientApiException
   * @throws InterruptedException
   */
  void scaleApplications(PcfRequestConfig pcfRequestConfig) throws PivotalClientApiException, InterruptedException;

  /**
   * Stop application.
   *
   * @param pcfRequestConfig
   * @throws PivotalClientApiException
   * @throws InterruptedException
   */
  void stopApplication(PcfRequestConfig pcfRequestConfig) throws PivotalClientApiException, InterruptedException;

  /**
   * Delete application.
   *
   * @param pcfRequestConfig
   * @throws PivotalClientApiException
   * @throws InterruptedException
   */
  void deleteApplication(PcfRequestConfig pcfRequestConfig) throws PivotalClientApiException, InterruptedException;

  /**
   * Push application.
   *
   * @param pcfRequestConfig
   * @param path Path
   * @param executionLogCallback
   * @throws PivotalClientApiException
   * @throws InterruptedException
   */
  void pushAppBySdk(PcfRequestConfig pcfRequestConfig, Path path, ExecutionLogCallback executionLogCallback)
      throws PivotalClientApiException, InterruptedException;

  /**
   * Create route.
   *
   * @param pcfRequestConfig
   * @param host
   * @param domain
   * @param path
   * @param tcpRoute
   * @param useRandomPort
   * @param port
   * @throws PivotalClientApiException
   * @throws InterruptedException
   */
  void createRouteMap(PcfRequestConfig pcfRequestConfig, String host, String domain, String path, boolean tcpRoute,
      boolean useRandomPort, Integer port) throws PivotalClientApiException, InterruptedException;

  /**
   * Unmap route.
   *
   * @param pcfRequestConfig
   * @param route
   * @throws PivotalClientApiException
   * @throws InterruptedException
   */
  void unmapRouteMapForApp(PcfRequestConfig pcfRequestConfig, Route route)
      throws PivotalClientApiException, InterruptedException;

  /**
   * Map routes.
   *
   * @param pcfRequestConfig
   * @param routes
   * @throws PivotalClientApiException
   * @throws InterruptedException
   */
  void mapRoutesForApplication(PcfRequestConfig pcfRequestConfig, List<String> routes)
      throws PivotalClientApiException, InterruptedException;

  /**
   * Map route.
   *
   * @param pcfRequestConfig
   * @param route
   * @throws PivotalClientApiException
   * @throws InterruptedException
   */
  void mapRouteMapForApp(PcfRequestConfig pcfRequestConfig, Route route)
      throws PivotalClientApiException, InterruptedException;

  /**
   * Unmap route.
   *
   * @param pcfRequestConfig
   * @param routes
   * @throws PivotalClientApiException
   * @throws InterruptedException
   */
  void unmapRoutesForApplication(PcfRequestConfig pcfRequestConfig, List<String> routes)
      throws PivotalClientApiException, InterruptedException;

  /**
   * Get route.
   *
   * @param pcfRequestConfig
   * @param route
   * @return
   * @throws PivotalClientApiException
   * @throws InterruptedException
   */
  Optional<Route> getRouteMap(PcfRequestConfig pcfRequestConfig, String route)
      throws PivotalClientApiException, InterruptedException;

  /**
   * Get route.
   *
   * @param paths
   * @param pcfRequestConfig
   * @return
   * @throws PivotalClientApiException
   * @throws InterruptedException
   */
  List<Route> getRouteMapsByNames(List<String> paths, PcfRequestConfig pcfRequestConfig)
      throws PivotalClientApiException, InterruptedException;

  /**
   * Get routes for space.
   *
   * @param pcfRequestConfig
   * @return
   * @throws PivotalClientApiException
   * @throws InterruptedException
   */
  List<String> getRoutesForSpace(PcfRequestConfig pcfRequestConfig)
      throws PivotalClientApiException, InterruptedException;

  /**
   *
   *
   * @param pcfRequestConfig
   * @param logsAfterTsNs
   * @return
   * @throws PivotalClientApiException
   */
  List<LogMessage> getRecentLogs(PcfRequestConfig pcfRequestConfig, long logsAfterTsNs)
      throws PivotalClientApiException;

  /**
   * Get application environments by name.
   *
   * @param pcfRequestConfig
   * @return
   * @throws PivotalClientApiException
   */
  ApplicationEnvironments getApplicationEnvironmentsByName(PcfRequestConfig pcfRequestConfig)
      throws PivotalClientApiException;

  /**
   * Get tasks.
   *
   * @param pcfRequestConfig
   * @throws PivotalClientApiException
   * @throws InterruptedException
   */
  void getTasks(PcfRequestConfig pcfRequestConfig) throws PivotalClientApiException, InterruptedException;

  /**
   * Get all space domains.
   *
   * @param pcfRequestConfig
   * @return
   * @throws PivotalClientApiException
   * @throws InterruptedException
   */
  List<Domain> getAllDomainsForSpace(PcfRequestConfig pcfRequestConfig)
      throws PivotalClientApiException, InterruptedException;
}
