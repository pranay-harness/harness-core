package io.harness.pcf.cfsdk;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.pcf.model.PcfConstants.PCF_ROUTE_PATH_SEPARATOR;
import static io.harness.pcf.model.PcfConstants.PIVOTAL_CLOUD_FOUNDRY_CLIENT_EXCEPTION;
import static io.harness.pcf.model.PcfConstants.PIVOTAL_CLOUD_FOUNDRY_LOG_PREFIX;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.EMPTY;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pcf.CfSdkClient;
import io.harness.pcf.PivotalClientApiException;
import io.harness.pcf.model.PcfRequestConfig;

import software.wings.beans.command.ExecutionLogCallback;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.cloudfoundry.doppler.LogMessage;
import org.cloudfoundry.operations.applications.ApplicationDetail;
import org.cloudfoundry.operations.applications.ApplicationEnvironments;
import org.cloudfoundry.operations.applications.ApplicationManifest;
import org.cloudfoundry.operations.applications.ApplicationManifestUtils;
import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.cloudfoundry.operations.applications.DeleteApplicationRequest;
import org.cloudfoundry.operations.applications.GetApplicationEnvironmentsRequest;
import org.cloudfoundry.operations.applications.GetApplicationRequest;
import org.cloudfoundry.operations.applications.ListApplicationTasksRequest;
import org.cloudfoundry.operations.applications.LogsRequest;
import org.cloudfoundry.operations.applications.PushApplicationManifestRequest;
import org.cloudfoundry.operations.applications.ScaleApplicationRequest;
import org.cloudfoundry.operations.applications.StartApplicationRequest;
import org.cloudfoundry.operations.applications.StopApplicationRequest;
import org.cloudfoundry.operations.applications.Task;
import org.cloudfoundry.operations.domains.Domain;
import org.cloudfoundry.operations.organizations.OrganizationDetail;
import org.cloudfoundry.operations.organizations.OrganizationInfoRequest;
import org.cloudfoundry.operations.organizations.OrganizationSummary;
import org.cloudfoundry.operations.routes.CreateRouteRequest;
import org.cloudfoundry.operations.routes.Level;
import org.cloudfoundry.operations.routes.ListRoutesRequest;
import org.cloudfoundry.operations.routes.MapRouteRequest;
import org.cloudfoundry.operations.routes.Route;
import org.cloudfoundry.operations.routes.UnmapRouteRequest;

@Singleton
@Slf4j
@OwnedBy(CDP)
public class CfSdkClientImpl implements CfSdkClient {
  public static final String BIN_BASH = "/bin/bash";
  public static final String SUCCESS = "SUCCESS";
  public static final String PCF_PROXY_PROPERTY = "https_proxy";

  @Inject private CloudFoundryOperationsProvider cloudFoundryOperationsProvider;

  @Override
  public List<OrganizationSummary> getOrganizations(PcfRequestConfig pcfRequestConfig)
      throws PivotalClientApiException, InterruptedException {
    log.info(format("%s Fetching Organizations", PIVOTAL_CLOUD_FOUNDRY_LOG_PREFIX));

    List<OrganizationSummary> organizations = new ArrayList<>();

    AtomicBoolean exceptionOccurred = new AtomicBoolean(false);
    StringBuilder errorBuilder = new StringBuilder();
    CountDownLatch latch = new CountDownLatch(1);
    try (CloudFoundryOperationsWrapper operationsWrapper =
             cloudFoundryOperationsProvider.getCloudFoundryOperationsWrapper(pcfRequestConfig)) {
      operationsWrapper.getCloudFoundryOperations().organizations().list().subscribe(organizations::add, throwable -> {
        exceptionOccurred.set(true);
        handleExceptionForGetOraganizationsAPI(throwable, "getOrganizations", errorBuilder);
        latch.countDown();
      }, latch::countDown);

      waitTillCompletion(latch, pcfRequestConfig.getTimeOutIntervalInMins());
      if (exceptionOccurred.get()) {
        throw new PivotalClientApiException(
            format("Exception occurred while fetching Organizations, Error: %s", errorBuilder.toString()));
      }
      return organizations;
    }
  }

  @Override
  public List<String> getSpacesForOrganization(PcfRequestConfig pcfRequestConfig)
      throws PivotalClientApiException, InterruptedException {
    List<OrganizationDetail> organizationDetails = new ArrayList<>();
    List<String> spaces = new ArrayList<>();
    log.info(format("%s Fetching Spaces", PIVOTAL_CLOUD_FOUNDRY_LOG_PREFIX));

    CountDownLatch latch = new CountDownLatch(1);

    AtomicBoolean exceptionOccurred = new AtomicBoolean(false);
    StringBuilder errorBuilder = new StringBuilder();
    try (CloudFoundryOperationsWrapper operationsWrapper =
             cloudFoundryOperationsProvider.getCloudFoundryOperationsWrapper(pcfRequestConfig)) {
      operationsWrapper.getCloudFoundryOperations()
          .organizations()
          .get(OrganizationInfoRequest.builder().name(pcfRequestConfig.getOrgName()).build())
          .subscribe(organizationDetails::add, throwable -> {
            exceptionOccurred.set(true);
            handleException(throwable, "getSpacesForOrganization", errorBuilder);
            latch.countDown();
          }, latch::countDown);

      waitTillCompletion(latch, pcfRequestConfig.getTimeOutIntervalInMins());

      if (exceptionOccurred.get()) {
        throw new PivotalClientApiException(
            format("Exception occurred while fetching Spaces, Error: %s", errorBuilder.toString()));
      }

      if (!CollectionUtils.isEmpty(organizationDetails)) {
        return organizationDetails.stream()
            .flatMap(organizationDetail -> organizationDetail.getSpaces().stream())
            .collect(toList());
      }
      return spaces;
    }
  }

