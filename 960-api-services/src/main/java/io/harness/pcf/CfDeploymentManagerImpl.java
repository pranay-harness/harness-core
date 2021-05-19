package io.harness.pcf;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.pcf.model.PcfConstants.DISABLE_AUTOSCALING;
import static io.harness.pcf.model.PcfConstants.ENABLE_AUTOSCALING;
import static io.harness.pcf.model.PcfConstants.HARNESS__ACTIVE__IDENTIFIER;
import static io.harness.pcf.model.PcfConstants.HARNESS__STAGE__IDENTIFIER;
import static io.harness.pcf.model.PcfConstants.HARNESS__STATUS__IDENTIFIER;
import static io.harness.pcf.model.PcfConstants.HARNESS__STATUS__INDENTIFIER;
import static io.harness.pcf.model.PcfConstants.PCF_CONNECTIVITY_SUCCESS;
import static io.harness.pcf.model.PcfConstants.PIVOTAL_CLOUD_FOUNDRY_CLIENT_EXCEPTION;
import static io.harness.pcf.model.PcfConstants.THREAD_SLEEP_INTERVAL_FOR_STEADY_STATE_CHECK;

import static software.wings.beans.LogColor.White;
import static software.wings.beans.LogHelper.color;
import static software.wings.beans.LogWeight.Bold;

import static java.util.Comparator.comparingInt;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.ExceptionUtils;
import io.harness.logging.LogCallback;
import io.harness.logging.LogLevel;
import io.harness.pcf.model.CfAppAutoscalarRequestData;
import io.harness.pcf.model.CfConfig;
import io.harness.pcf.model.CfCreateApplicationRequestData;
import io.harness.pcf.model.CfRequestConfig;
import io.harness.pcf.model.CfRunPluginScriptRequestData;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.cloudfoundry.operations.applications.ApplicationDetail;
import org.cloudfoundry.operations.applications.ApplicationEnvironments;
import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.cloudfoundry.operations.organizations.OrganizationSummary;
import org.cloudfoundry.operations.routes.Route;
import org.zeroturnaround.exec.StartedProcess;

@Singleton
@OwnedBy(CDP)
public class CfDeploymentManagerImpl implements CfDeploymentManager {
  public static final String DELIMITER = "__";
  private static final List<String> STATUS_ENV_VARIABLES =
      Arrays.asList(HARNESS__STATUS__INDENTIFIER, HARNESS__STATUS__IDENTIFIER);
  @Inject private CfCliClient cfCliClient;
  @Inject private CfSdkClient cfSdkClient;

  @Override
  public List<String> getOrganizations(CfRequestConfig cfRequestConfig) throws PivotalClientApiException {
    try {
      List<OrganizationSummary> organizationSummaries = cfSdkClient.getOrganizations(cfRequestConfig);
      return organizationSummaries.stream().map(OrganizationSummary::getName).collect(toList());
    } catch (Exception e) {
      throw new PivotalClientApiException(PIVOTAL_CLOUD_FOUNDRY_CLIENT_EXCEPTION + ExceptionUtils.getMessage(e), e);
    }
  }

  @Override
  public List<String> getSpacesForOrganization(CfRequestConfig cfRequestConfig) throws PivotalClientApiException {
    try {
      return cfSdkClient.getSpacesForOrganization(cfRequestConfig);
    } catch (Exception e) {
      throw new PivotalClientApiException(PIVOTAL_CLOUD_FOUNDRY_CLIENT_EXCEPTION + ExceptionUtils.getMessage(e), e);
    }
  }

  @Override
  public List<String> getRouteMaps(CfRequestConfig cfRequestConfig) throws PivotalClientApiException {
    try {
      return cfSdkClient.getRoutesForSpace(cfRequestConfig);
    } catch (Exception e) {
      throw new PivotalClientApiException(PIVOTAL_CLOUD_FOUNDRY_CLIENT_EXCEPTION + ExceptionUtils.getMessage(e), e);
    }
  }

