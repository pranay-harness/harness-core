package software.wings.helpers.ext.pcf;

import static com.google.common.base.Charsets.UTF_8;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.pcf.model.PcfConstants.APP_TOKEN;
import static io.harness.pcf.model.PcfConstants.CF_COMMAND_FOR_APP_LOG_TAILING;
import static io.harness.pcf.model.PcfConstants.CF_COMMAND_FOR_CHECKING_APP_AUTOSCALAR_BINDING;
import static io.harness.pcf.model.PcfConstants.CF_COMMAND_FOR_CHECKING_AUTOSCALAR;
import static io.harness.pcf.model.PcfConstants.CF_HOME;
import static io.harness.pcf.model.PcfConstants.CF_PASSWORD;
import static io.harness.pcf.model.PcfConstants.CF_PLUGIN_HOME;
import static io.harness.pcf.model.PcfConstants.CF_USERNAME;
import static io.harness.pcf.model.PcfConstants.CONFIGURE_AUTOSCALING;
import static io.harness.pcf.model.PcfConstants.DISABLE_AUTOSCALING;
import static io.harness.pcf.model.PcfConstants.ENABLE_AUTOSCALING;
import static io.harness.pcf.model.PcfConstants.PCF_ROUTE_PATH_SEPARATOR;
import static io.harness.pcf.model.PcfConstants.PCF_ROUTE_PATH_SEPARATOR_CHAR;
import static io.harness.pcf.model.PcfConstants.PCF_ROUTE_PORT_SEPARATOR;
import static io.harness.pcf.model.PcfConstants.PIVOTAL_CLOUD_FOUNDRY_CLIENT_EXCEPTION;
import static io.harness.pcf.model.PcfConstants.PIVOTAL_CLOUD_FOUNDRY_LOG_PREFIX;
import static io.harness.pcf.model.PcfConstants.SYS_VAR_CF_PLUGIN_HOME;
import static io.harness.pcf.model.PcfRouteType.PCF_ROUTE_TYPE_HTTP;
import static io.harness.pcf.model.PcfRouteType.PCF_ROUTE_TYPE_TCP;
import static java.lang.String.format;
import static java.lang.String.join;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static software.wings.beans.Log.LogColor.Red;
import static software.wings.beans.Log.LogLevel.ERROR;
import static software.wings.beans.Log.color;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.inject.Singleton;

import io.harness.delegate.task.pcf.PcfManifestFileData;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.pcf.model.PcfRouteInfo;
import io.harness.pcf.model.PcfRouteInfo.PcfRouteInfoBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.doppler.LogMessage;
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
import org.cloudfoundry.reactor.ConnectionContext;
import org.cloudfoundry.reactor.DefaultConnectionContext;
import org.cloudfoundry.reactor.TokenProvider;
import org.cloudfoundry.reactor.client.ReactorCloudFoundryClient;
import org.cloudfoundry.reactor.tokenprovider.PasswordGrantTokenProvider;
import org.jetbrains.annotations.NotNull;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.StartedProcess;
import org.zeroturnaround.exec.stream.LogOutputStream;
import software.wings.beans.Log.LogWeight;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.helpers.ext.pcf.request.PcfAppAutoscalarRequestData;
import software.wings.helpers.ext.pcf.request.PcfCreateApplicationRequestData;
import software.wings.helpers.ext.pcf.request.PcfRunPluginScriptRequestData;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
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
import java.util.stream.Collectors;

@Singleton
@Slf4j
public class PcfClientImpl implements PcfClient {
  public static final String BIN_SH = "/bin/sh";