  @Override
  public List<ApplicationSummary> getApplications(PcfRequestConfig pcfRequestConfig)
      throws PivotalClientApiException, InterruptedException {
    log.info(format("%s Fetching PCF Applications", PIVOTAL_CLOUD_FOUNDRY_LOG_PREFIX));

    CountDownLatch latch = new CountDownLatch(1);
    List<ApplicationSummary> applicationSummaries = new ArrayList<>();

    AtomicBoolean exceptionOccured = new AtomicBoolean(false);
    StringBuilder errorBuilder = new StringBuilder();
    try (CloudFoundryOperationsWrapper operationsWrapper =
             cloudFoundryOperationsProvider.getCloudFoundryOperationsWrapper(pcfRequestConfig)) {
      operationsWrapper.getCloudFoundryOperations().applications().list().subscribe(
          applicationSummaries::add, throwable -> {
            exceptionOccured.set(true);
            handleException(throwable, "getApplications", errorBuilder);
            latch.countDown();
          }, latch::countDown);

      waitTillCompletion(latch, pcfRequestConfig.getTimeOutIntervalInMins());

      if (exceptionOccured.get()) {
        throw new PivotalClientApiException(
            format("Exception occurred while fetching Applications, Error: %s", errorBuilder.toString()));
      }
      return applicationSummaries;
    }
  }

  @Override
  public ApplicationDetail getApplicationByName(PcfRequestConfig pcfRequestConfig)
      throws PivotalClientApiException, InterruptedException {
    log.info(
        format("%s Getting application: %s", PIVOTAL_CLOUD_FOUNDRY_LOG_PREFIX, pcfRequestConfig.getApplicationName()));

    CountDownLatch latch = new CountDownLatch(1);

    AtomicBoolean exceptionOccurred = new AtomicBoolean(false);
    StringBuilder errorBuilder = new StringBuilder();
    List<ApplicationDetail> applicationDetails = new ArrayList<>();
    try (CloudFoundryOperationsWrapper operationsWrapper =
             cloudFoundryOperationsProvider.getCloudFoundryOperationsWrapper(pcfRequestConfig)) {
      operationsWrapper.getCloudFoundryOperations()
          .applications()
          .get(GetApplicationRequest.builder().name(pcfRequestConfig.getApplicationName()).build())
          .subscribe(applicationDetails::add, throwable -> {
            exceptionOccurred.set(true);
            handleException(throwable, "getApplicationByName", errorBuilder);
            latch.countDown();
          }, latch::countDown);

      waitTillCompletion(latch, pcfRequestConfig.getTimeOutIntervalInMins());
      if (exceptionOccurred.get()) {
        throw new PivotalClientApiException(format("Exception occurred while getting application: %s, Error: %s",
            pcfRequestConfig.getApplicationName(), errorBuilder.toString()));
      }
      return isNotEmpty(applicationDetails) ? applicationDetails.get(0) : null;
    }
  }

  @Override
  public void startApplication(PcfRequestConfig pcfRequestConfig)
      throws PivotalClientApiException, InterruptedException {
    log.info(
        format("%s Starting application: %s", PIVOTAL_CLOUD_FOUNDRY_LOG_PREFIX, pcfRequestConfig.getApplicationName()));

    CountDownLatch latch = new CountDownLatch(1);

    AtomicBoolean exceptionOccurred = new AtomicBoolean(false);
    StringBuilder errorBuilder = new StringBuilder();
    try (CloudFoundryOperationsWrapper operationsWrapper =
             cloudFoundryOperationsProvider.getCloudFoundryOperationsWrapper(pcfRequestConfig)) {
      operationsWrapper.getCloudFoundryOperations()
          .applications()
          .start(StartApplicationRequest.builder().name(pcfRequestConfig.getApplicationName()).build())
          .subscribe(null, throwable -> {
            exceptionOccurred.set(true);
            handleException(throwable, "startApplication", errorBuilder);
            latch.countDown();
          }, latch::countDown);

      waitTillCompletion(latch, pcfRequestConfig.getTimeOutIntervalInMins());
      if (exceptionOccurred.get()) {
        throw new PivotalClientApiException(format("Exception occurred while starting application: %s, Error: %s",
            pcfRequestConfig.getApplicationName(), errorBuilder.toString()));
      }
    }
  }