  @Override
  public ApplicationDetail createApplication(
      CfCreateApplicationRequestData requestData, LogCallback executionLogCallback) throws PivotalClientApiException {
    CfRequestConfig pcfRequestConfig = requestData.getCfRequestConfig();
    try {
      if (pcfRequestConfig.isUseCFCLI()) {
        cfCliClient.pushAppByCli(requestData, executionLogCallback);
      } else {
        Path path = Paths.get(requestData.getManifestFilePath());
        cfSdkClient.pushAppBySdk(pcfRequestConfig, path, executionLogCallback);
      }

      return getApplicationByName(requestData.getCfRequestConfig());
    } catch (Exception e) {
      throw new PivotalClientApiException(PIVOTAL_CLOUD_FOUNDRY_CLIENT_EXCEPTION + ExceptionUtils.getMessage(e), e);
    }
  }

  @Override
  public ApplicationDetail getApplicationByName(CfRequestConfig cfRequestConfig) throws PivotalClientApiException {
    try {
      return cfSdkClient.getApplicationByName(cfRequestConfig);
    } catch (Exception e) {
      throw new PivotalClientApiException(PIVOTAL_CLOUD_FOUNDRY_CLIENT_EXCEPTION + ExceptionUtils.getMessage(e), e);
    }
  }

  @Override
  public ApplicationDetail resizeApplication(CfRequestConfig pcfRequestConfig) throws PivotalClientApiException {
    try {
      ApplicationDetail applicationDetail = cfSdkClient.getApplicationByName(pcfRequestConfig);
      cfSdkClient.scaleApplications(pcfRequestConfig);
      if (pcfRequestConfig.getDesiredCount() > 0 && applicationDetail.getInstances() == 0) {
        cfSdkClient.startApplication(pcfRequestConfig);
      }

      // is scales down to 0, stop application
      if (pcfRequestConfig.getDesiredCount() == 0) {
        cfSdkClient.stopApplication(pcfRequestConfig);
      }

      return cfSdkClient.getApplicationByName(pcfRequestConfig);
    } catch (Exception e) {
      throw new PivotalClientApiException(PIVOTAL_CLOUD_FOUNDRY_CLIENT_EXCEPTION + ExceptionUtils.getMessage(e), e);
    }
  }

