package software.wings.helpers.ext.pcf;

import static java.util.stream.Collectors.toList;
import static software.wings.helpers.ext.pcf.PcfConstants.PIVOTAL_CLOUD_FOUNDRY_LOG_PREFIX;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.harness.data.structure.EmptyPredicate;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.operations.applications.ApplicationDetail;
import org.cloudfoundry.operations.applications.ApplicationManifest;
import org.cloudfoundry.operations.applications.ApplicationManifest.Builder;
import org.cloudfoundry.operations.applications.ApplicationManifestUtils;
import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.cloudfoundry.operations.applications.DeleteApplicationRequest;
import org.cloudfoundry.operations.applications.GetApplicationManifestRequest;
import org.cloudfoundry.operations.applications.GetApplicationRequest;
import org.cloudfoundry.operations.applications.ListApplicationTasksRequest;
import org.cloudfoundry.operations.applications.PushApplicationManifestRequest;
import org.cloudfoundry.operations.applications.ScaleApplicationRequest;
import org.cloudfoundry.operations.applications.SetEnvironmentVariableApplicationRequest;
import org.cloudfoundry.operations.applications.StartApplicationRequest;
import org.cloudfoundry.operations.applications.StopApplicationRequest;
import org.cloudfoundry.operations.applications.Task;
import org.cloudfoundry.operations.organizations.OrganizationDetail;
import org.cloudfoundry.operations.organizations.OrganizationInfoRequest;
import org.cloudfoundry.operations.organizations.OrganizationSummary;
import org.cloudfoundry.operations.routes.CheckRouteRequest;
import org.cloudfoundry.operations.routes.CreateRouteRequest;
import org.cloudfoundry.operations.routes.Level;
import org.cloudfoundry.operations.routes.ListRoutesRequest;
import org.cloudfoundry.operations.routes.MapRouteRequest;
import org.cloudfoundry.operations.routes.Route;
import org.cloudfoundry.operations.routes.UnmapRouteRequest;
import org.cloudfoundry.reactor.ConnectionContext;
import org.cloudfoundry.reactor.DefaultConnectionContext;
import org.cloudfoundry.reactor.TokenProvider;
import org.cloudfoundry.reactor.client.ReactorCloudFoundryClient;
import org.cloudfoundry.reactor.tokenprovider.PasswordGrantTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class PcfClientImpl implements PcfClient {
  private static final Logger logger = LoggerFactory.getLogger(PcfClientImpl.class);

  public CloudFoundryOperations getCloudFoundryOperations(PcfRequestConfig pcfRequestConfig)
      throws PivotalClientApiException {
    try {
      return DefaultCloudFoundryOperations.builder()
          .cloudFoundryClient(getCloudFoundryClient(pcfRequestConfig))
          .organization(pcfRequestConfig.getOrgName())
          .space(pcfRequestConfig.getSpaceName())
          .build();
    } catch (Exception e) {
      throw new PivotalClientApiException("Exception while creating CloudFoundryOperations: ", e);
    }
  }

  public CloudFoundryClient getCloudFoundryClient(PcfRequestConfig pcfRequestConfig) throws PivotalClientApiException {
    try {
      return ReactorCloudFoundryClient.builder()
          .connectionContext(getConnectionContext(pcfRequestConfig.getEndpointUrl()))
          .tokenProvider(getTokenProvider(pcfRequestConfig.getUserName(), pcfRequestConfig.getPassword()))
          .build();
    } catch (Exception e) {
      throw new PivotalClientApiException("Exception while creating CloudFoundryOperations: ", e);
    }
  }

  @Override
  public List<OrganizationSummary> getOrganizations(PcfRequestConfig pcfRequestConfig)
      throws PivotalClientApiException, InterruptedException {
    logger.info(
        new StringBuilder().append(PIVOTAL_CLOUD_FOUNDRY_LOG_PREFIX).append("Fetching Organizations ").toString());

    List<OrganizationSummary> organizations = new ArrayList<>();

    AtomicBoolean exceptionOccured = new AtomicBoolean(false);
    StringBuilder errorBuilder = new StringBuilder();
    CountDownLatch latch = new CountDownLatch(1);
    getCloudFoundryOperations(pcfRequestConfig).organizations().list().subscribe(organizations::add, throwable -> {
      exceptionOccured.set(true);
      handleException(throwable, "getOrganizations", errorBuilder);
      latch.countDown();
    }, latch::countDown);

    waitTillCompletion(latch, pcfRequestConfig.getTimeOutIntervalInMins());
    if (exceptionOccured.get()) {
      throw new PivotalClientApiException(new StringBuilder()
                                              .append("Exception occured while fetching Organizations")
                                              .append(", Error: " + errorBuilder.toString())
                                              .toString());
    }
    return organizations;
  }

  public List<String> getSpacesForOrganization(PcfRequestConfig pcfRequestConfig)
      throws PivotalClientApiException, InterruptedException {
    List<OrganizationDetail> organizationDetails = new ArrayList<>();
    List<String> spaces = new ArrayList<>();
    logger.info(new StringBuilder().append(PIVOTAL_CLOUD_FOUNDRY_LOG_PREFIX).append("Fetching Spaces ").toString());

    CountDownLatch latch = new CountDownLatch(1);

    AtomicBoolean exceptionOccured = new AtomicBoolean(false);
    StringBuilder errorBuilder = new StringBuilder();
    getCloudFoundryOperations(pcfRequestConfig)
        .organizations()
        .get(OrganizationInfoRequest.builder().name(pcfRequestConfig.getOrgName()).build())
        .subscribe(organizationDetails::add, throwable -> {
          exceptionOccured.set(true);
          handleException(throwable, "getSpacesForOrganization", errorBuilder);
          latch.countDown();
        }, latch::countDown);

    waitTillCompletion(latch, pcfRequestConfig.getTimeOutIntervalInMins());

    if (exceptionOccured.get()) {
      throw new PivotalClientApiException(new StringBuilder(PIVOTAL_CLOUD_FOUNDRY_LOG_PREFIX)
                                              .append("Exception occured while fetching Spaces")
                                              .append(", Error: " + errorBuilder.toString())
                                              .toString());
    }

    if (!CollectionUtils.isEmpty(organizationDetails)) {
      return organizationDetails.stream()
          .flatMap(organizationDetail -> organizationDetail.getSpaces().stream())
          .collect(toList());
    }

    return spaces;
  }

  public List<String> getRoutesForSpace(PcfRequestConfig pcfRequestConfig)
      throws PivotalClientApiException, InterruptedException {
    List<Route> routes = getAllRoutesForSpace(pcfRequestConfig);
    if (!CollectionUtils.isEmpty(routes)) {
      return routes.stream().map(route -> getPathFromRouteMap(route)).collect(toList());
    }

    return Collections.EMPTY_LIST;
  }

  public List<ApplicationSummary> getApplications(PcfRequestConfig pcfRequestConfig)
      throws PivotalClientApiException, InterruptedException {
    logger.info(
        new StringBuilder().append(PIVOTAL_CLOUD_FOUNDRY_LOG_PREFIX).append("Fetching PCF Applications: ").toString());
    CountDownLatch latch = new CountDownLatch(1);
    List<ApplicationSummary> applicationSummaries = new ArrayList<>();

    AtomicBoolean exceptionOccured = new AtomicBoolean(false);
    StringBuilder errorBuilder = new StringBuilder();
    getCloudFoundryOperations(pcfRequestConfig)
        .applications()
        .list()
        .subscribe(applicationSummaries::add, throwable -> {
          exceptionOccured.set(true);
          handleException(throwable, "getApplications", errorBuilder);
          latch.countDown();
        }, latch::countDown);

    waitTillCompletion(latch, pcfRequestConfig.getTimeOutIntervalInMins());

    if (exceptionOccured.get()) {
      throw new PivotalClientApiException(new StringBuilder()
                                              .append("Exception occured while fetching Applications ")
                                              .append(", Error: " + errorBuilder.toString())
                                              .toString());
    }
    return applicationSummaries;
  }

  @SuppressFBWarnings("DLS_DEAD_LOCAL_STORE")
  public void scaleApplications(PcfRequestConfig pcfRequestConfig)
      throws PivotalClientApiException, InterruptedException {
    logger.info(new StringBuilder()
                    .append(PIVOTAL_CLOUD_FOUNDRY_LOG_PREFIX)
                    .append("Scaling Applications: ")
                    .append(pcfRequestConfig.getApplicationName())
                    .append(", to count: ")
                    .append(pcfRequestConfig.getDesiredCount())
                    .toString());

    CountDownLatch latch = new CountDownLatch(1);
    List<ApplicationSummary> applicationSummaries = new ArrayList<>();

    AtomicBoolean exceptionOccured = new AtomicBoolean(false);
    StringBuilder errorBuilder = new StringBuilder();
    getCloudFoundryOperations(pcfRequestConfig)
        .applications()
        .scale(ScaleApplicationRequest.builder()
                   .name(pcfRequestConfig.getApplicationName())
                   .instances(pcfRequestConfig.getDesiredCount())
                   .build())
        .subscribe(null, throwable -> {
          exceptionOccured.set(true);
          handleException(throwable, "scaleApplications", errorBuilder);
          latch.countDown();
        }, latch::countDown);

    waitTillCompletion(latch, pcfRequestConfig.getTimeOutIntervalInMins());

    if (exceptionOccured.get()) {
      throw new PivotalClientApiException(new StringBuilder()
                                              .append("Exception Occured Scaling Applications: ")
                                              .append(pcfRequestConfig.getApplicationName())
                                              .append(", to count: ")
                                              .append(pcfRequestConfig.getDesiredCount())
                                              .append(", Error: " + errorBuilder.toString())
                                              .toString());
    }
  }

  public void getTasks(PcfRequestConfig pcfRequestConfig) throws PivotalClientApiException, InterruptedException {
    logger.info(new StringBuilder()
                    .append(PIVOTAL_CLOUD_FOUNDRY_LOG_PREFIX)
                    .append("Getting Tasks for Applications: ")
                    .append(pcfRequestConfig.getApplicationName())
                    .toString());

    CountDownLatch latch = new CountDownLatch(1);
    List<Task> tasks = new ArrayList<>();

    AtomicBoolean exceptionOccured = new AtomicBoolean(false);
    StringBuilder errorBuilder = new StringBuilder();

    getCloudFoundryOperations(pcfRequestConfig)
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
                                              .append("Exception Occured while getting Tasks for Application: ")
                                              .append(pcfRequestConfig.getApplicationName())
                                              .append(", Error: " + errorBuilder.toString())
                                              .toString());
    }
  }

  public void pushApplicationUsingManifest(PcfRequestConfig pcfRequestConfig, String filePath)
      throws PivotalClientApiException, InterruptedException {
    logger.info(new StringBuilder()
                    .append(PIVOTAL_CLOUD_FOUNDRY_LOG_PREFIX)
                    .append("Creating Application: ")
                    .append(pcfRequestConfig.getApplicationName())
                    .toString());

    Path path = Paths.get(filePath);
    List<ApplicationManifest> applicationManifests = ApplicationManifestUtils.read(path);

    ApplicationManifest applicationManifest = applicationManifests.get(0);
    applicationManifest = InitializeApplicationManifest(applicationManifest, pcfRequestConfig);

    AtomicBoolean exceptionOccured = new AtomicBoolean(false);
    StringBuilder errorBuilder = new StringBuilder();
    CountDownLatch latch = new CountDownLatch(1);

    CloudFoundryOperations cloudFoundryOperations = getCloudFoundryOperations(pcfRequestConfig);

    cloudFoundryOperations.applications()
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

  private ApplicationManifest InitializeApplicationManifest(
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
      for (Entry<String, String> entry : pcfRequestConfig.getServiceVariables().entrySet()) {
        builder.environmentVariable(entry.getKey(), entry.getValue());
      }
    }

    if (EmptyPredicate.isNotEmpty(applicationManifest.getEnvironmentVariables())) {
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
        .buildpack(applicationManifest.getBuildpack())
        .path(applicationManifest.getPath())
        .instances(0)
        .build();
  }

  private void addRouteMapsToManifest(PcfRequestConfig pcfRequestConfig, Builder builder) {
    // Set routeMaps
    if (EmptyPredicate.isNotEmpty(pcfRequestConfig.getRouteMaps())) {
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
      appPrefix = appPrefix.replaceAll("__", "-");
      builder.host(appPrefix);
    }
  }

  public void stopApplication(PcfRequestConfig pcfRequestConfig)
      throws PivotalClientApiException, InterruptedException {
    logger.info(new StringBuilder()
                    .append(PIVOTAL_CLOUD_FOUNDRY_LOG_PREFIX)
                    .append("Stopping Application: ")
                    .append(pcfRequestConfig.getApplicationName())
                    .toString());

    CountDownLatch latch = new CountDownLatch(1);
    AtomicBoolean exceptionOccured = new AtomicBoolean(false);
    StringBuilder errorBuilder = new StringBuilder();
    getCloudFoundryOperations(pcfRequestConfig)
        .applications()
        .stop(StopApplicationRequest.builder().name(pcfRequestConfig.getApplicationName()).build())
        .subscribe(null, throwable -> {
          exceptionOccured.set(true);
          handleException(throwable, "stopApplication", errorBuilder);
          latch.countDown();
        }, latch::countDown);

    waitTillCompletion(latch, pcfRequestConfig.getTimeOutIntervalInMins());
    if (exceptionOccured.get()) {
      throw new PivotalClientApiException(new StringBuilder()
                                              .append("Exception occuered while stopping Application: ")
                                              .append(pcfRequestConfig.getApplicationName())
                                              .append(", Error: " + errorBuilder.toString())
                                              .toString());
    }
  }

  public ApplicationManifest getApplicationManifest(PcfRequestConfig pcfRequestConfig)
      throws PivotalClientApiException, InterruptedException {
    logger.info(new StringBuilder()
                    .append(PIVOTAL_CLOUD_FOUNDRY_LOG_PREFIX)
                    .append("Getting Manifest for Application : ")
                    .append(pcfRequestConfig.getApplicationName())
                    .toString());

    CountDownLatch latch = new CountDownLatch(1);
    AtomicBoolean exceptionOccured = new AtomicBoolean(false);
    List<ApplicationManifest> applicationManifests = new ArrayList<>();
    StringBuilder errorBuilder = new StringBuilder();

    getCloudFoundryOperations(pcfRequestConfig)
        .applications()
        .getApplicationManifest(
            GetApplicationManifestRequest.builder().name(pcfRequestConfig.getApplicationName()).build())
        .subscribe(applicationManifests::add, throwable -> {
          exceptionOccured.set(true);
          handleException(throwable, "fetApplicationManifest", errorBuilder);
          latch.countDown();
        }, latch::countDown);

    waitTillCompletion(latch, pcfRequestConfig.getTimeOutIntervalInMins());
    if (exceptionOccured.get()) {
      throw new PivotalClientApiException(new StringBuilder()
                                              .append("Exception occuered while fetching application manifest: ")
                                              .append(pcfRequestConfig.getApplicationName())
                                              .append(", Error: " + errorBuilder.toString())
                                              .toString());
    }

    return applicationManifests.get(0);
  }

  public List<Route> getRouteMapsByNames(List<String> paths, PcfRequestConfig pcfRequestConfig)
      throws PivotalClientApiException, InterruptedException {
    if (EmptyPredicate.isEmpty(paths)) {
      return Collections.EMPTY_LIST;
    }

    List<Route> routes = getAllRoutesForSpace(pcfRequestConfig);
    paths = paths.stream().map(path -> path.toLowerCase()).collect(toList());
    Set<String> routeSet = new HashSet<>(paths);

    return routes.stream()
        .filter(route -> routeSet.contains(getPathFromRouteMap(route).toLowerCase()))
        .collect(toList());
  }

  private List<Route> getAllRoutesForSpace(PcfRequestConfig pcfRequestConfig)
      throws PivotalClientApiException, InterruptedException {
    logger.info(new StringBuilder()
                    .append(PIVOTAL_CLOUD_FOUNDRY_LOG_PREFIX)
                    .append("Getting routeMaps for Application: ")
                    .append(pcfRequestConfig.getApplicationName())
                    .toString());

    CountDownLatch latch = new CountDownLatch(1);
    AtomicBoolean exceptionOccured = new AtomicBoolean(false);
    StringBuilder errorBuilder = new StringBuilder();
    List<Route> routes = new ArrayList<>();

    getCloudFoundryOperations(pcfRequestConfig)
        .routes()
        .list(ListRoutesRequest.builder().level(Level.SPACE).build())
        .subscribe(routes ::add, throwable -> {
          exceptionOccured.set(true);
          handleException(throwable, "getRouteMap", errorBuilder);
          latch.countDown();
        }, latch::countDown);

    waitTillCompletion(latch, pcfRequestConfig.getTimeOutIntervalInMins());
    if (exceptionOccured.get()) {
      throw new PivotalClientApiException(new StringBuilder()
                                              .append("Exception occuered while getting routeMaps for Application: ")
                                              .append(pcfRequestConfig.getApplicationName())
                                              .append(", Error: " + errorBuilder.toString())
                                              .toString());
    }
    return routes;
  }

  public ApplicationDetail getApplicationByName(PcfRequestConfig pcfRequestConfig)
      throws PivotalClientApiException, InterruptedException {
    logger.info(new StringBuilder()
                    .append(PIVOTAL_CLOUD_FOUNDRY_LOG_PREFIX)
                    .append("Getting application: ")
                    .append(pcfRequestConfig.getApplicationName())
                    .toString());
    CountDownLatch latch = new CountDownLatch(1);

    AtomicBoolean exceptionOccured = new AtomicBoolean(false);
    StringBuilder errorBuilder = new StringBuilder();
    List<ApplicationDetail> applicationDetails = new ArrayList<>();
    getCloudFoundryOperations(pcfRequestConfig)
        .applications()
        .get(GetApplicationRequest.builder().name(pcfRequestConfig.getApplicationName()).build())
        .subscribe(applicationDetails::add, throwable -> {
          exceptionOccured.set(true);
          handleException(throwable, "getApplicationByName", errorBuilder);
          latch.countDown();
        }, latch::countDown);

    waitTillCompletion(latch, pcfRequestConfig.getTimeOutIntervalInMins());
    if (exceptionOccured.get()) {
      throw new PivotalClientApiException(new StringBuilder()
                                              .append("Exception occuered while  getting application: ")
                                              .append(pcfRequestConfig.getApplicationName())
                                              .append(", Error: " + errorBuilder.toString())
                                              .toString());
    }

    return applicationDetails.size() > 0 ? applicationDetails.get(0) : null;
  }

  public void createRouteMapIfNotExists(PcfRequestConfig pcfRequestConfig, String host, String domain)
      throws PivotalClientApiException, InterruptedException {
    logger.info(new StringBuilder()
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

    final CountDownLatch latch = new CountDownLatch(1);
    AtomicBoolean exceptionOccured = new AtomicBoolean(false);
    StringBuilder errorBuilder = new StringBuilder();

    getCloudFoundryOperations(pcfRequestConfig)
        .routes()
        .check(CheckRouteRequest.builder().host(host).domain(domain).build())
        .subscribe(null, throwable -> {
          exceptionOccured.set(true);
          handleException(throwable, "createRouteMapIfNotExists", errorBuilder);
          latch.countDown();
        }, latch::countDown);

    waitTillCompletion(latch, pcfRequestConfig.getTimeOutIntervalInMins());

    // create routeMap
    final CountDownLatch latch2 = new CountDownLatch(1);
    errorBuilder.setLength(0);
    getCloudFoundryOperations(pcfRequestConfig)
        .routes()
        .create(CreateRouteRequest.builder().domain(domain).host(host).space(pcfRequestConfig.getSpaceName()).build())
        .subscribe(null, throwable -> {
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
                                              .append(", Error: " + errorBuilder.toString())
                                              .toString());
    }
  }

  public void deleteApplication(PcfRequestConfig pcfRequestConfig)
      throws PivotalClientApiException, InterruptedException {
    logger.info(new StringBuilder()
                    .append(PIVOTAL_CLOUD_FOUNDRY_LOG_PREFIX)
                    .append("Deleting application: ")
                    .append(pcfRequestConfig.getApplicationName())
                    .toString());

    StringBuilder errorBuilder = new StringBuilder();
    CountDownLatch latch = new CountDownLatch(1);
    AtomicBoolean exceptionOccured = new AtomicBoolean(false);
    getCloudFoundryOperations(pcfRequestConfig)
        .applications()
        .delete(
            DeleteApplicationRequest.builder().name(pcfRequestConfig.getApplicationName()).deleteRoutes(false).build())
        .subscribe(null, throwable -> {
          exceptionOccured.set(true);
          handleException(throwable, "deleteApplication", errorBuilder);
          latch.countDown();
        }, latch ::countDown);

    waitTillCompletion(latch, pcfRequestConfig.getTimeOutIntervalInMins());
    if (exceptionOccured.get()) {
      throw new PivotalClientApiException(new StringBuilder()
                                              .append("Exception occured while deleting application: ")
                                              .append(pcfRequestConfig.getApplicationName())
                                              .append(", Error: " + errorBuilder.toString())
                                              .toString());
    }
  }

  private void handleException(Throwable t, String apiName, StringBuilder errorBuilder) {
    logger.error(PIVOTAL_CLOUD_FOUNDRY_LOG_PREFIX + "Exception Occured while executing PCF api: " + apiName, t);
    errorBuilder.append(t.getMessage());
  }

  private void waitTillCompletion(CountDownLatch latch, int time) throws InterruptedException {
    boolean check = latch.await(time, TimeUnit.MINUTES);
    if (!check) {
      throw new RuntimeException(PIVOTAL_CLOUD_FOUNDRY_LOG_PREFIX + "PCF operation times out");
    }
  }

  private void setEnvironmentVariable(CloudFoundryOperations cloudFoundryOperations, PcfRequestConfig pcfRequestConfig,
      String key, String value) throws PivotalClientApiException, InterruptedException {
    logger.info(new StringBuilder()
                    .append(PIVOTAL_CLOUD_FOUNDRY_LOG_PREFIX)
                    .append("Setting Env Variables for application: ")
                    .append(pcfRequestConfig.getApplicationName())
                    .toString());

    CountDownLatch latch = new CountDownLatch(1);
    AtomicBoolean exceptionOccured = new AtomicBoolean(false);
    StringBuilder errorBuilder = new StringBuilder();
    cloudFoundryOperations.applications()
        .setEnvironmentVariable(SetEnvironmentVariableApplicationRequest.builder()
                                    .name(pcfRequestConfig.getApplicationName())
                                    .variableName(key)
                                    .variableValue(value)
                                    .build())
        .subscribe(null, throwable -> {
          exceptionOccured.set(true);
          handleException(throwable, "setEnvironmentVariable", errorBuilder);
          latch.countDown();
        }, latch::countDown);

    waitTillCompletion(latch, pcfRequestConfig.getTimeOutIntervalInMins());

    if (exceptionOccured.get()) {
      throw new PivotalClientApiException(new StringBuilder()
                                              .append("Exception Occured while setting Env Variables for application: ")
                                              .append(pcfRequestConfig.getApplicationName())
                                              .append(", Error: " + errorBuilder.toString())
                                              .toString());
    }
  }

  public void startApplication(PcfRequestConfig pcfRequestConfig)
      throws PivotalClientApiException, InterruptedException {
    logger.info(new StringBuilder()
                    .append(PIVOTAL_CLOUD_FOUNDRY_LOG_PREFIX)
                    .append("Starting application: ")
                    .append(pcfRequestConfig.getApplicationName())
                    .toString());

    CountDownLatch latch = new CountDownLatch(1);

    AtomicBoolean exceptionOccured = new AtomicBoolean(false);
    StringBuilder errorBuilder = new StringBuilder();
    getCloudFoundryOperations(pcfRequestConfig)
        .applications()
        .start(StartApplicationRequest.builder().name(pcfRequestConfig.getApplicationName()).build())
        .subscribe(null, throwable -> {
          exceptionOccured.set(true);
          handleException(throwable, "startApplication", errorBuilder);
          latch.countDown();
        }, latch::countDown);

    waitTillCompletion(latch, pcfRequestConfig.getTimeOutIntervalInMins());
    if (exceptionOccured.get()) {
      throw new PivotalClientApiException(new StringBuilder()
                                              .append("Exception Occured while starting application: ")
                                              .append(pcfRequestConfig.getApplicationName())
                                              .append(", Error: " + errorBuilder.toString())
                                              .toString());
    }
  }

  public void unmapRoutesForApplication(PcfRequestConfig pcfRequestConfig, List<String> routes)
      throws PivotalClientApiException, InterruptedException {
    logger.info(new StringBuilder()
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

  public void mapRoutesForApplication(PcfRequestConfig pcfRequestConfig, List<String> routes)
      throws PivotalClientApiException, InterruptedException {
    logger.info(new StringBuilder()
                    .append(PIVOTAL_CLOUD_FOUNDRY_LOG_PREFIX)
                    .append("Mapping route maps for Application : ")
                    .append(pcfRequestConfig.getApplicationName())
                    .append(", Paths: ")
                    .append(routes)
                    .toString());

    List<Route> routeList = getRouteMapsByNames(routes, pcfRequestConfig);
    for (Route route : routeList) {
      mapRouteMapForApp(pcfRequestConfig, route);
    }
  }

  private String getPathFromRouteMap(Route route) {
    return new StringBuilder()
        .append(StringUtils.isBlank(route.getHost()) ? StringUtils.EMPTY : route.getHost() + ".")
        .append(route.getDomain())
        .append(StringUtils.isBlank(route.getPath()) ? StringUtils.EMPTY : "/" + route.getPath())
        .append(StringUtils.isBlank(route.getPort()) ? StringUtils.EMPTY : ":" + Integer.parseInt(route.getPort()))
        .toString();
  }

  public void unmapRouteMapForApp(PcfRequestConfig pcfRequestConfig, Route route)
      throws PivotalClientApiException, InterruptedException {
    logger.info(new StringBuilder()
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

    getCloudFoundryOperations(pcfRequestConfig).routes().unmap(builder.build()).subscribe(null, throwable -> {
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

  public void mapRouteMapForApp(PcfRequestConfig pcfRequestConfig, Route route)
      throws PivotalClientApiException, InterruptedException {
    logger.info(new StringBuilder()
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
    getCloudFoundryOperations(pcfRequestConfig).routes().map(builder.build()).subscribe(null, throwable -> {
      exceptionOccured.set(true);
      handleException(throwable, "mapRouteMapForApp", errorBuilder);
      latch.countDown();
    }, latch::countDown);

    waitTillCompletion(latch, pcfRequestConfig.getTimeOutIntervalInMins());

    if (exceptionOccured.get()) {
      throw new PivotalClientApiException(new StringBuilder()
                                              .append("Exception occuered while mapping routeMap: ")
                                              .append(route)
                                              .append(", AppName: ")
                                              .append(pcfRequestConfig.getApplicationName())
                                              .append(", Error: " + errorBuilder.toString())
                                              .toString());
    }
  }

  private TokenProvider getTokenProvider(String username, String password) throws PivotalClientApiException {
    try {
      return PasswordGrantTokenProvider.builder().username(username).password(password).build();
    } catch (Exception t) {
      throw new PivotalClientApiException(t.getMessage());
    }
  }

  private ConnectionContext getConnectionContext(String endPointUrl) throws PivotalClientApiException {
    try {
      return DefaultConnectionContext.builder().apiHost(endPointUrl).skipSslValidation(false).build();
    } catch (Exception t) {
      throw new PivotalClientApiException(t.getMessage());
    }
  }
}