  @Override
  public void scaleApplications(PcfRequestConfig pcfRequestConfig)
      throws PivotalClientApiException, InterruptedException {
    log.info(format("%s Scaling Applications: %s, to count: %s", PIVOTAL_CLOUD_FOUNDRY_LOG_PREFIX,
        pcfRequestConfig.getApplicationName(), pcfRequestConfig.getDesiredCount()));

    CountDownLatch latch = new CountDownLatch(1);

    AtomicBoolean exceptionOccurred = new AtomicBoolean(false);
    StringBuilder errorBuilder = new StringBuilder();
    try (CloudFoundryOperationsWrapper operationsWrapper =
             cloudFoundryOperationsProvider.getCloudFoundryOperationsWrapper(pcfRequestConfig)) {
      operationsWrapper.getCloudFoundryOperations()
          .applications()
          .scale(ScaleApplicationRequest.builder()
                     .name(pcfRequestConfig.getApplicationName())
                     .instances(pcfRequestConfig.getDesiredCount())
                     .build())
          .subscribe(null, throwable -> {
            exceptionOccurred.set(true);
            handleException(throwable, "scaleApplications", errorBuilder);
            latch.countDown();
          }, latch::countDown);

      waitTillCompletion(latch, pcfRequestConfig.getTimeOutIntervalInMins());
      if (exceptionOccurred.get()) {
        throw new PivotalClientApiException(
            format("Exception occurred Scaling Applications: %s, to count: %s, Error: %s",
                pcfRequestConfig.getApplicationName(), pcfRequestConfig.getDesiredCount(), errorBuilder.toString()));
      }
    }
  }

  @Override
  public void stopApplication(PcfRequestConfig pcfRequestConfig)
      throws PivotalClientApiException, InterruptedException {
    log.info(
        format("%s Stopping Application: %s", PIVOTAL_CLOUD_FOUNDRY_LOG_PREFIX, pcfRequestConfig.getApplicationName()));

    CountDownLatch latch = new CountDownLatch(1);
    AtomicBoolean exceptionOccurred = new AtomicBoolean(false);
    StringBuilder errorBuilder = new StringBuilder();
    try (CloudFoundryOperationsWrapper operationsWrapper =
             cloudFoundryOperationsProvider.getCloudFoundryOperationsWrapper(pcfRequestConfig)) {
      operationsWrapper.getCloudFoundryOperations()
          .applications()
          .stop(StopApplicationRequest.builder().name(pcfRequestConfig.getApplicationName()).build())
          .subscribe(null, throwable -> {
            exceptionOccurred.set(true);
            handleException(throwable, "stopApplication", errorBuilder);
            latch.countDown();
          }, latch::countDown);

      waitTillCompletion(latch, pcfRequestConfig.getTimeOutIntervalInMins());
      if (exceptionOccurred.get()) {
        throw new PivotalClientApiException(format("Exception occurred while stopping Application: %s, Error: %s",
            pcfRequestConfig.getApplicationName(), errorBuilder.toString()));
      }
    }
  }

  @Override
  public void deleteApplication(PcfRequestConfig pcfRequestConfig)
      throws PivotalClientApiException, InterruptedException {
    log.info(
        format("%s Deleting application: %s", PIVOTAL_CLOUD_FOUNDRY_LOG_PREFIX, pcfRequestConfig.getApplicationName()));

    StringBuilder errorBuilder = new StringBuilder();
    CountDownLatch latch = new CountDownLatch(1);
    AtomicBoolean exceptionOccured = new AtomicBoolean(false);
    try (CloudFoundryOperationsWrapper operationsWrapper =
             cloudFoundryOperationsProvider.getCloudFoundryOperationsWrapper(pcfRequestConfig)) {
      operationsWrapper.getCloudFoundryOperations()
          .applications()
          .delete(DeleteApplicationRequest.builder()
                      .name(pcfRequestConfig.getApplicationName())
                      .deleteRoutes(false)
                      .build())
          .subscribe(null, throwable -> {
            exceptionOccured.set(true);
            handleException(throwable, "deleteApplication", errorBuilder);
            latch.countDown();
          }, latch::countDown);

      waitTillCompletion(latch, pcfRequestConfig.getTimeOutIntervalInMins());
      if (exceptionOccured.get()) {
        throw new PivotalClientApiException(format("Exception occurred while deleting application: %s, Error: %s",
            pcfRequestConfig.getApplicationName(), errorBuilder.toString()));
      }
    }
  }

  @Override
  public void getTasks(PcfRequestConfig pcfRequestConfig) throws PivotalClientApiException, InterruptedException {
    log.info(new StringBuilder()
                 .append(PIVOTAL_CLOUD_FOUNDRY_LOG_PREFIX)
                 .append("Getting Tasks for Applications: ")
                 .append(pcfRequestConfig.getApplicationName())
                 .toString());

    CountDownLatch latch = new CountDownLatch(1);
    List<Task> tasks = new ArrayList<>();

    AtomicBoolean exceptionOccured = new AtomicBoolean(false);
    StringBuilder errorBuilder = new StringBuilder();
    try (CloudFoundryOperationsWrapper operationsWrapper =
             cloudFoundryOperationsProvider.getCloudFoundryOperationsWrapper(pcfRequestConfig)) {
      operationsWrapper.getCloudFoundryOperations()
          .applications()
          .listTasks(ListApplicationTasksRequest.builder().name(pcfRequestConfig.getApplicationName()).build())
          .subscribe(tasks::add, throwable -> {
            exceptionOccured.set(true);
            handleException(throwable, "getTasks", errorBuilder);
            latch.countDown();
          }, latch::countDown);

      waitTillCompletion(latch, pcfRequestConfig.getTimeOutIntervalInMins());

      if (exceptionOccured.get()) {
        throw new PivotalClientApiException(new StringBuilder()
                                                .append("Exception occurred while getting Tasks for Application: ")
                                                .append(pcfRequestConfig.getApplicationName())
                                                .append(", Error: " + errorBuilder.toString())
                                                .toString());
      }
    }
  }