  public CloudFoundryOperationsWrapper getCloudFoundryOperationsWrapper(PcfRequestConfig pcfRequestConfig)
      throws PivotalClientApiException {
    try {
      ConnectionContext connectionContext = getConnectionContext(pcfRequestConfig);
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

  @Override
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

  @Override
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
  @Override
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

  @Override
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

  @Override
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

  @Override
  public void pushApplicationUsingManifest(PcfCreateApplicationRequestData requestData,
      ExecutionLogCallback executionLogCallback) throws PivotalClientApiException, InterruptedException {
    PcfRequestConfig pcfRequestConfig = requestData.getPcfRequestConfig();

    logger.info(new StringBuilder()
                    .append(PIVOTAL_CLOUD_FOUNDRY_LOG_PREFIX)
                    .append("Creating Application: ")
                    .append(pcfRequestConfig.getApplicationName())
                    .toString());

    if (pcfRequestConfig.isUseCFCLI()) {
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
      boolean loginSuccessful = true;
      if (!requestData.getPcfRequestConfig().isLoggedin()) {
        loginSuccessful = doLogin(pcfRequestConfig, executionLogCallback, requestData.getConfigPathVar());
      }

      if (loginSuccessful) {
        exitCode = doCfPush(pcfRequestConfig, executionLogCallback, finalFilePath, requestData);
      }
    } catch (Exception e) {
      throw new PivotalClientApiException(new StringBuilder()
                                              .append("Exception occurred while creating Application: ")
                                              .append(pcfRequestConfig.getApplicationName())
                                              .append(", Error: App creation process Failed :  ")
                                              .toString(),
          e);
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

  @Override
  public boolean checkIfAppAutoscalarInstalled() throws PivotalClientApiException {
    boolean appAutoscalarInstalled;
    Map<String, String> map = new HashMap();
    map.put(CF_PLUGIN_HOME, resolvePcfPluginHome());
    ProcessExecutor processExecutor = createExecutorForAutoscalarPluginCheck(map);

    try {
      ProcessResult processResult = processExecutor.execute();
      appAutoscalarInstalled = isNotEmpty(processResult.outputUTF8());
    } catch (InterruptedException e) {
      throw new PivotalClientApiException("check for App Autoscalar plugin failed", e);
    } catch (Exception e) {
      throw new PivotalClientApiException("check for AppAutoscalar plugin failed", e);
    }

    return appAutoscalarInstalled;
  }

  @Override
  public void performConfigureAutoscalar(PcfAppAutoscalarRequestData appAutoscalarRequestData,
      ExecutionLogCallback executionLogCallback) throws PivotalClientApiException {
    int exitCode = 1;

    try {
      // First login
      boolean loginSuccessful = logInForAppAutoscalarCliCommand(appAutoscalarRequestData, executionLogCallback);

      if (loginSuccessful) {
        logManifestFile(appAutoscalarRequestData.getAutoscalarFilePath(), executionLogCallback);

        // perform configure-autoscalar command
        ProcessExecutor processExecutor = createProccessExecutorForPcfTask(appAutoscalarRequestData.getTimeoutInMins(),
            getConfigureAutosaclarCfCliCommand(appAutoscalarRequestData),
            getAppAutoscalarEnvMapForCustomPlugin(appAutoscalarRequestData), executionLogCallback);
        exitCode = processExecutor.execute().getExitValue();
      }
    } catch (InterruptedException e) {
      execeptionForAutoscalarConfigureFailure(appAutoscalarRequestData.getApplicationName(), e);
    } catch (Exception e) {
      execeptionForAutoscalarConfigureFailure(appAutoscalarRequestData.getApplicationName(), e);
    }

    if (exitCode != 0) {
      throw new PivotalClientApiException(
          new StringBuilder()
              .append("Exception occurred while Configuring autoscalar for Application: ")
              .append(appAutoscalarRequestData.getApplicationName())
              .append(", Error: App Autoscalar configuration Failed :  ")
              .append(exitCode)
              .toString());
    }
  }

  @Override
  @VisibleForTesting
  public void changeAutoscalarState(PcfAppAutoscalarRequestData appAutoscalarRequestData,
      ExecutionLogCallback executionLogCallback, boolean enable) throws PivotalClientApiException {
    int exitCode = 1;

    try {
      // First login
      boolean loginSuccessful = logInForAppAutoscalarCliCommand(appAutoscalarRequestData, executionLogCallback);

      if (loginSuccessful) {
        // perform enable/disable autoscalar
        String completeCommand = generateChangeAutoscalarStateCommand(appAutoscalarRequestData, enable);

        ProcessExecutor processExecutor = createProccessExecutorForPcfTask(appAutoscalarRequestData.getTimeoutInMins(),
            completeCommand, getAppAutoscalarEnvMapForCustomPlugin(appAutoscalarRequestData), executionLogCallback);
        exitCode = processExecutor.execute().getExitValue();
      }
    } catch (InterruptedException e) {
      exceptionForAutoscalarStateChangeFailure(appAutoscalarRequestData.getApplicationName(), enable, e);
      return;
    } catch (Exception e) {
      exceptionForAutoscalarStateChangeFailure(appAutoscalarRequestData.getApplicationName(), enable, e);
    }
    if (exitCode != 0) {
      throw new PivotalClientApiException(new StringBuilder()
                                              .append("Exception occurred for Application: ")
                                              .append(appAutoscalarRequestData.getApplicationName())
                                              .append(", for action: ")
                                              .append(enable ? ENABLE_AUTOSCALING : DISABLE_AUTOSCALING)
                                              .append(exitCode)
                                              .toString());
    }
  }

  @Override
  public boolean checkIfAppHasAutoscalarAttached(PcfAppAutoscalarRequestData appAutoscalarRequestData,
      ExecutionLogCallback executionLogCallback) throws PivotalClientApiException {
    boolean appAutoscalarInstalled = false;
    executionLogCallback.saveExecutionLog("\n# Checking if Application: "
        + appAutoscalarRequestData.getApplicationName() + " has Autoscalar Bound to it");

    try {
      boolean loginSuccessful = logInForAppAutoscalarCliCommand(appAutoscalarRequestData, executionLogCallback);
      if (loginSuccessful) {
        ProcessExecutor processExecutor = createProccessExecutorForPcfTask(1,
            CF_COMMAND_FOR_CHECKING_APP_AUTOSCALAR_BINDING.replace(
                APP_TOKEN, appAutoscalarRequestData.getApplicationGuid()),
            getAppAutoscalarEnvMapForCustomPlugin(appAutoscalarRequestData), executionLogCallback);

        ProcessResult processResult = processExecutor.execute();
        appAutoscalarInstalled = isNotEmpty(processResult.outputUTF8());
      }
    } catch (InterruptedException e) {
      throw new PivotalClientApiException("check for App Autoscalar Binding failed", e);
    } catch (Exception e) {
      throw new PivotalClientApiException("check for AppAutoscalar Binding failed", e);
    }

    return appAutoscalarInstalled;
  }

  @Override
  public boolean checkIfAppHasAutoscalarWithExpectedState(PcfAppAutoscalarRequestData appAutoscalarRequestData,
      ExecutionLogCallback logCallback) throws PivotalClientApiException {
    boolean appAutoscalarInExpectedState = false;
    logCallback.saveExecutionLog("\n# Checking if Application: " + appAutoscalarRequestData.getApplicationName()
        + " has Autoscalar Bound to it");

    try {
      boolean loginSuccessful = logInForAppAutoscalarCliCommand(appAutoscalarRequestData, logCallback);
      if (loginSuccessful) {
        ProcessExecutor executor = createProccessExecutorForPcfTask(1,
            CF_COMMAND_FOR_CHECKING_APP_AUTOSCALAR_BINDING.replace(
                APP_TOKEN, appAutoscalarRequestData.getApplicationGuid()),
            getAppAutoscalarEnvMapForCustomPlugin(appAutoscalarRequestData), logCallback);

        ProcessResult processResult = executor.execute();
        String output = processResult.outputUTF8();
        if (isEmpty(output)) {
          logCallback.saveExecutionLog("\n# No App Autoscalar Bound to App");
        } else {
          logCallback.saveExecutionLog("# App Autoscalar Current State: " + output);
          String status = appAutoscalarRequestData.isExpectedEnabled() ? " true " : " false ";
          if (output.contains(status)) {
            appAutoscalarInExpectedState = true;
          }
        }
      }
    } catch (InterruptedException e) {
      throw new PivotalClientApiException("check for App Autoscalar Binding failed", e);
    } catch (Exception e) {
      throw new PivotalClientApiException("check for AppAutoscalar Binding failed", e);
    }

    return appAutoscalarInExpectedState;
  }

  @VisibleForTesting
  ProcessExecutor createExecutorForAutoscalarPluginCheck(Map<String, String> map) {
    return new ProcessExecutor()
        .timeout(1, TimeUnit.MINUTES)
        .command(BIN_SH, "-c", CF_COMMAND_FOR_CHECKING_AUTOSCALAR)
        .readOutput(true)
        .environment(map);
  }

  @VisibleForTesting
  String generateChangeAutoscalarStateCommand(PcfAppAutoscalarRequestData appAutoscalarRequestData, boolean enable) {
    String commandName = enable ? ENABLE_AUTOSCALING : DISABLE_AUTOSCALING;
    return new StringBuilder(128)
        .append("cf ")
        .append(commandName)
        .append(' ')
        .append(appAutoscalarRequestData.getApplicationName())
        .toString();
  }

  @VisibleForTesting
  boolean logInForAppAutoscalarCliCommand(PcfAppAutoscalarRequestData appAutoscalarRequestData,
      ExecutionLogCallback executionLogCallback) throws InterruptedException, TimeoutException, IOException {
    boolean loginSuccessful = true;
    if (!appAutoscalarRequestData.getPcfRequestConfig().isLoggedin()) {
      loginSuccessful = doLogin(appAutoscalarRequestData.getPcfRequestConfig(), executionLogCallback,
          appAutoscalarRequestData.getConfigPathVar());
    }
    appAutoscalarRequestData.getPcfRequestConfig().setLoggedin(loginSuccessful);
    return loginSuccessful;
  }

  @VisibleForTesting
  Map<String, String> getAppAutoscalarEnvMapForCustomPlugin(PcfAppAutoscalarRequestData appAutoscalarRequestData) {
    Map<String, String> environmentMapForPcfExecutor =
        getEnvironmentMapForPcfExecutor(appAutoscalarRequestData.getConfigPathVar());
    // set CUSTOM_PLUGIN_HOME, NEEDED FOR AUTO-SCALAR PLUIN
    environmentMapForPcfExecutor.put(CF_PLUGIN_HOME, resolvePcfPluginHome());
    return environmentMapForPcfExecutor;
  }

  @VisibleForTesting
  ProcessExecutor createProccessExecutorForPcfTask(
      long timeout, String command, Map<String, String> env, ExecutionLogCallback executionLogCallback) {
    return new ProcessExecutor()
        .timeout(timeout, TimeUnit.MINUTES)
        .command(BIN_SH, "-c", command)
        .readOutput(true)
        .environment(env)
        .redirectOutput(new LogOutputStream() {
          @Override
          protected void processLine(String line) {
            executionLogCallback.saveExecutionLog(line);
          }
        });
  }

  private void exceptionForAutoscalarStateChangeFailure(String appName, boolean enable, Exception e)
      throws PivotalClientApiException {
    throw new PivotalClientApiException(new StringBuilder()
                                            .append("Exception occurred for Application: ")
                                            .append(appName)
                                            .append(", for action: ")
                                            .append(enable ? ENABLE_AUTOSCALING : DISABLE_AUTOSCALING)
                                            .append(", Error: ")
                                            .append(e)
                                            .toString(),
        e);
  }

  private void execeptionForAutoscalarConfigureFailure(String applicationName, Exception e)
      throws PivotalClientApiException {
    throw new PivotalClientApiException(new StringBuilder(128)
                                            .append("Exception occurred while Configuring autoscalar for Application: ")
                                            .append(applicationName)
                                            .append(", Error: ")
                                            .append(e)
                                            .toString(),
        e);
  }

  @NotNull
  private String getConfigureAutosaclarCfCliCommand(PcfAppAutoscalarRequestData appAutoscalarRequestData) {
    return new StringBuilder(128)
        .append(CONFIGURE_AUTOSCALING)
        .append(' ')
        .append(appAutoscalarRequestData.getApplicationName())
        .append(' ')
        .append(appAutoscalarRequestData.getAutoscalarFilePath())
        .toString();
  }

  @Override
  public String resolvePcfPluginHome() {
    // look into java system variable
    final String sysVarPluginHome = System.getProperty(SYS_VAR_CF_PLUGIN_HOME);
    if (isNotEmpty(sysVarPluginHome)) {
      return sysVarPluginHome.trim();
    }
    // env variable
    final String envVarPluginHome = System.getenv(CF_PLUGIN_HOME);
    if (isNotEmpty(envVarPluginHome)) {
      return envVarPluginHome.trim();
    }
    // default is user home
    return System.getProperty("user.home");
  }

  @Override
  public List<LogMessage> getRecentLogs(PcfRequestConfig pcfRequestConfig, long logsAfterTsNs)
      throws PivotalClientApiException {
    try (CloudFoundryOperationsWrapper operationsWrapper = getCloudFoundryOperationsWrapper(pcfRequestConfig)) {
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

  private int doCfPush(PcfRequestConfig pcfRequestConfig, ExecutionLogCallback executionLogCallback,
      String finalFilePath, PcfCreateApplicationRequestData requestData)
      throws InterruptedException, TimeoutException, IOException {
    executionLogCallback.saveExecutionLog("# Performing \"cf push\"");
    String command = constructCfPushCommand(requestData, finalFilePath);
    ProcessExecutor processExecutor = new ProcessExecutor()
                                          .timeout(pcfRequestConfig.getTimeOutIntervalInMins(), TimeUnit.MINUTES)
                                          .command(BIN_SH, "-c", command)
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

  @VisibleForTesting
  Map<String, String> getEnvironmentMapForPcfExecutor(String configPathVar) {
    return getEnvironmentMapForPcfExecutor(configPathVar, null);
  }

  private Map<String, String> getEnvironmentMapForPcfExecutor(String configPathVar, String pluginHomeAbsPath) {
    final Map<String, String> map = new HashMap<>();
    map.put(CF_HOME, configPathVar);
    if (isNotEmpty(pluginHomeAbsPath)) {
      map.put(CF_PLUGIN_HOME, pluginHomeAbsPath);
    }
    return map;
  }

  int executeCommand(String command, Map<String, String> env, ExecutionLogCallback logCallback)
      throws IOException, InterruptedException, TimeoutException {
    logCallback.saveExecutionLog(format("Executing command: [%s]", command));
    ProcessExecutor executor = new ProcessExecutor()
                                   .timeout(5, TimeUnit.MINUTES)
                                   .command(BIN_SH, "-c", command)
                                   .readOutput(true)
                                   .environment(env)
                                   .redirectOutput(new LogOutputStream() {
                                     @Override
                                     protected void processLine(String line) {
                                       logCallback.saveExecutionLog(line);
                                     }
                                   });
    ProcessResult result = executor.execute();
    return result.getExitValue();
  }

  boolean doLogin(PcfRequestConfig pcfRequestConfig, ExecutionLogCallback executionLogCallback, String configPathVar)
      throws IOException, InterruptedException, TimeoutException {
    executionLogCallback.saveExecutionLog("# Performing \"login\"");

    String command;
    int exitValue;
    Map<String, String> env = getEnvironmentMapForPcfExecutor(configPathVar);

    command = format("cf api %s --skip-ssl-validation", pcfRequestConfig.getEndpointUrl());
    exitValue = executeCommand(command, env, executionLogCallback);

    if (exitValue == 0) {
      Map<String, String> envForAuth = new HashMap<>(env);
      envForAuth.put(CF_USERNAME, pcfRequestConfig.getUserName());
      envForAuth.put(CF_PASSWORD, pcfRequestConfig.getPassword());
      command = "cf auth";
      exitValue = executeCommand(command, envForAuth, executionLogCallback);
    }

    if (exitValue == 0) {
      command = format("cf target -o %s -s %s", pcfRequestConfig.getOrgName(), pcfRequestConfig.getSpaceName());
      exitValue = executeCommand(command, env, executionLogCallback);
    }

    executionLogCallback.saveExecutionLog(exitValue == 0 ? "# Login Successful" : "# Login Failed");
    return exitValue == 0;
  }

  String handlePwdForSpecialCharsForShell(String password) {
    // Wrapped around single quotes to by pass '$' characted.
    // Single quotes cannot be part of the password due to limitations on escaping them.
    return "'" + password + "'";
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

  private void addRouteMapsToManifest(PcfRequestConfig pcfRequestConfig, Builder builder) {
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

  @Override
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

  @Override
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

  @Override
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

  @Override
  public List<String> getRoutesForSpace(PcfRequestConfig pcfRequestConfig)
      throws PivotalClientApiException, InterruptedException {
    List<Route> routes = getAllRoutesForSpace(pcfRequestConfig);
    if (!CollectionUtils.isEmpty(routes)) {
      return routes.stream().map(this ::getPathFromRouteMap).collect(toList());
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

  @Override
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

  @Override
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
  public void runPcfPluginScript(PcfRunPluginScriptRequestData pcfRunPluginScriptRequestData,
      ExecutionLogCallback executionLogCallback) throws PivotalClientApiException {
    PcfRequestConfig pcfRequestConfig = pcfRunPluginScriptRequestData.getPcfRequestConfig();
    int exitCode = -1;
    try {
      executionLogCallback.saveExecutionLog("# Final Script to execute :");
      executionLogCallback.saveExecutionLog("# ------------------------------------------ \n");
      executionLogCallback.saveExecutionLog(pcfRunPluginScriptRequestData.getFinalScriptString());
      executionLogCallback.saveExecutionLog("\n# ------------------------------------------ ");
      executionLogCallback.saveExecutionLog(
          "\n# CF_HOME value: " + pcfRunPluginScriptRequestData.getWorkingDirectory());
      final String pcfPluginHome = resolvePcfPluginHome();
      executionLogCallback.saveExecutionLog("# CF_PLUGIN_HOME value: " + pcfPluginHome);
      boolean loginSuccessful =
          doLogin(pcfRequestConfig, executionLogCallback, pcfRunPluginScriptRequestData.getWorkingDirectory());
      if (loginSuccessful) {
        executionLogCallback.saveExecutionLog("# Executing pcf plugin script :");

        ProcessExecutor processExecutor =
            new ProcessExecutor()
                .timeout(pcfRequestConfig.getTimeOutIntervalInMins(), TimeUnit.MINUTES)
                .command(BIN_SH, "-c", pcfRunPluginScriptRequestData.getFinalScriptString())
                .readOutput(true)
                .environment(
                    getEnvironmentMapForPcfExecutor(pcfRunPluginScriptRequestData.getWorkingDirectory(), pcfPluginHome))
                .redirectOutput(new LogOutputStream() {
                  @Override
                  protected void processLine(String line) {
                    executionLogCallback.saveExecutionLog(line);
                  }
                });
        ProcessResult processResult = runProcessExecutor(processExecutor);
        executionLogCallback.saveExecutionLog("# Exit value =" + processResult.getExitValue());
        exitCode = processResult.getExitValue();
      }
    } catch (Exception e) {
      throw new PivotalClientApiException("Exception occurred while running pcf plugin script", e);
    }
    if (exitCode != 0) {
      throw new PivotalClientApiException("Exception occurred while running pcf plugin script"
          + ", Error: Plugin Script process ExitCode:  " + exitCode);
    }
  }

  @VisibleForTesting
  ProcessResult runProcessExecutor(ProcessExecutor processExecutor)
      throws InterruptedException, TimeoutException, IOException {
    return processExecutor.execute();
  }

  @Override
  public StartedProcess tailLogsForPcf(PcfRequestConfig pcfRequestConfig, ExecutionLogCallback executionLogCallback)
      throws PivotalClientApiException {
    try {
      boolean loginSuccessful = pcfRequestConfig.isLoggedin()
          ? pcfRequestConfig.isLoggedin()
          : doLogin(pcfRequestConfig, executionLogCallback, pcfRequestConfig.getCfHomeDirPath());

      if (!loginSuccessful) {
        executionLogCallback.saveExecutionLog(color("Failed to login", Red, LogWeight.Bold));
        throw new PivotalClientApiException("Failed to login");
      }

      ProcessExecutor processExecutor = getProcessExecutorForLogTailing(pcfRequestConfig, executionLogCallback);

      return processExecutor.start();
    } catch (Exception e) {
      throw new PivotalClientApiException(PIVOTAL_CLOUD_FOUNDRY_CLIENT_EXCEPTION + "Failed while tailing logs", e);
    }
  }

  @VisibleForTesting
  ProcessExecutor getProcessExecutorForLogTailing(
      PcfRequestConfig pcfRequestConfig, ExecutionLogCallback executionLogCallback) {
    return new ProcessExecutor()
        .timeout(pcfRequestConfig.getTimeOutIntervalInMins(), TimeUnit.MINUTES)
        .command(BIN_SH, "-c", CF_COMMAND_FOR_APP_LOG_TAILING.replace(APP_TOKEN, pcfRequestConfig.getApplicationName()))
        .readOutput(true)
        .environment(getEnvironmentMapForPcfExecutor(pcfRequestConfig.getCfHomeDirPath()))
        .redirectOutput(new LogOutputStream() {
          @Override
          protected void processLine(String line) {
            executionLogCallback.saveExecutionLog(line);
          }
        });
  }

  @Override
  public void unmapRoutesForApplicationUsingCli(PcfRequestConfig pcfRequestConfig, List<String> routes,
      ExecutionLogCallback logCallback) throws PivotalClientApiException, InterruptedException {
    executeRoutesOperationForApplicationUsingCli("cf unmap-route", pcfRequestConfig, routes, logCallback);
  }

  @Override
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

  @VisibleForTesting
  PcfRouteInfo extractRouteInfoFromPath(Set<String> domainNames, String route) throws PivotalClientApiException {
    PcfRouteInfoBuilder builder = PcfRouteInfo.builder();
    int index = route.indexOf(PCF_ROUTE_PORT_SEPARATOR);
    if (index != -1) {
      // TCP
      builder.type(PCF_ROUTE_TYPE_TCP);
      String port = route.substring(index + 1);
      builder.port(port);
      String domain = route.substring(0, index);
      builder.domain(domain);
      return builder.build();
    }

    // HTTP
    builder.type(PCF_ROUTE_TYPE_HTTP);
    boolean foundMatch = false;
    String longestMatchingDomain = EMPTY;
    for (String domainName : domainNames) {
      if (route.contains(domainName)) {
        if (!foundMatch) {
          foundMatch = true;
          longestMatchingDomain = domainName;
        } else {
          if (domainName.length() > longestMatchingDomain.length()) {
            longestMatchingDomain = domainName;
          }
        }
      }
    }
    builder.domain(longestMatchingDomain);

    if (!foundMatch) {
      throw new PivotalClientApiException(new StringBuilder(128)
                                              .append("Invalid Route Name: ")
                                              .append(route)
                                              .append(", used domain not present in this space")
                                              .toString());
    }

    int domainStartIndex = route.indexOf(longestMatchingDomain);
    String hostName = domainStartIndex == 0 ? null : route.substring(0, domainStartIndex - 1);
    builder.hostName(hostName);

    String path = null;
    int indexForPath = route.indexOf(PCF_ROUTE_PATH_SEPARATOR_CHAR);
    if (indexForPath != -1) {
      path = route.substring(indexForPath + 1);
    }
    builder.path(path);
    return builder.build();
  }

  @Override
  public void mapRoutesForApplicationUsingCli(PcfRequestConfig pcfRequestConfig, List<String> routes,
      ExecutionLogCallback logCallback) throws PivotalClientApiException {
    executeRoutesOperationForApplicationUsingCli("cf map-route", pcfRequestConfig, routes, logCallback);
  }

  @VisibleForTesting
  void executeRoutesOperationForApplicationUsingCli(String operation, PcfRequestConfig pcfRequestConfig,
      List<String> routes, ExecutionLogCallback logCallback) throws PivotalClientApiException {
    try {
      if (!pcfRequestConfig.isUseCFCLI()) {
        throw new InvalidRequestException("Trying to map routes using Cli without flag in Pcf request Config");
      }

      if (!pcfRequestConfig.isLoggedin()) {
        if (!doLogin(pcfRequestConfig, logCallback, pcfRequestConfig.getCfHomeDirPath())) {
          String errorMessage = format("Failed to login when performing: [%s]", operation);
          logCallback.saveExecutionLog(color(errorMessage, Red, LogWeight.Bold));
          throw new InvalidRequestException(errorMessage);
        }
      }

      List<Domain> allDomainsForSpace = getAllDomainsForSpace(pcfRequestConfig);
      Set<String> domainNames = allDomainsForSpace.stream().map(Domain::getName).collect(toSet());
      logCallback.saveExecutionLog(format("Found domain names: [%s]", join(", ", domainNames)));

      if (isNotEmpty(routes)) {
        int exitcode;
        String command;
        Map<String, String> env = getEnvironmentMapForPcfExecutor(pcfRequestConfig.getCfHomeDirPath());
        for (String route : routes) {
          logCallback.saveExecutionLog(format("Extracting info from route: [%s]", route));
          PcfRouteInfo info = extractRouteInfoFromPath(domainNames, route);
          if (PCF_ROUTE_TYPE_TCP == info.getType()) {
            command = format("%s %s %s --port %s", operation, pcfRequestConfig.getApplicationName(), info.getDomain(),
                info.getPort());
          } else {
            StringBuilder stringBuilder = new StringBuilder(
                format("%s %s %s", operation, pcfRequestConfig.getApplicationName(), info.getDomain()));
            if (isNotEmpty(info.getHostName())) {
              stringBuilder.append(format(" --hostname %s ", info.getHostName()));
            }
            if (isNotEmpty(info.getPath())) {
              stringBuilder.append(format(" --path %s ", info.getPath()));
            }
            command = stringBuilder.toString();
          }
          exitcode = executeCommand(command, env, logCallback);
          if (exitcode != 0) {
            String message = format("Failed to map route: [%s]", route);
            logCallback.saveExecutionLog(message, ERROR);
            throw new InvalidRequestException(message);
          }
        }
      }
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new PivotalClientApiException(PIVOTAL_CLOUD_FOUNDRY_CLIENT_EXCEPTION + "Failed mapping routes", ex);
    } catch (IOException | TimeoutException ex) {
      throw new PivotalClientApiException(PIVOTAL_CLOUD_FOUNDRY_CLIENT_EXCEPTION + "Failed mapping routes", ex);
    }
  }

  @Override
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
      Set<String> routesExisting = routeList.stream().map(this ::getPathFromRouteMap).collect(toSet());
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

  @Override
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

  @VisibleForTesting
  ConnectionContext getConnectionContext(PcfRequestConfig pcfRequestConfig) throws PivotalClientApiException {
    try {
      long timeout = pcfRequestConfig.getTimeOutIntervalInMins() <= 0 ? 5 : pcfRequestConfig.getTimeOutIntervalInMins();
      return DefaultConnectionContext.builder()
          .apiHost(pcfRequestConfig.getEndpointUrl())
          .skipSslValidation(true)
          .connectTimeout(Duration.ofMinutes(timeout))
          .build();
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