package software.wings.helpers.ext.pcf;

import static com.google.common.base.Charsets.UTF_8;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.pcf.model.PcfConstants.CF_HOME;
import static io.harness.pcf.model.PcfConstants.PCF_ROUTE_PATH_SEPARATOR;
import static io.harness.pcf.model.PcfConstants.PIVOTAL_CLOUD_FOUNDRY_LOG_PREFIX;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.EMPTY;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.inject.Singleton;

import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.task.pcf.PcfManifestFileData;
import io.harness.exception.ExceptionUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
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
import org.cloudfoundry.operations.applications.GetApplicationRequest;
import org.cloudfoundry.operations.applications.ListApplicationTasksRequest;
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
import org.cloudfoundry.reactor.ConnectionContext;
import org.cloudfoundry.reactor.DefaultConnectionContext;
import org.cloudfoundry.reactor.TokenProvider;
import org.cloudfoundry.reactor.client.ReactorCloudFoundryClient;
import org.cloudfoundry.reactor.tokenprovider.PasswordGrantTokenProvider;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.stream.LogOutputStream;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.helpers.ext.pcf.request.PcfCreateApplicationRequestData;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

@Singleton
@Slf4j
public class PcfClientImpl implements PcfClient {
  public CloudFoundryOperationsWrapper getCloudFoundryOperationsWrapper(PcfRequestConfig pcfRequestConfig)
      throws PivotalClientApiException {
    try {
      ConnectionContext connectionContext = getConnectionContext(pcfRequestConfig.getEndpointUrl());
      CloudFoundryOperations cloudFoundryOperations =
          DefaultCloudFoundryOperations.builder()
              .cloudFoundryClient(getCloudFoundryClient(pcfRequestConfig, connectionContext))
              .organization(pcfRequestConfig.getOrgName())
              .space(pcfRequestConfig.getSpaceName())
              .build();

      return CloudFoundryOperationsWrapper.builder()
          .cloudFoundryOperations(cloudFoundryOperations)
          .connectionContext(connectionContext)
          .build();
    } catch (Exception e) {
      throw new PivotalClientApiException("Exception while creating CloudFoundryOperations: " + e.getMessage(), e);
    }
  }

  public CloudFoundryClient getCloudFoundryClient(
      PcfRequestConfig pcfRequestConfig, ConnectionContext connectionContext) throws PivotalClientApiException {
    return ReactorCloudFoundryClient.builder()
        .connectionContext(connectionContext)
        .tokenProvider(getTokenProvider(pcfRequestConfig.getUserName(), pcfRequestConfig.getPassword()))
        .build();
  }

  // Start Org apis
  @Override
  public List<OrganizationSummary> getOrganizations(PcfRequestConfig pcfRequestConfig)
      throws PivotalClientApiException, InterruptedException {
    logger.info(
        new StringBuilder().append(PIVOTAL_CLOUD_FOUNDRY_LOG_PREFIX).append("Fetching Organizations ").toString());

    List<OrganizationSummary> organizations = new ArrayList<>();

    AtomicBoolean exceptionOccured = new AtomicBoolean(false);
    StringBuilder errorBuilder = new StringBuilder();
    CountDownLatch latch = new CountDownLatch(1);
    try (CloudFoundryOperationsWrapper operationsWrapper = getCloudFoundryOperationsWrapper(pcfRequestConfig)) {
      operationsWrapper.getCloudFoundryOperations().organizations().list().subscribe(organizations::add, throwable -> {
        exceptionOccured.set(true);
        handleException(throwable, "getOrganizations", errorBuilder);
        latch.countDown();
      }, latch::countDown);

      waitTillCompletion(latch, pcfRequestConfig.getTimeOutIntervalInMins());
      if (exceptionOccured.get()) {
        throw new PivotalClientApiException(new StringBuilder()
                                                .append("Exception occurred while fetching Organizations")
                                                .append(", Error:" + errorBuilder.toString())
                                                .toString());
      }
      return organizations;
    }
  }