  @Override
  public void pushAppBySdk(PcfRequestConfig pcfRequestConfig, Path path, ExecutionLogCallback executionLogCallback)
      throws PivotalClientApiException, InterruptedException {
    executionLogCallback.saveExecutionLog(
        "Using SDK to create application, Deprecated... Please enable flag: USE_PCF_CLI");

    List<ApplicationManifest> applicationManifests = ApplicationManifestUtils.read(path);

    ApplicationManifest applicationManifest = applicationManifests.get(0);
    applicationManifest = initializeApplicationManifest(applicationManifest, pcfRequestConfig);
    AtomicBoolean exceptionOccured = new AtomicBoolean(false);
    StringBuilder errorBuilder = new StringBuilder();
    CountDownLatch latch = new CountDownLatch(1);

    try (CloudFoundryOperationsWrapper operationsWrapper =
             cloudFoundryOperationsProvider.getCloudFoundryOperationsWrapper(pcfRequestConfig)) {
      operationsWrapper.getCloudFoundryOperations()
          .applications()
          .pushManifest(PushApplicationManifestRequest.builder().noStart(true).manifest(applicationManifest).build())
          .subscribe(null, throwable -> {
            exceptionOccured.set(true);
            handleException(throwable, "pushApplicationUsingManifest", errorBuilder);
            latch.countDown();
          }, latch::countDown);

      waitTillCompletion(latch, 10);

      if (exceptionOccured.get()) {
        throw new PivotalClientApiException(new StringBuilder()
                                                .append("Exceotion occured while creating Application: ")
                                                .append(pcfRequestConfig.getApplicationName())
                                                .append(", Error: " + errorBuilder.toString())
                                                .toString());
      }
    }
  }

  @Override
  public List<LogMessage> getRecentLogs(PcfRequestConfig pcfRequestConfig, long logsAfterTsNs)
      throws PivotalClientApiException {
    try (CloudFoundryOperationsWrapper operationsWrapper =
             cloudFoundryOperationsProvider.getCloudFoundryOperationsWrapper(pcfRequestConfig)) {
      return operationsWrapper.getCloudFoundryOperations()
          .applications()
          .logs(LogsRequest.builder().name(pcfRequestConfig.getApplicationName()).recent(true).build())
          .timeout(Duration.ofMinutes(
              pcfRequestConfig.getTimeOutIntervalInMins() > 0 ? pcfRequestConfig.getTimeOutIntervalInMins() : 10))
          .skipUntil(log -> log.getTimestamp() > logsAfterTsNs)
          .toStream()
          .collect(Collectors.toList());
    } catch (Exception e) {
      final StringBuilder errorBuilder = new StringBuilder();
      handleException(e, "getRecentLogs", errorBuilder);
      throw new PivotalClientApiException(new StringBuilder()
                                              .append("Exception occurred while getting recent logs for application: ")
                                              .append(pcfRequestConfig.getApplicationName())
                                              .append(", Error: " + errorBuilder.toString())
                                              .toString());
    }
  }

  String handlePwdForSpecialCharsForShell(String password) {
    // Wrapped around single quotes to by pass '$' characted.
    // Single quotes cannot be part of the password due to limitations on escaping them.
    return "'" + password + "'";
  }

  private ApplicationManifest initializeApplicationManifest(
      ApplicationManifest applicationManifest, PcfRequestConfig pcfRequestConfig) {
    ApplicationManifest.Builder builder = ApplicationManifest.builder();

    if (applicationManifest.getDomains() != null) {
      builder.addAllDomains(applicationManifest.getDomains());
    }

    if (applicationManifest.getHosts() != null) {
      builder.addAllHosts(applicationManifest.getHosts());
    }

    if (applicationManifest.getServices() != null) {
      builder.addAllServices(applicationManifest.getServices());
    }
    // use Random route if provided no route-map is provided
    addRouteMapsToManifest(pcfRequestConfig, builder);

    // Add user provided environment variables
    if (pcfRequestConfig.getServiceVariables() != null) {
      for (Map.Entry<String, String> entry : pcfRequestConfig.getServiceVariables().entrySet()) {
        builder.environmentVariable(entry.getKey(), entry.getValue());
      }
    }

    if (isNotEmpty(applicationManifest.getEnvironmentVariables())) {
      for (Map.Entry<String, Object> entry : applicationManifest.getEnvironmentVariables().entrySet()) {
        builder.environmentVariable(entry.getKey(), entry.getValue());
      }
    }

    return builder.buildpack(applicationManifest.getBuildpack())
        .command(applicationManifest.getCommand())
        .disk(applicationManifest.getDisk())
        .instances(applicationManifest.getInstances())
        .memory(applicationManifest.getMemory())
        .name(pcfRequestConfig.getApplicationName())
        .path(applicationManifest.getPath())
        .instances(0)
        .healthCheckType(applicationManifest.getHealthCheckType())
        .healthCheckHttpEndpoint(applicationManifest.getHealthCheckHttpEndpoint())
        .stack(applicationManifest.getStack())
        .timeout(applicationManifest.getTimeout())
        .domains(applicationManifest.getDomains())
        .build();
  }