  @Override
  public ApplicationDetail upsizeApplicationWithSteadyStateCheck(
      CfRequestConfig cfRequestConfig, LogCallback executionLogCallback) throws PivotalClientApiException {
    boolean steadyStateReached = false;
    long timeout = cfRequestConfig.getTimeOutIntervalInMins() <= 0 ? 10 : cfRequestConfig.getTimeOutIntervalInMins();
    long expiryTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(timeout);

    executionLogCallback.saveExecutionLog(color("\n# Streaming Logs From PCF -", White, Bold));
    StartedProcess startedProcess = startTailingLogsIfNeeded(cfRequestConfig, executionLogCallback, null);

    ApplicationDetail applicationDetail = resizeApplication(cfRequestConfig);
    while (!steadyStateReached && System.currentTimeMillis() < expiryTime) {
      try {
        startedProcess = startTailingLogsIfNeeded(cfRequestConfig, executionLogCallback, startedProcess);

        applicationDetail = cfSdkClient.getApplicationByName(cfRequestConfig);
        if (reachedDesiredState(applicationDetail, cfRequestConfig.getDesiredCount())) {
          steadyStateReached = true;
          destroyProcess(startedProcess);
        } else {
          Thread.sleep(THREAD_SLEEP_INTERVAL_FOR_STEADY_STATE_CHECK);
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt(); // restore the flag
        throw new PivotalClientApiException("Thread Was Interrupted, stopping execution");
      } catch (Exception e) {
        executionLogCallback.saveExecutionLog(
            "Error while waiting for steadyStateCheck." + e.getMessage() + ", Continuing with steadyStateCheck");
      }
    }

    if (!steadyStateReached) {
      executionLogCallback.saveExecutionLog(color("# Steady State Check Failed", White, Bold));
      destroyProcess(startedProcess);
      throw new PivotalClientApiException(PIVOTAL_CLOUD_FOUNDRY_CLIENT_EXCEPTION + "Failed to reach steady state");
    }

    return applicationDetail;
  }

  @VisibleForTesting
  void destroyProcess(StartedProcess startedProcess) {
    if (startedProcess != null && startedProcess.getProcess() != null) {
      Process process = startedProcess.getProcess();

      try {
        if (startedProcess.getFuture() != null && !startedProcess.getFuture().isDone()
            && !startedProcess.getFuture().isCancelled()) {
          startedProcess.getFuture().cancel(true);
        }
      } catch (Exception e) {
        // This is a safeguards, as we still want to continue to destroy process.
      }
      process.destroy();
      if (process.isAlive()) {
        process.destroyForcibly();
      }
    }
  }

  @VisibleForTesting
  boolean reachedDesiredState(ApplicationDetail applicationDetail, int desiredCount) {
    if (applicationDetail.getRunningInstances() != desiredCount) {
      return false;
    }

    boolean reachedDesiredState = false;
    if (EmptyPredicate.isNotEmpty(applicationDetail.getInstanceDetails())) {
      int count = (int) applicationDetail.getInstanceDetails()
                      .stream()
                      .filter(instanceDetail -> "RUNNING".equals(instanceDetail.getState()))
                      .count();
      reachedDesiredState = count == desiredCount;
    }

    return reachedDesiredState;
  }

  @VisibleForTesting
  StartedProcess startTailingLogsIfNeeded(
      CfRequestConfig cfRequestConfig, LogCallback executionLogCallback, StartedProcess startedProcess) {
    if (!cfRequestConfig.isUseCFCLI()) {
      return null;
    }

    try {
      if (startedProcess == null || startedProcess.getProcess() == null || !startedProcess.getProcess().isAlive()) {
        executionLogCallback.saveExecutionLog("# Printing next Log batch: ");
        startedProcess = cfCliClient.tailLogsForPcf(cfRequestConfig, executionLogCallback);
      }
    } catch (PivotalClientApiException e) {
      executionLogCallback.saveExecutionLog("Failed while retrieving logs in this attempt", LogLevel.WARN);
    }
    return startedProcess;
  }

  @Override
  public void unmapRouteMapForApplication(CfRequestConfig cfRequestConfig, List<String> paths, LogCallback logCallback)
      throws PivotalClientApiException {
    try {
      if (cfRequestConfig.isUseCFCLI()) {
        cfCliClient.unmapRoutesForApplicationUsingCli(cfRequestConfig, paths, logCallback);
      } else {
        cfSdkClient.unmapRoutesForApplication(cfRequestConfig, paths);
      }
    } catch (Exception e) {
      throw new PivotalClientApiException(PIVOTAL_CLOUD_FOUNDRY_CLIENT_EXCEPTION + ExceptionUtils.getMessage(e), e);
    }
  }

  @Override
  public void mapRouteMapForApplication(CfRequestConfig cfRequestConfig, List<String> paths, LogCallback logCallback)
      throws PivotalClientApiException {
    try {
      if (cfRequestConfig.isUseCFCLI()) {
        cfCliClient.mapRoutesForApplicationUsingCli(cfRequestConfig, paths, logCallback);
      } else {
        cfSdkClient.mapRoutesForApplication(cfRequestConfig, paths);
      }
    } catch (Exception e) {
      throw new PivotalClientApiException(PIVOTAL_CLOUD_FOUNDRY_CLIENT_EXCEPTION + ExceptionUtils.getMessage(e), e);
    }
  }

  @Override
  public List<ApplicationSummary> getDeployedServicesWithNonZeroInstances(
      CfRequestConfig cfRequestConfig, String prefix) throws PivotalClientApiException {
    try {
      List<ApplicationSummary> applicationSummaries = cfSdkClient.getApplications(cfRequestConfig);
      if (CollectionUtils.isEmpty(applicationSummaries)) {
        return Collections.EMPTY_LIST;
      }

      return applicationSummaries.stream()
          .filter(
              applicationSummary -> matchesPrefix(prefix, applicationSummary) && applicationSummary.getInstances() > 0)
          .sorted(comparingInt(applicationSummary -> getRevisionFromServiceName(applicationSummary.getName())))
          .collect(toList());

    } catch (Exception e) {
      throw new PivotalClientApiException(PIVOTAL_CLOUD_FOUNDRY_CLIENT_EXCEPTION + ExceptionUtils.getMessage(e), e);
    }
  }

  @Override
  public List<ApplicationSummary> getPreviousReleases(CfRequestConfig cfRequestConfig, String prefix)
      throws PivotalClientApiException {
    try {
      List<ApplicationSummary> applicationSummaries = cfSdkClient.getApplications(cfRequestConfig);
      if (CollectionUtils.isEmpty(applicationSummaries)) {
        return Collections.EMPTY_LIST;
      }

      return applicationSummaries.stream()
          .filter(applicationSummary -> matchesPrefix(prefix, applicationSummary))
          .sorted(comparingInt(applicationSummary -> getRevisionFromServiceName(applicationSummary.getName())))
          .collect(toList());

    } catch (Exception e) {
      throw new PivotalClientApiException(PIVOTAL_CLOUD_FOUNDRY_CLIENT_EXCEPTION + ExceptionUtils.getMessage(e), e);
    }
  }

  @VisibleForTesting
  boolean matchesPrefix(String prefix, ApplicationSummary applicationSummary) {
    int revision = getRevisionFromServiceName(applicationSummary.getName());
    // has no revision, so this app was not deployed by harness
    if (revision == -1) {
      return false;
    }

    return getAppPrefixByRemovingNumber(applicationSummary.getName()).equals(prefix);
  }

  @Override
  public void deleteApplication(CfRequestConfig cfRequestConfig) throws PivotalClientApiException {
    try {
      cfSdkClient.deleteApplication(cfRequestConfig);
    } catch (Exception e) {
      throw new PivotalClientApiException(PIVOTAL_CLOUD_FOUNDRY_CLIENT_EXCEPTION + ExceptionUtils.getMessage(e), e);
    }
  }

  @Override
  public String stopApplication(CfRequestConfig cfRequestConfig) throws PivotalClientApiException {
    try {
      cfSdkClient.stopApplication(cfRequestConfig);
      return getDetailedApplicationState(cfRequestConfig);
    } catch (Exception e) {
      throw new PivotalClientApiException(PIVOTAL_CLOUD_FOUNDRY_CLIENT_EXCEPTION + ExceptionUtils.getMessage(e), e);
    }
  }

  @Override
  public String createRouteMap(CfRequestConfig cfRequestConfig, String host, String domain, String path,
      boolean tcpRoute, boolean useRandomPort, Integer port) throws PivotalClientApiException, InterruptedException {
    validateArgs(host, domain, path, tcpRoute, useRandomPort, port);

    // Path should always start with '/'
    if (StringUtils.isNotBlank(path) && path.charAt(0) != '/') {
      path = new StringBuilder(64).append("/").append(path).toString();
    }

    cfSdkClient.createRouteMap(cfRequestConfig, host, domain, path, tcpRoute, useRandomPort, port);

    String routePath = generateRouteUrl(host, domain, path, tcpRoute, useRandomPort, port);
    Optional<Route> route = cfSdkClient.getRouteMap(cfRequestConfig, routePath);
    if (route.isPresent()) {
      return routePath;
    } else {
      throw new PivotalClientApiException("Failed To Create Route");
    }
  }

  @Override
  public String checkConnectivity(
      CfConfig pcfConfig, boolean limitPcfThreads, boolean ignorePcfConnectionContextCache) {
    try {
      getOrganizations(CfRequestConfig.builder()
                           .endpointUrl(pcfConfig.getEndpointUrl())
                           .userName(String.valueOf(pcfConfig.getUsername()))
                           .password(String.valueOf(pcfConfig.getPassword()))
                           .limitPcfThreads(limitPcfThreads)
                           .timeOutIntervalInMins(5)
                           .build());
    } catch (PivotalClientApiException e) {
      return e.getMessage();
    }

    return PCF_CONNECTIVITY_SUCCESS;
  }

  @Override
  public boolean checkIfAppHasAutoscalarAttached(CfAppAutoscalarRequestData appAutoscalarRequestData,
      LogCallback executionLogCallback) throws PivotalClientApiException {
    return cfCliClient.checkIfAppHasAutoscalerAttached(appAutoscalarRequestData, executionLogCallback);
  }

  @Override
  public void performConfigureAutoscalar(CfAppAutoscalarRequestData appAutoscalarRequestData,
      LogCallback executionLogCallback) throws PivotalClientApiException {
    boolean autoscalarAttached =
        cfCliClient.checkIfAppHasAutoscalerAttached(appAutoscalarRequestData, executionLogCallback);
    if (autoscalarAttached) {
      cfCliClient.performConfigureAutoscaler(appAutoscalarRequestData, executionLogCallback);
    } else {
      executionLogCallback.saveExecutionLog(
          color(new StringBuilder(128)
                    .append("# No Autoscaling service Instance was associated with Application: ")
                    .append(appAutoscalarRequestData.getApplicationName())
                    .append(", Configure autoscalar can not be performed")
                    .toString(),
              White, Bold));
    }
  }

  @Override
  public boolean changeAutoscalarState(CfAppAutoscalarRequestData appAutoscalarRequestData,
      LogCallback executionLogCallback, boolean enable) throws PivotalClientApiException {
    // If we want to enable it, its expected to be disabled and vice versa
    appAutoscalarRequestData.setExpectedEnabled(!enable);
    boolean autoscalarAttachedWithExpectedStatus =
        cfCliClient.checkIfAppHasAutoscalerWithExpectedState(appAutoscalarRequestData, executionLogCallback);

    if (autoscalarAttachedWithExpectedStatus) {
      executionLogCallback.saveExecutionLog(color(new StringBuilder(128)
                                                      .append("# Performing Operation: ")
                                                      .append(enable ? ENABLE_AUTOSCALING : DISABLE_AUTOSCALING)
                                                      .append(" For Application: ")
                                                      .append(appAutoscalarRequestData.getApplicationName())
                                                      .toString(),
          White, Bold));
      cfCliClient.changeAutoscalerState(appAutoscalarRequestData, executionLogCallback, enable);
      return true;
    } else {
      executionLogCallback.saveExecutionLog(
          color("# No Need to update Autoscalar for Application: " + appAutoscalarRequestData.getApplicationName(),
              White, Bold));
    }

    return false;
  }

  @Override
  public boolean isActiveApplication(CfRequestConfig cfRequestConfig, LogCallback executionLogCallback)
      throws PivotalClientApiException {
    // If we want to enable it, its expected to be disabled and vice versa
    ApplicationEnvironments applicationEnvironments = cfSdkClient.getApplicationEnvironmentsByName(cfRequestConfig);
    if (applicationEnvironments != null && EmptyPredicate.isNotEmpty(applicationEnvironments.getUserProvided())) {
      for (String statusKey : STATUS_ENV_VARIABLES) {
        if (applicationEnvironments.getUserProvided().containsKey(statusKey)
            && HARNESS__ACTIVE__IDENTIFIER.equals(applicationEnvironments.getUserProvided().get(statusKey))) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public void setEnvironmentVariableForAppStatus(CfRequestConfig cfRequestConfig, boolean activeStatus,
      LogCallback executionLogCallback) throws PivotalClientApiException {
    // If we want to enable it, its expected to be disabled and vice versa
    removeOldStatusVariableIfExist(cfRequestConfig, executionLogCallback);
    cfCliClient.setEnvVariablesForApplication(
        Collections.singletonMap(
            HARNESS__STATUS__IDENTIFIER, activeStatus ? HARNESS__ACTIVE__IDENTIFIER : HARNESS__STAGE__IDENTIFIER),
        cfRequestConfig, executionLogCallback);
  }

  private void removeOldStatusVariableIfExist(CfRequestConfig pcfRequestConfig, LogCallback executionLogCallback)
      throws PivotalClientApiException {
    ApplicationEnvironments applicationEnvironments = cfSdkClient.getApplicationEnvironmentsByName(pcfRequestConfig);
    if (applicationEnvironments != null && EmptyPredicate.isNotEmpty(applicationEnvironments.getUserProvided())) {
      Map<String, Object> userProvided = applicationEnvironments.getUserProvided();
      if (userProvided.containsKey(HARNESS__STATUS__INDENTIFIER)) {
        cfCliClient.unsetEnvVariablesForApplication(
            Collections.singletonList(HARNESS__STATUS__INDENTIFIER), pcfRequestConfig, executionLogCallback);
      }
    }
  }

  @Override
  public void unsetEnvironmentVariableForAppStatus(CfRequestConfig cfRequestConfig, LogCallback executionLogCallback)
      throws PivotalClientApiException {
    // If we want to enable it, its expected to be disabled and vice versa
    ApplicationEnvironments applicationEnvironments = cfSdkClient.getApplicationEnvironmentsByName(cfRequestConfig);
    if (applicationEnvironments != null && EmptyPredicate.isNotEmpty(applicationEnvironments.getUserProvided())) {
      Map<String, Object> userProvided = applicationEnvironments.getUserProvided();
      for (String statusKey : STATUS_ENV_VARIABLES) {
        if (userProvided.containsKey(statusKey)) {
          cfCliClient.unsetEnvVariablesForApplication(
              Collections.singletonList(statusKey), cfRequestConfig, executionLogCallback);
        }
      }
    }
  }

  @Override
  public void runPcfPluginScript(CfRunPluginScriptRequestData requestData, LogCallback logCallback)
      throws PivotalClientApiException {
    cfCliClient.runPcfPluginScript(requestData, logCallback);
  }

  private String generateRouteUrl(
      String host, String domain, String path, boolean tcpRoute, boolean useRandomPort, Integer port) {
    StringBuilder routeBuilder = new StringBuilder(128);
    if (tcpRoute) {
      if (useRandomPort) {
        routeBuilder.append(domain);
      } else {
        routeBuilder.append(domain).append(':').append(port);
      }
    } else {
      routeBuilder.append(host).append('.').append(domain);
      if (StringUtils.isNotBlank(path)) {
        routeBuilder.append(path);
      }
    }

    return routeBuilder.toString();
  }

  private void validateArgs(String host, String domain, String path, boolean tcpRoute, boolean useRandomPort,
      Integer port) throws PivotalClientApiException {
    if (isBlank(domain)) {
      throw new PivotalClientApiException(
          PIVOTAL_CLOUD_FOUNDRY_CLIENT_EXCEPTION + "Domain Can Not Be Null For Create Route Request");
    }

    if (!tcpRoute) {
      if (isBlank(host)) {
        throw new PivotalClientApiException(
            PIVOTAL_CLOUD_FOUNDRY_CLIENT_EXCEPTION + "HostName is required For Http Route");
      }
    } else {
      if (!useRandomPort && port == null) {
        throw new PivotalClientApiException(PIVOTAL_CLOUD_FOUNDRY_CLIENT_EXCEPTION
            + "For TCP Route when UseRandomPort = false, port value must be provided");
      }
    }
  }

  private String getDetailedApplicationState(CfRequestConfig cfRequestConfig)
      throws PivotalClientApiException, InterruptedException {
    ApplicationDetail applicationDetail = cfSdkClient.getApplicationByName(cfRequestConfig);
    return new StringBuilder("Application Created : ")
        .append(applicationDetail.getName())
        .append(", Details: ")
        .append(applicationDetail.toString())
        .toString();
  }

  public static int getRevisionFromServiceName(String name) {
    if (name != null) {
      int index = name.lastIndexOf(DELIMITER);
      if (index >= 0) {
        try {
          return Integer.parseInt(name.substring(index + DELIMITER.length()));
        } catch (NumberFormatException e) {
          // Ignore
        }
      }
    }
    return -1;
  }

  public String getAppPrefixByRemovingNumber(String name) {
    if (StringUtils.isBlank(name)) {
      return StringUtils.EMPTY;
    }

    int index = name.lastIndexOf(DELIMITER);
    if (index >= 0) {
      name = name.substring(0, index);
    }
    return name;
  }
}
