package software.wings.helpers.ext.pcf;

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
import io.harness.pcf.PivotalClientApiException;
import io.harness.pcf.model.CfAppAutoscalarRequestData;
import io.harness.pcf.model.CfConfig;
import io.harness.pcf.model.CfCreateApplicationRequestData;
import io.harness.pcf.model.CfRequestConfig;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
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

  @Inject CfClient pcfClient;

  @Override
  public List<String> getOrganizations(CfRequestConfig cfRequestConfig) throws PivotalClientApiException {
    try {
      List<OrganizationSummary> organizationSummaries = pcfClient.getOrganizations(cfRequestConfig);
      return organizationSummaries.stream().map(OrganizationSummary::getName).collect(toList());
    } catch (Exception e) {
      throw new PivotalClientApiException(PIVOTAL_CLOUD_FOUNDRY_CLIENT_EXCEPTION + ExceptionUtils.getMessage(e), e);
    }
  }

  @Override
  public List<String> getSpacesForOrganization(CfRequestConfig cfRequestConfig) throws PivotalClientApiException {
    try {
      return pcfClient.getSpacesForOrganization(cfRequestConfig);
    } catch (Exception e) {
      throw new PivotalClientApiException(PIVOTAL_CLOUD_FOUNDRY_CLIENT_EXCEPTION + ExceptionUtils.getMessage(e), e);
    }
  }

  @Override
  public ApplicationDetail createApplication(CfCreateApplicationRequestData requestData, LogCallback logCallback)
      throws PivotalClientApiException {
    try {
      pcfClient.pushApplicationUsingManifest(requestData, logCallback);
      return getApplicationByName(requestData.getCfRequestConfig());
    } catch (Exception e) {
      throw new PivotalClientApiException(PIVOTAL_CLOUD_FOUNDRY_CLIENT_EXCEPTION + ExceptionUtils.getMessage(e), e);
    }
  }

  @Override
  public ApplicationDetail getApplicationByName(CfRequestConfig cfRequestConfig) throws PivotalClientApiException {
    try {
      return pcfClient.getApplicationByName(cfRequestConfig);
    } catch (Exception e) {
      throw new PivotalClientApiException(PIVOTAL_CLOUD_FOUNDRY_CLIENT_EXCEPTION + ExceptionUtils.getMessage(e), e);
    }
  }

  @Override
  public ApplicationDetail resizeApplication(CfRequestConfig pcfRequestConfig) throws PivotalClientApiException {
    try {
      ApplicationDetail applicationDetail = pcfClient.getApplicationByName(pcfRequestConfig);
      pcfClient.scaleApplications(pcfRequestConfig);
      if (pcfRequestConfig.getDesiredCount() > 0 && applicationDetail.getInstances() == 0) {
        pcfClient.startApplication(pcfRequestConfig);
      }

      // is scales down to 0, stop application
      if (pcfRequestConfig.getDesiredCount() == 0) {
        pcfClient.stopApplication(pcfRequestConfig);
      }

      return pcfClient.getApplicationByName(pcfRequestConfig);
    } catch (Exception e) {
      throw new PivotalClientApiException(PIVOTAL_CLOUD_FOUNDRY_CLIENT_EXCEPTION + ExceptionUtils.getMessage(e), e);
    }
  }

  @Override
  public void deleteApplication(CfRequestConfig pcfRequestConfig) throws PivotalClientApiException {
    try {
      pcfClient.deleteApplication(pcfRequestConfig);
    } catch (Exception e) {
      throw new PivotalClientApiException(PIVOTAL_CLOUD_FOUNDRY_CLIENT_EXCEPTION + ExceptionUtils.getMessage(e), e);
    }
  }

  @Override
  public String stopApplication(CfRequestConfig pcfRequestConfig) throws PivotalClientApiException {
    try {
      pcfClient.stopApplication(pcfRequestConfig);
      return getDetailedApplicationState(pcfRequestConfig);
    } catch (Exception e) {
      throw new PivotalClientApiException(PIVOTAL_CLOUD_FOUNDRY_CLIENT_EXCEPTION + ExceptionUtils.getMessage(e), e);
    }
  }

  @Override
  public void unmapRouteMapForApplication(CfRequestConfig pcfRequestConfig, List<String> paths, LogCallback logCallback)
      throws PivotalClientApiException {
    try {
      if (pcfRequestConfig.isUseCFCLI()) {
        pcfClient.unmapRoutesForApplicationUsingCli(pcfRequestConfig, paths, logCallback);
      } else {
        pcfClient.unmapRoutesForApplication(pcfRequestConfig, paths);
      }
    } catch (Exception e) {
      throw new PivotalClientApiException(PIVOTAL_CLOUD_FOUNDRY_CLIENT_EXCEPTION + ExceptionUtils.getMessage(e), e);
    }
  }

  @Override
  public void mapRouteMapForApplication(CfRequestConfig pcfRequestConfig, List<String> paths, LogCallback logCallback)
      throws PivotalClientApiException {
    try {
      if (pcfRequestConfig.isUseCFCLI()) {
        pcfClient.mapRoutesForApplicationUsingCli(pcfRequestConfig, paths, logCallback);
      } else {
        pcfClient.mapRoutesForApplication(pcfRequestConfig, paths);
      }
    } catch (Exception e) {
      throw new PivotalClientApiException(PIVOTAL_CLOUD_FOUNDRY_CLIENT_EXCEPTION + ExceptionUtils.getMessage(e), e);
    }
  }

  @Override
  public List<ApplicationSummary> getDeployedServicesWithNonZeroInstances(
      CfRequestConfig pcfRequestConfig, String prefix) throws PivotalClientApiException {
    try {
      List<ApplicationSummary> applicationSummaries = pcfClient.getApplications(pcfRequestConfig);
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
  public List<ApplicationSummary> getPreviousReleases(CfRequestConfig pcfRequestConfig, String prefix)
      throws PivotalClientApiException {
    try {
      List<ApplicationSummary> applicationSummaries = pcfClient.getApplications(pcfRequestConfig);
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

  @Override
  public List<String> getRouteMaps(CfRequestConfig pcfRequestConfig) throws PivotalClientApiException {
    try {
      return pcfClient.getRoutesForSpace(pcfRequestConfig);
    } catch (Exception e) {
      throw new PivotalClientApiException(PIVOTAL_CLOUD_FOUNDRY_CLIENT_EXCEPTION + ExceptionUtils.getMessage(e), e);
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
  public String createRouteMap(CfRequestConfig pcfRequestConfig, String host, String domain, String path,
      boolean tcpRoute, boolean useRandomPort, Integer port) throws PivotalClientApiException, InterruptedException {
    validateArgs(host, domain, path, tcpRoute, useRandomPort, port);

    // Path should always start with '/'
    if (StringUtils.isNotBlank(path) && path.charAt(0) != '/') {
      path = new StringBuilder(64).append("/").append(path).toString();
    }

    pcfClient.createRouteMap(pcfRequestConfig, host, domain, path, tcpRoute, useRandomPort, port);

    String routePath = generateRouteUrl(host, domain, path, tcpRoute, useRandomPort, port);
    Optional<Route> route = pcfClient.getRouteMap(pcfRequestConfig, routePath);
    if (route.isPresent()) {
      return routePath;
    } else {
      throw new PivotalClientApiException("Failed To Create Route");
    }
  }

  @Override
  public void performConfigureAutoscalar(io.harness.pcf.model.CfAppAutoscalarRequestData appAutoscalarRequestData,
      LogCallback executionLogCallback) throws PivotalClientApiException {
    boolean autoscalarAttached =
        pcfClient.checkIfAppHasAutoscalarAttached(appAutoscalarRequestData, executionLogCallback);
    if (autoscalarAttached) {
      pcfClient.performConfigureAutoscalar(appAutoscalarRequestData, executionLogCallback);
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
  public boolean changeAutoscalarState(io.harness.pcf.model.CfAppAutoscalarRequestData appAutoscalarRequestData,
      LogCallback executionLogCallback, boolean enable) throws PivotalClientApiException {
    // If we want to enable it, its expected to be disabled and vice versa
    appAutoscalarRequestData.setExpectedEnabled(!enable);
    boolean autoscalarAttachedWithExpectedStatus =
        pcfClient.checkIfAppHasAutoscalarWithExpectedState(appAutoscalarRequestData, executionLogCallback);

    if (autoscalarAttachedWithExpectedStatus) {
      executionLogCallback.saveExecutionLog(color(new StringBuilder(128)
                                                      .append("# Performing Operation: ")
                                                      .append(enable ? ENABLE_AUTOSCALING : DISABLE_AUTOSCALING)
                                                      .append(" For Application: ")
                                                      .append(appAutoscalarRequestData.getApplicationName())
                                                      .toString(),
          White, Bold));
      pcfClient.changeAutoscalarState(appAutoscalarRequestData, executionLogCallback, enable);
      return true;
    } else {
      executionLogCallback.saveExecutionLog(
          color("# No Need to update Autoscalar for Application: " + appAutoscalarRequestData.getApplicationName(),
              White, Bold));
    }

    return false;
  }

  @Override
  public boolean checkIfAppHasAutoscalarAttached(
      CfAppAutoscalarRequestData appAutoscalarRequestData, LogCallback logCallback) throws PivotalClientApiException {
    return pcfClient.checkIfAppHasAutoscalarAttached(appAutoscalarRequestData, logCallback);
  }

  @Override
  public ApplicationDetail upsizeApplicationWithSteadyStateCheck(
      CfRequestConfig pcfRequestConfig, LogCallback executionLogCallback) throws PivotalClientApiException {
    boolean steadyStateReached = false;
    long timeout = pcfRequestConfig.getTimeOutIntervalInMins() <= 0 ? 10 : pcfRequestConfig.getTimeOutIntervalInMins();
    long expiryTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(timeout);

    executionLogCallback.saveExecutionLog(color("\n# Streaming Logs From PCF -", White, Bold));
    StartedProcess startedProcess = startTailingLogsIfNeeded(pcfRequestConfig, executionLogCallback, null);

    ApplicationDetail applicationDetail = resizeApplication(pcfRequestConfig);
    while (!steadyStateReached && System.currentTimeMillis() < expiryTime) {
      try {
        startedProcess = startTailingLogsIfNeeded(pcfRequestConfig, executionLogCallback, startedProcess);

        applicationDetail = pcfClient.getApplicationByName(pcfRequestConfig);
        if (reachedDesiredState(applicationDetail, pcfRequestConfig.getDesiredCount())) {
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

  @Override
  public boolean isActiveApplication(CfRequestConfig pcfRequestConfig, LogCallback logCallback)
      throws PivotalClientApiException {
    // If we want to enable it, its expected to be disabled and vice versa
    ApplicationEnvironments applicationEnvironments = pcfClient.getApplicationEnvironmentsByName(pcfRequestConfig);
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
  public void setEnvironmentVariableForAppStatus(CfRequestConfig pcfRequestConfig, boolean activeStatus,
      LogCallback logCallback) throws PivotalClientApiException {
    // If we want to enable it, its expected to be disabled and vice versa
    removeOldStatusVariableIfExist(pcfRequestConfig, logCallback);
    pcfClient.setEnvVariablesForApplication(
        Collections.singletonMap(
            HARNESS__STATUS__IDENTIFIER, activeStatus ? HARNESS__ACTIVE__IDENTIFIER : HARNESS__STAGE__IDENTIFIER),
        pcfRequestConfig, logCallback);
  }

  @Override
  public void unsetEnvironmentVariableForAppStatus(CfRequestConfig pcfRequestConfig, LogCallback executionLogCallback)
      throws PivotalClientApiException {
    // If we want to enable it, its expected to be disabled and vice versa
    ApplicationEnvironments applicationEnvironments = pcfClient.getApplicationEnvironmentsByName(pcfRequestConfig);
    if (applicationEnvironments != null && EmptyPredicate.isNotEmpty(applicationEnvironments.getUserProvided())) {
      Map<String, Object> userProvided = applicationEnvironments.getUserProvided();
      for (String statusKey : STATUS_ENV_VARIABLES) {
        if (userProvided.containsKey(statusKey)) {
          pcfClient.unsetEnvVariablesForApplication(
              Collections.singletonList(statusKey), pcfRequestConfig, executionLogCallback);
        }
      }
    }
  }

  private void removeOldStatusVariableIfExist(CfRequestConfig pcfRequestConfig, LogCallback executionLogCallback)
      throws PivotalClientApiException {
    ApplicationEnvironments applicationEnvironments = pcfClient.getApplicationEnvironmentsByName(pcfRequestConfig);
    if (applicationEnvironments != null && EmptyPredicate.isNotEmpty(applicationEnvironments.getUserProvided())) {
      Map<String, Object> userProvided = applicationEnvironments.getUserProvided();
      if (userProvided.containsKey(HARNESS__STATUS__INDENTIFIER)) {
        pcfClient.unsetEnvVariablesForApplication(
            Collections.singletonList(HARNESS__STATUS__INDENTIFIER), pcfRequestConfig, executionLogCallback);
      }
    }
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
      CfRequestConfig pcfRequestConfig, LogCallback logCallback, StartedProcess startedProcess) {
    if (!pcfRequestConfig.isUseCFCLI()) {
      return null;
    }

    try {
      if (startedProcess == null || startedProcess.getProcess() == null || !startedProcess.getProcess().isAlive()) {
        logCallback.saveExecutionLog("# Printing next Log batch: ");
        startedProcess = pcfClient.tailLogsForPcf(pcfRequestConfig, logCallback);
      }
    } catch (PivotalClientApiException e) {
      logCallback.saveExecutionLog("Failed while retrieving logs in this attempt", LogLevel.WARN);
    }
    return startedProcess;
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

  @VisibleForTesting
  boolean matchesPrefix(String prefix, ApplicationSummary applicationSummary) {
    int revision = getRevisionFromServiceName(applicationSummary.getName());
    // has no revision, so this app was not deployed by harness
    if (revision == -1) {
      return false;
    }

    return getAppPrefixByRemovingNumber(applicationSummary.getName()).equals(prefix);
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

  private String getDetailedApplicationState(CfRequestConfig pcfRequestConfig)
      throws PivotalClientApiException, InterruptedException {
    ApplicationDetail applicationDetail = pcfClient.getApplicationByName(pcfRequestConfig);
    return new StringBuilder("Application Created : ")
        .append(applicationDetail.getName())
        .append(", Details: ")
        .append(applicationDetail.toString())
        .toString();
  }
}