  private void addRouteMapsToManifest(PcfRequestConfig pcfRequestConfig, ApplicationManifest.Builder builder) {
    // Set routeMaps
    if (isNotEmpty(pcfRequestConfig.getRouteMaps())) {
      List<org.cloudfoundry.operations.applications.Route> routeList =
          pcfRequestConfig.getRouteMaps()
              .stream()
              .map(routeMap -> org.cloudfoundry.operations.applications.Route.builder().route(routeMap).build())
              .collect(toList());
      builder.routes(routeList);
    } else {
      // In case no routeMap is given (Blue green deployment, let PCF create a route map)
      builder.randomRoute(true);
      String appName = pcfRequestConfig.getApplicationName();
      String appPrefix = appName.substring(0, appName.lastIndexOf("__"));

      // '_' in routemap is not allowed, PCF lets us create route but while accessing it, fails
      appPrefix = appPrefix.replaceAll("__", "-");
      appPrefix = appPrefix.replaceAll("_", "-");

      builder.host(appPrefix);
    }
  }

  @Override
  public ApplicationEnvironments getApplicationEnvironmentsByName(PcfRequestConfig pcfRequestConfig)
      throws PivotalClientApiException {
    log.info(new StringBuilder()
                 .append(PIVOTAL_CLOUD_FOUNDRY_LOG_PREFIX)
                 .append("Getting application: ")
                 .append(pcfRequestConfig.getApplicationName())
                 .toString());
    CountDownLatch latch = new CountDownLatch(1);

    AtomicBoolean exceptionOccured = new AtomicBoolean(false);
    StringBuilder errorBuilder = new StringBuilder();
    List<ApplicationEnvironments> applicationEnvironments = new ArrayList<>();
    try (CloudFoundryOperationsWrapper operationsWrapper =
             cloudFoundryOperationsProvider.getCloudFoundryOperationsWrapper(pcfRequestConfig)) {
      operationsWrapper.getCloudFoundryOperations()
          .applications()
          .getEnvironments(
              GetApplicationEnvironmentsRequest.builder().name(pcfRequestConfig.getApplicationName()).build())
          .subscribe(applicationEnvironments::add, throwable -> {
            exceptionOccured.set(true);
            handleException(throwable, "getApplicationEnvironmentsByName", errorBuilder);
            latch.countDown();
          }, latch::countDown);

      waitTillCompletion(latch, pcfRequestConfig.getTimeOutIntervalInMins());
      if (exceptionOccured.get()) {
        throw new PivotalClientApiException(new StringBuilder()
                                                .append("Exception occurred while  getting application Environments: ")
                                                .append(pcfRequestConfig.getApplicationName())
                                                .append(", Error: ")
                                                .append(errorBuilder.toString())
                                                .toString());
      }

      return applicationEnvironments.size() > 0 ? applicationEnvironments.get(0) : null;
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new PivotalClientApiException(PIVOTAL_CLOUD_FOUNDRY_CLIENT_EXCEPTION
              + "Failed while fetching Env details for application " + pcfRequestConfig.getApplicationName(),
          ex);
    }
  }

  @Override
  public List<String> getRoutesForSpace(PcfRequestConfig pcfRequestConfig)
      throws PivotalClientApiException, InterruptedException {
    List<Route> routes = getAllRoutesForSpace(pcfRequestConfig);
    if (!CollectionUtils.isEmpty(routes)) {
      return routes.stream().map(this::getPathFromRouteMap).collect(toList());
    }

    return Collections.EMPTY_LIST;
  }

  @Override
  public List<Route> getRouteMapsByNames(List<String> paths, PcfRequestConfig pcfRequestConfig)
      throws PivotalClientApiException, InterruptedException {
    if (isEmpty(paths)) {
      return Collections.EMPTY_LIST;
    }

    List<Route> routes = getAllRoutesForSpace(pcfRequestConfig);
    paths = paths.stream().map(String::toLowerCase).collect(toList());
    Set<String> routeSet = new HashSet<>(paths);

    return routes.stream()
        .filter(route -> routeSet.contains(getPathFromRouteMap(route).toLowerCase()))
        .collect(toList());
  }

  private List<Route> getAllRoutesForSpace(PcfRequestConfig pcfRequestConfig)
      throws PivotalClientApiException, InterruptedException {
    log.info(new StringBuilder()
                 .append(PIVOTAL_CLOUD_FOUNDRY_LOG_PREFIX)
                 .append("Getting routeMaps for Application: ")
                 .append(pcfRequestConfig.getApplicationName())
                 .toString());

    CountDownLatch latch = new CountDownLatch(1);
    AtomicBoolean exceptionOccured = new AtomicBoolean(false);
    StringBuilder errorBuilder = new StringBuilder();
    List<Route> routes = new ArrayList<>();
    try (CloudFoundryOperationsWrapper operationsWrapper =
             cloudFoundryOperationsProvider.getCloudFoundryOperationsWrapper(pcfRequestConfig)) {
      operationsWrapper.getCloudFoundryOperations()
          .routes()
          .list(ListRoutesRequest.builder().level(Level.SPACE).build())
          .subscribe(routes::add, throwable -> {
            exceptionOccured.set(true);
            handleException(throwable, "getRouteMap", errorBuilder);
            latch.countDown();
          }, latch::countDown);

      waitTillCompletion(latch, pcfRequestConfig.getTimeOutIntervalInMins());
      if (exceptionOccured.get()) {
        throw new PivotalClientApiException(new StringBuilder()
                                                .append("Exception occurred while getting routeMaps for Application: ")
                                                .append(pcfRequestConfig.getApplicationName())
                                                .append(", Error: " + errorBuilder.toString())
                                                .toString());
      }
      return routes;
    }
  }