  public List<String> getSpacesForOrganization(PcfRequestConfig pcfRequestConfig)
      throws PivotalClientApiException, InterruptedException {
    List<OrganizationDetail> organizationDetails = new ArrayList<>();
    List<String> spaces = new ArrayList<>();
    logger.info(new StringBuilder().append(PIVOTAL_CLOUD_FOUNDRY_LOG_PREFIX).append("Fetching Spaces ").toString());

    CountDownLatch latch = new CountDownLatch(1);

    AtomicBoolean exceptionOccured = new AtomicBoolean(false);
    StringBuilder errorBuilder = new StringBuilder();
    try (CloudFoundryOperationsWrapper operationsWrapper = getCloudFoundryOperationsWrapper(pcfRequestConfig)) {
      operationsWrapper.getCloudFoundryOperations()
          .organizations()
          .get(OrganizationInfoRequest.builder().name(pcfRequestConfig.getOrgName()).build())
          .subscribe(organizationDetails::add, throwable -> {
            exceptionOccured.set(true);
            handleException(throwable, "getSpacesForOrganization", errorBuilder);
            latch.countDown();
          }, latch::countDown);

      waitTillCompletion(latch, pcfRequestConfig.getTimeOutIntervalInMins());

      if (exceptionOccured.get()) {
        throw new PivotalClientApiException(new StringBuilder()
                                                .append("Exception occurred while fetching Spaces")
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
  }
  // End Org apis

  // Start Application apis
  public List<ApplicationSummary> getApplications(PcfRequestConfig pcfRequestConfig)
      throws PivotalClientApiException, InterruptedException {
    logger.info(
        new StringBuilder().append(PIVOTAL_CLOUD_FOUNDRY_LOG_PREFIX).append("Fetching PCF Applications: ").toString());
    CountDownLatch latch = new CountDownLatch(1);
    List<ApplicationSummary> applicationSummaries = new ArrayList<>();

    AtomicBoolean exceptionOccured = new AtomicBoolean(false);
    StringBuilder errorBuilder = new StringBuilder();
    try (CloudFoundryOperationsWrapper operationsWrapper = getCloudFoundryOperationsWrapper(pcfRequestConfig)) {
      operationsWrapper.getCloudFoundryOperations().applications().list().subscribe(
          applicationSummaries::add, throwable -> {
            exceptionOccured.set(true);
            handleException(throwable, "getApplications", errorBuilder);
            latch.countDown();
          }, latch::countDown);

      waitTillCompletion(latch, pcfRequestConfig.getTimeOutIntervalInMins());

      if (exceptionOccured.get()) {
        throw new PivotalClientApiException(new StringBuilder()
                                                .append("Exception occurred while fetching Applications ")
                                                .append(", Error: " + errorBuilder.toString())
                                                .toString());
      }
      return applicationSummaries;
    }
  }

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

    AtomicBoolean exceptionOccured = new AtomicBoolean(false);
    StringBuilder errorBuilder = new StringBuilder();
    try (CloudFoundryOperationsWrapper operationsWrapper = getCloudFoundryOperationsWrapper(pcfRequestConfig)) {
      operationsWrapper.getCloudFoundryOperations()
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
                                                .append("Exception occurred Scaling Applications: ")
                                                .append(pcfRequestConfig.getApplicationName())
                                                .append(", to count: ")
                                                .append(pcfRequestConfig.getDesiredCount())
                                                .append(", Error:" + errorBuilder.toString())
                                                .toString());
      }
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

    try (CloudFoundryOperationsWrapper operationsWrapper = getCloudFoundryOperationsWrapper(pcfRequestConfig)) {
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

  public void pushApplicationUsingManifest(PcfCreateApplicationRequestData requestData,
      ExecutionLogCallback executionLogCallback) throws PivotalClientApiException, InterruptedException {
    PcfRequestConfig pcfRequestConfig = requestData.getPcfRequestConfig();

    logger.info(new StringBuilder()
                    .append(PIVOTAL_CLOUD_FOUNDRY_LOG_PREFIX)
                    .append("Creating Application: ")
                    .append(pcfRequestConfig.getApplicationName())
                    .toString());

    if (pcfRequestConfig.isUseCLIForAppCreate()) {
      logger.info("Using CLI to create application");
      performCfPushUsingCli(requestData, executionLogCallback);
      return;
    } else {
      executionLogCallback.saveExecutionLog(
          "Using SDK to create application, Deprecated... Please enable flag: USE_PCF_CLI");
      Path path = Paths.get(requestData.getManifestFilePath());
      pushUsingPcfSdk(pcfRequestConfig, path);
    }
  }

  @VisibleForTesting
  void pushUsingPcfSdk(PcfRequestConfig pcfRequestConfig, Path path)
      throws PivotalClientApiException, InterruptedException {
    List<ApplicationManifest> applicationManifests = ApplicationManifestUtils.read(path);

    ApplicationManifest applicationManifest = applicationManifests.get(0);
    applicationManifest = InitializeApplicationManifest(applicationManifest, pcfRequestConfig);
    AtomicBoolean exceptionOccured = new AtomicBoolean(false);
    StringBuilder errorBuilder = new StringBuilder();
    CountDownLatch latch = new CountDownLatch(1);

    try (CloudFoundryOperationsWrapper operationsWrapper = getCloudFoundryOperationsWrapper(pcfRequestConfig)) {
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

  @VisibleForTesting
  void performCfPushUsingCli(PcfCreateApplicationRequestData requestData, ExecutionLogCallback executionLogCallback)
      throws PivotalClientApiException {
    // Create a new filePath.
    PcfRequestConfig pcfRequestConfig = requestData.getPcfRequestConfig();

    int exitCode = 1;
    try {
      String finalFilePath = requestData.getManifestFilePath().replace(".yml", "_1.yml");
      FileUtils.writeStringToFile(new File(finalFilePath), requestData.getFinalManifestYaml(), UTF_8);
      logManifestFile(finalFilePath, executionLogCallback);

      executionLogCallback.saveExecutionLog("# CF_HOME value: " + requestData.getConfigPathVar());
      boolean loginSuccessful = doLogin(pcfRequestConfig, executionLogCallback, requestData.getConfigPathVar());
      if (loginSuccessful) {
        exitCode = doCfPush(pcfRequestConfig, executionLogCallback, finalFilePath, requestData);
      }
    } catch (Exception e) {
      throw new PivotalClientApiException(new StringBuilder()
                                              .append("Exception occurred while creating Application: ")
                                              .append(pcfRequestConfig.getApplicationName())
                                              .append(", Error: App creation process Failed :  ")
                                              .append(e)
                                              .toString());
    }

    if (exitCode != 0) {
      throw new PivotalClientApiException(new StringBuilder()
                                              .append("Exception occured while creating Application: ")
                                              .append(pcfRequestConfig.getApplicationName())
                                              .append(", Error: App creation process ExitCode:  ")
                                              .append(exitCode)
                                              .toString());
    }
  }

  private int doCfPush(PcfRequestConfig pcfRequestConfig, ExecutionLogCallback executionLogCallback,
      String finalFilePath, PcfCreateApplicationRequestData requestData)
      throws InterruptedException, TimeoutException, IOException {
    executionLogCallback.saveExecutionLog("# Performing \"cf push\"");
    String command = constructCfPushCommand(requestData, finalFilePath);
    ProcessExecutor processExecutor = new ProcessExecutor()
                                          .timeout(pcfRequestConfig.getTimeOutIntervalInMins(), TimeUnit.MINUTES)
                                          .command("/bin/sh", "-c", command)
                                          .readOutput(true)
                                          .environment(getEnvironmentMapForPcfExecutor(requestData.getConfigPathVar()))
                                          .redirectOutput(new LogOutputStream() {
                                            @Override
                                            protected void processLine(String line) {
                                              executionLogCallback.saveExecutionLog(line);
                                            }
                                          });
    ProcessResult processResult = processExecutor.execute();
    return processResult.getExitValue();
  }

  private String constructCfPushCommand(PcfCreateApplicationRequestData requestData, String finalFilePath) {
    StringBuilder builder = new StringBuilder(128).append("cf push -f ").append(finalFilePath);
    if (!requestData.isVarsYmlFilePresent()) {
      return builder.toString();
    }

    PcfManifestFileData pcfManifestFileData = requestData.getPcfManifestFileData();
    if (isNotEmpty(pcfManifestFileData.getVarFiles())) {
      pcfManifestFileData.getVarFiles().forEach(varsFile -> {
        if (varsFile != null) {
          builder.append(" --vars-file ").append(varsFile.getAbsoluteFile());
        }
      });
    }

    return builder.toString();
  }

  private Map<String, String> getEnvironmentMapForPcfExecutor(String configPathVar) {
    Map<String, String> map = new HashMap();
    map.put(CF_HOME, configPathVar);
    return map;
  }

  boolean doLogin(PcfRequestConfig pcfRequestConfig, ExecutionLogCallback executionLogCallback, String configPathVar)
      throws IOException, InterruptedException, TimeoutException {
    executionLogCallback.saveExecutionLog("# Performing \"login\"");
    String password = handlePwdForSpecialCharsForShell(pcfRequestConfig.getPassword());
    ProcessExecutor processExecutor = new ProcessExecutor()
                                          .timeout(1, TimeUnit.MINUTES)
                                          .command("/bin/sh", "-c",
                                              new StringBuilder(128)
                                                  .append("cf login -a ")
                                                  .append(pcfRequestConfig.getEndpointUrl())
                                                  .append(" -o ")
                                                  .append(pcfRequestConfig.getOrgName())
                                                  .append(" -s ")
                                                  .append(pcfRequestConfig.getSpaceName())
                                                  .append(" --skip-ssl-validation -u ")
                                                  .append(pcfRequestConfig.getUserName())
                                                  .append(" -p ")
                                                  .append(password)
                                                  .toString())
                                          .readOutput(true)
                                          .environment(getEnvironmentMapForPcfExecutor(configPathVar))
                                          .redirectOutput(new LogOutputStream() {
                                            @Override
                                            protected void processLine(String line) {
                                              executionLogCallback.saveExecutionLog(line);
                                            }
                                          });
    ProcessResult processResult = processExecutor.execute();
    executionLogCallback.saveExecutionLog(processResult.getExitValue() == 0 ? "# Login Successful" : "# Login Failed");
    return processResult.getExitValue() == 0;
  }

  String handlePwdForSpecialCharsForShell(String password) {
    password = password.replace("\"", "\\\"");
    return "\"" + password + "\"";
  }

  private void logManifestFile(String finalFilePath, ExecutionLogCallback executionLogCallback) {
    String content;
    try {
      content = new String(Files.readAllBytes(Paths.get(finalFilePath)), Charsets.UTF_8);
      executionLogCallback.saveExecutionLog(
          new StringBuilder(128).append("# Manifest File Content: \n").append(content).append('\n').toString());
      logger.info(new StringBuilder(128)
                      .append("Manifest File at Path: ")
                      .append(finalFilePath)
                      .append(", contents are \n")
                      .append(content)
                      .toString());
    } catch (Exception e) {
      logger.warn("Failed to log manifest file contents at path : " + finalFilePath);
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
        .path(applicationManifest.getPath())
        .instances(0)
        .healthCheckType(applicationManifest.getHealthCheckType())
        .healthCheckHttpEndpoint(applicationManifest.getHealthCheckHttpEndpoint())
        .stack(applicationManifest.getStack())
        .timeout(applicationManifest.getTimeout())
        .domains(applicationManifest.getDomains())
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

      // '_' in routemap is not allowed, PCF lets us create route but while accessing it, fails
      appPrefix = appPrefix.replaceAll("__", "-");
      appPrefix = appPrefix.replaceAll("_", "-");

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
    try (CloudFoundryOperationsWrapper operationsWrapper = getCloudFoundryOperationsWrapper(pcfRequestConfig)) {
      operationsWrapper.getCloudFoundryOperations()
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
                                                .append("Exception occurred while stopping Application: ")
                                                .append(pcfRequestConfig.getApplicationName())
                                                .append(", Error: " + errorBuilder.toString())
                                                .toString());
      }
    }
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
    try (CloudFoundryOperationsWrapper operationsWrapper = getCloudFoundryOperationsWrapper(pcfRequestConfig)) {
      operationsWrapper.getCloudFoundryOperations()
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
                                                .append("Exception occurred while  getting application: ")
                                                .append(pcfRequestConfig.getApplicationName())
                                                .append(", Error: " + errorBuilder.toString())
                                                .toString());
      }

      return applicationDetails.size() > 0 ? applicationDetails.get(0) : null;
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
    try (CloudFoundryOperationsWrapper operationsWrapper = getCloudFoundryOperationsWrapper(pcfRequestConfig)) {
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
        throw new PivotalClientApiException(new StringBuilder()
                                                .append("Exception occurred while deleting application: ")
                                                .append(pcfRequestConfig.getApplicationName())
                                                .append(", Error: " + errorBuilder.toString())
                                                .toString());
      }
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
    try (CloudFoundryOperationsWrapper operationsWrapper = getCloudFoundryOperationsWrapper(pcfRequestConfig)) {
      operationsWrapper.getCloudFoundryOperations()
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
                                                .append("Exception occurred while starting application: ")
                                                .append(pcfRequestConfig.getApplicationName())
                                                .append(", Error: " + errorBuilder.toString())
                                                .toString());
      }
    }
  }
  // End Application apis

  // Start Rout Map Apis

  /**
   * Get Route Application by entire route path
   */

  public List<String> getRoutesForSpace(PcfRequestConfig pcfRequestConfig)
      throws PivotalClientApiException, InterruptedException {
    List<Route> routes = getAllRoutesForSpace(pcfRequestConfig);
    if (!CollectionUtils.isEmpty(routes)) {
      return routes.stream().map(route -> getPathFromRouteMap(route)).collect(toList());
    }

    return Collections.EMPTY_LIST;
  }

  public List<Route> getRouteMapsByNames(List<String> paths, PcfRequestConfig pcfRequestConfig)
      throws PivotalClientApiException, InterruptedException {
    if (isEmpty(paths)) {
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

    try (CloudFoundryOperationsWrapper operationsWrapper = getCloudFoundryOperationsWrapper(pcfRequestConfig)) {
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

  public List<Domain> getAllDomainsForSpace(PcfRequestConfig pcfRequestConfig)
      throws PivotalClientApiException, InterruptedException {
    logger.info(new StringBuilder()
                    .append(PIVOTAL_CLOUD_FOUNDRY_LOG_PREFIX)
                    .append("Getting Domains for Space: ")
                    .append(pcfRequestConfig.getSpaceName())
                    .toString());

    CountDownLatch latch = new CountDownLatch(1);
    AtomicBoolean exceptionOccured = new AtomicBoolean(false);
    StringBuilder errorBuilder = new StringBuilder();
    List<Domain> domains = new ArrayList<>();

    try (CloudFoundryOperationsWrapper operationsWrapper = getCloudFoundryOperationsWrapper(pcfRequestConfig)) {
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

  public void createRouteMap(PcfRequestConfig pcfRequestConfig, String host, String domain, String path,
      boolean tcpRoute, boolean useRandomPort, Integer port) throws PivotalClientApiException, InterruptedException {
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

    // create routeMap
    final CountDownLatch latch2 = new CountDownLatch(1);
    AtomicBoolean exceptionOccured = new AtomicBoolean(false);
    StringBuilder errorBuilder = new StringBuilder();

    path = StringUtils.isBlank(path) ? null : path;
    errorBuilder.setLength(0);

    CreateRouteRequest.Builder createRouteRequestBuilder =
        getCreateRouteRequest(pcfRequestConfig, host, domain, path, tcpRoute, useRandomPort, port);

    try (CloudFoundryOperationsWrapper operationsWrapper = getCloudFoundryOperationsWrapper(pcfRequestConfig)) {
      operationsWrapper.getCloudFoundryOperations()
          .routes()
          .create(createRouteRequestBuilder.build())
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

  public Optional<Route> getRouteMap(PcfRequestConfig pcfRequestConfig, String route)
      throws PivotalClientApiException, InterruptedException {
    if (StringUtils.isBlank(route)) {
      throw new PivotalClientApiException(
          PIVOTAL_CLOUD_FOUNDRY_LOG_PREFIX + "Route Can Not Be Blank When Fetching RouteMap");
    }

    List<Route> routes = getRouteMapsByNames(Arrays.asList(route), pcfRequestConfig);
    if (EmptyPredicate.isNotEmpty(routes)) {
      return Optional.of(routes.get(0));
    }

    return Optional.empty();
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
    List<String> routesNeedToBeCreated = findRoutesNeedToBeCreated(routes, routeList);

    if (isNotEmpty(routesNeedToBeCreated)) {
      List<Domain> allDomainsForSpace = getAllDomainsForSpace(pcfRequestConfig);
      Set<String> domainNames = allDomainsForSpace.stream().map(domain -> domain.getName()).collect(toSet());
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
        validRoute = true;
        domainNameUsed = domainName;
        break;
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
      path = routeToCreate.substring(indexForPath + 1);
    }

    createRouteMap(pcfRequestConfig, hostName, domainNameUsed, path, false, false, null);
  }

  @VisibleForTesting
  List<String> findRoutesNeedToBeCreated(List<String> routes, List<Route> routeList) {
    Set<String> routesExisting = routeList.stream().map(route -> getPathFromRouteMap(route)).collect(toSet());
    return routes.stream().filter(route -> !routesExisting.contains(route)).collect(toList());
  }

  @VisibleForTesting
  String getPathFromRouteMap(Route route) {
    return new StringBuilder()
        .append(StringUtils.isBlank(route.getHost()) ? EMPTY : route.getHost() + ".")
        .append(route.getDomain())
        .append(StringUtils.isBlank(route.getPath()) ? EMPTY : "/" + route.getPath())
        .append(StringUtils.isBlank(route.getPort()) ? EMPTY : ":" + Integer.parseInt(route.getPort()))
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

    try (CloudFoundryOperationsWrapper operationsWrapper = getCloudFoundryOperationsWrapper(pcfRequestConfig)) {
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
    try (CloudFoundryOperationsWrapper operationsWrapper = getCloudFoundryOperationsWrapper(pcfRequestConfig)) {
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

  // End Route Map Apis

  private TokenProvider getTokenProvider(String username, String password) throws PivotalClientApiException {
    try {
      return PasswordGrantTokenProvider.builder().username(username).password(password).build();
    } catch (Exception t) {
      throw new PivotalClientApiException(ExceptionUtils.getMessage(t));
    }
  }

  private ConnectionContext getConnectionContext(String endPointUrl) throws PivotalClientApiException {
    try {
      return DefaultConnectionContext.builder().apiHost(endPointUrl).skipSslValidation(false).build();
    } catch (Exception t) {
      throw new PivotalClientApiException(ExceptionUtils.getMessage(t));
    }
  }

  private void handleException(Throwable t, String apiName, StringBuilder errorBuilder) {
    logger.error(PIVOTAL_CLOUD_FOUNDRY_LOG_PREFIX + "Exception Occured while executing PCF api: " + apiName, t);
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