  @Override
  public void unmapRoutesForApplication(PcfRequestConfig pcfRequestConfig, List<String> routes)
      throws PivotalClientApiException, InterruptedException {
    log.info(new StringBuilder()
                 .append(PIVOTAL_CLOUD_FOUNDRY_LOG_PREFIX)
                 .append("Unmapping route maps for : ")
                 .append(pcfRequestConfig.getApplicationName())
                 .append(", Paths: ")
                 .append(routes)
                 .toString());

    List<Route> routeList = getRouteMapsByNames(routes, pcfRequestConfig);
    for (Route route : routeList) {
      unmapRouteMapForApp(pcfRequestConfig, route);
    }
  }

  @Override
  public void createRouteMap(PcfRequestConfig pcfRequestConfig, String host, String domain, String path,
      boolean tcpRoute, boolean useRandomPort, Integer port) throws PivotalClientApiException, InterruptedException {
    log.info(new StringBuilder()
                 .append(PIVOTAL_CLOUD_FOUNDRY_LOG_PREFIX)
                 .append("creating routeMap: ")
                 .append(host + "." + domain)
                 .append(" for Endpoint: ")
                 .append(pcfRequestConfig.getEndpointUrl())
                 .append(", Organization: ")
                 .append(pcfRequestConfig.getOrgName())
                 .append(", for Space: ")
                 .append(pcfRequestConfig.getSpaceName())
                 .append(", AppName: ")
                 .append(pcfRequestConfig.getApplicationName())
                 .toString());

    // create routeMap
    final CountDownLatch latch2 = new CountDownLatch(1);
    AtomicBoolean exceptionOccured = new AtomicBoolean(false);
    StringBuilder errorBuilder = new StringBuilder();

    path = StringUtils.isBlank(path) ? null : path;
    errorBuilder.setLength(0);

    CreateRouteRequest.Builder createRouteRequestBuilder =
        getCreateRouteRequest(pcfRequestConfig, host, domain, path, tcpRoute, useRandomPort, port);
    try (CloudFoundryOperationsWrapper operationsWrapper =
             cloudFoundryOperationsProvider.getCloudFoundryOperationsWrapper(pcfRequestConfig)) {
      operationsWrapper.getCloudFoundryOperations()
          .routes()
          .create(createRouteRequestBuilder.build())
          .subscribe(null, throwable -> {
            exceptionOccured.set(true);
            handleException(throwable, "createRouteMapIfNotExists", errorBuilder);
            latch2.countDown();
          }, latch2::countDown);

      waitTillCompletion(latch2, 5);

      if (exceptionOccured.get()) {
        throw new PivotalClientApiException(new StringBuilder()
                                                .append("Exception occured while creating routeMap: ")
                                                .append(host + "." + domain)
                                                .append(" for Endpoint: ")
                                                .append(pcfRequestConfig.getEndpointUrl())
                                                .append(", Organization: ")
                                                .append(pcfRequestConfig.getOrgName())
                                                .append(", for Space: ")
                                                .append(pcfRequestConfig.getSpaceName())
                                                .append(", AppName: ")
                                                .append(pcfRequestConfig.getApplicationName())
                                                .append(", Host: ")
                                                .append(host)
                                                .append(", Domain: ")
                                                .append(domain)
                                                .append(", Path: ")
                                                .append(path)
                                                .append(", Port")
                                                .append(port)
                                                .append(", Error: " + errorBuilder.toString())
                                                .toString());
      }
    }
  }

  public List<Domain> getAllDomainsForSpace(PcfRequestConfig pcfRequestConfig)
      throws PivotalClientApiException, InterruptedException {
    log.info(new StringBuilder()
                 .append(PIVOTAL_CLOUD_FOUNDRY_LOG_PREFIX)
                 .append("Getting Domains for Space: ")
                 .append(pcfRequestConfig.getSpaceName())
                 .toString());

    CountDownLatch latch = new CountDownLatch(1);
    AtomicBoolean exceptionOccured = new AtomicBoolean(false);
    StringBuilder errorBuilder = new StringBuilder();
    List<Domain> domains = new ArrayList<>();

    try (CloudFoundryOperationsWrapper operationsWrapper =
             cloudFoundryOperationsProvider.getCloudFoundryOperationsWrapper(pcfRequestConfig)) {
      operationsWrapper.getCloudFoundryOperations().domains().list().subscribe(domains::add, throwable -> {
        exceptionOccured.set(true);
        handleException(throwable, "getAllDomainsForSpace", errorBuilder);
        latch.countDown();
      }, latch::countDown);

      waitTillCompletion(latch, pcfRequestConfig.getTimeOutIntervalInMins());
      if (exceptionOccured.get()) {
        throw new PivotalClientApiException(new StringBuilder()
                                                .append("Exception occurred while getting domains for space: ")
                                                .append(pcfRequestConfig.getApplicationName())
                                                .append(", Error: " + errorBuilder.toString())
                                                .toString());
      }
      return domains;
    }
  }

  private CreateRouteRequest.Builder getCreateRouteRequest(PcfRequestConfig pcfRequestConfig, String host,
      String domain, String path, boolean tcpRoute, boolean useRandomPort, Integer port) {
    CreateRouteRequest.Builder createRouteRequestBuilder =
        CreateRouteRequest.builder().domain(domain).space(pcfRequestConfig.getSpaceName());

    if (tcpRoute) {
      addTcpRouteDetails(useRandomPort, port, createRouteRequestBuilder);
    } else {
      addHttpRouteDetails(host, path, createRouteRequestBuilder);
    }
    return createRouteRequestBuilder;
  }

  /**
   * Http Route Needs Domain, host and path is optional
   */
  private void addHttpRouteDetails(String host, String path, CreateRouteRequest.Builder createRouteRequestBuilder) {
    createRouteRequestBuilder.path(path);
    createRouteRequestBuilder.host(host);
  }

  private void addTcpRouteDetails(
      boolean useRandomPort, Integer port, CreateRouteRequest.Builder createRouteRequestBuilder) {
    if (useRandomPort) {
      createRouteRequestBuilder.randomPort(true);
    } else {
      createRouteRequestBuilder.port(port);
    }
  }

  @Override
  public Optional<Route> getRouteMap(PcfRequestConfig pcfRequestConfig, String route)
      throws PivotalClientApiException, InterruptedException {
    if (StringUtils.isBlank(route)) {
      throw new PivotalClientApiException(
          PIVOTAL_CLOUD_FOUNDRY_LOG_PREFIX + "Route Can Not Be Blank When Fetching RouteMap");
    }

    List<Route> routes = getRouteMapsByNames(Arrays.asList(route), pcfRequestConfig);
    if (isNotEmpty(routes)) {
      return Optional.of(routes.get(0));
    }

    return Optional.empty();
  }

  @Override
  public void mapRoutesForApplication(PcfRequestConfig pcfRequestConfig, List<String> routes)
      throws PivotalClientApiException, InterruptedException {
    log.info(new StringBuilder()
                 .append(PIVOTAL_CLOUD_FOUNDRY_LOG_PREFIX)
                 .append("Mapping route maps for Application : ")
                 .append(pcfRequestConfig.getApplicationName())
                 .append(", Paths: ")
                 .append(routes)
                 .toString());

    List<Route> routeList = getRouteMapsByNames(routes, pcfRequestConfig);
    List<String> routesNeedToBeCreated = findRoutesNeedToBeCreated(routes, routeList);

    if (isNotEmpty(routesNeedToBeCreated)) {
      List<Domain> allDomainsForSpace = getAllDomainsForSpace(pcfRequestConfig);
      Set<String> domainNames = allDomainsForSpace.stream().map(Domain::getName).collect(toSet());
      createRoutesThatDoNotExists(routesNeedToBeCreated, domainNames, pcfRequestConfig);
      routeList = getRouteMapsByNames(routes, pcfRequestConfig);
    }
    for (Route route : routeList) {
      mapRouteMapForApp(pcfRequestConfig, route);
    }
  }

  private void createRoutesThatDoNotExists(List<String> routesNeedToBeCreated, Set<String> domainNames,
      PcfRequestConfig pcfRequestConfig) throws PivotalClientApiException, InterruptedException {
    for (String routeToCreate : routesNeedToBeCreated) {
      createRouteFromPath(routeToCreate, pcfRequestConfig, domainNames);
    }
  }

  @VisibleForTesting
  void createRouteFromPath(String routeToCreate, PcfRequestConfig pcfRequestConfig, Set<String> domainNames)
      throws PivotalClientApiException, InterruptedException {
    boolean validRoute = false;
    String domainNameUsed = EMPTY;
    for (String domainName : domainNames) {
      if (routeToCreate.contains(domainName)) {
        if (!validRoute) {
          validRoute = true;
          domainNameUsed = domainName;
        } else {
          if (domainName.length() > domainNameUsed.length()) {
            domainNameUsed = domainName;
          }
        }
      }
    }

    if (!validRoute) {
      throw new PivotalClientApiException(new StringBuilder(128)
                                              .append("Invalid Route Name: ")
                                              .append(routeToCreate)
                                              .append(", used domain not present in this space")
                                              .toString());
    }

    int domainStartIndex = routeToCreate.indexOf(domainNameUsed);
    String hostName = domainStartIndex == 0 ? null : routeToCreate.substring(0, domainStartIndex - 1);

    String path = null;
    int indexForPath = routeToCreate.indexOf(PCF_ROUTE_PATH_SEPARATOR);
    if (indexForPath != -1) {
      path = routeToCreate.substring(indexForPath);
    }

    createRouteMap(pcfRequestConfig, hostName, domainNameUsed, path, false, false, null);
  }

  @VisibleForTesting
  List<String> findRoutesNeedToBeCreated(List<String> routes, List<Route> routeList) {
    if (isNotEmpty(routes)) {
      Set<String> routesExisting = routeList.stream().map(this::getPathFromRouteMap).collect(toSet());
      return routes.stream().filter(route -> !routesExisting.contains(route)).collect(toList());
    }

    return emptyList();
  }

  @VisibleForTesting
  String getPathFromRouteMap(Route route) {
    return new StringBuilder()
        .append(StringUtils.isBlank(route.getHost()) ? EMPTY : route.getHost() + ".")
        .append(route.getDomain())
        .append(StringUtils.isBlank(route.getPath()) ? EMPTY : route.getPath())
        .append(StringUtils.isBlank(route.getPort()) ? EMPTY : ":" + Integer.parseInt(route.getPort()))
        .toString();
  }

  @Override
  public void unmapRouteMapForApp(PcfRequestConfig pcfRequestConfig, Route route)
      throws PivotalClientApiException, InterruptedException {
    log.info(new StringBuilder()
                 .append(PIVOTAL_CLOUD_FOUNDRY_LOG_PREFIX)
                 .append("Unmapping routeMap for Application: ")
                 .append(pcfRequestConfig.getApplicationName())
                 .toString());

    CountDownLatch latch = new CountDownLatch(1);
    AtomicBoolean exceptionOccured = new AtomicBoolean(false);
    StringBuilder errorBuilder = new StringBuilder();
    UnmapRouteRequest.Builder builder = UnmapRouteRequest.builder()
                                            .applicationName(pcfRequestConfig.getApplicationName())
                                            .domain(route.getDomain())
                                            .host(route.getHost())
                                            .path(route.getPath());

    if (!StringUtils.isBlank(route.getPort())) {
      builder.port(Integer.valueOf(route.getPort()));
    }

    try (CloudFoundryOperationsWrapper operationsWrapper =
             cloudFoundryOperationsProvider.getCloudFoundryOperationsWrapper(pcfRequestConfig)) {
      operationsWrapper.getCloudFoundryOperations().routes().unmap(builder.build()).subscribe(null, throwable -> {
        exceptionOccured.set(true);
        handleException(throwable, "unmapRouteMapForApp", errorBuilder);
        latch.countDown();
      }, latch::countDown);

      waitTillCompletion(latch, pcfRequestConfig.getTimeOutIntervalInMins());

      if (exceptionOccured.get()) {
        throw new PivotalClientApiException(new StringBuilder()
                                                .append("Exception occurred while unmapping routeMap for Application: ")
                                                .append(pcfRequestConfig.getApplicationName())
                                                .append(", Error: " + errorBuilder.toString())
                                                .toString());
      }
    }
  }

  @Override
  public void mapRouteMapForApp(PcfRequestConfig pcfRequestConfig, Route route)
      throws PivotalClientApiException, InterruptedException {
    log.info(new StringBuilder()
                 .append(PIVOTAL_CLOUD_FOUNDRY_LOG_PREFIX)
                 .append("Mapping routeMap: ")
                 .append(route)
                 .append(", AppName: ")
                 .append(pcfRequestConfig.getApplicationName())
                 .toString());

    CountDownLatch latch = new CountDownLatch(1);
    MapRouteRequest.Builder builder = MapRouteRequest.builder()
                                          .applicationName(pcfRequestConfig.getApplicationName())
                                          .domain(route.getDomain())
                                          .host(route.getHost())
                                          .path(route.getPath());

    if (!StringUtils.isEmpty(route.getPort())) {
      builder.port(Integer.valueOf(route.getPort()));
    }

    AtomicBoolean exceptionOccured = new AtomicBoolean(false);
    StringBuilder errorBuilder = new StringBuilder();
    try (CloudFoundryOperationsWrapper operationsWrapper =
             cloudFoundryOperationsProvider.getCloudFoundryOperationsWrapper(pcfRequestConfig)) {
      operationsWrapper.getCloudFoundryOperations().routes().map(builder.build()).subscribe(null, throwable -> {
        exceptionOccured.set(true);
        handleException(throwable, "mapRouteMapForApp", errorBuilder);
        latch.countDown();
      }, latch::countDown);

      waitTillCompletion(latch, pcfRequestConfig.getTimeOutIntervalInMins());

      if (exceptionOccured.get()) {
        throw new PivotalClientApiException(new StringBuilder()
                                                .append("Exception occurred while mapping routeMap: ")
                                                .append(route)
                                                .append(", AppName: ")
                                                .append(pcfRequestConfig.getApplicationName())
                                                .append(", Error: " + errorBuilder.toString())
                                                .toString());
      }
    }
  }

  private void handleException(Throwable t, String apiName, StringBuilder errorBuilder) {
    log.error(PIVOTAL_CLOUD_FOUNDRY_LOG_PREFIX + "Exception Occured while executing PCF api: " + apiName, t);
    errorBuilder.append(t.getMessage());
  }

  private void handleExceptionForGetOraganizationsAPI(Throwable t, String apiName, StringBuilder errorBuilder) {
    log.error(PIVOTAL_CLOUD_FOUNDRY_LOG_PREFIX + "Exception Occured while executing PCF api: " + apiName
        + " EXCEPTION: " + t.toString());
    errorBuilder.append(t.getMessage());
  }

  private void waitTillCompletion(CountDownLatch latch, int time)
      throws InterruptedException, PivotalClientApiException {
    boolean check = latch.await(time, TimeUnit.MINUTES);
    if (!check) {
      throw new PivotalClientApiException(PIVOTAL_CLOUD_FOUNDRY_LOG_PREFIX + "PCF operation times out");
    }
  }
}
