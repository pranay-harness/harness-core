package software.wings.helpers.ext.pcf;

import static io.harness.pcf.model.PcfConstants.DISABLE_AUTOSCALING;
import static io.harness.pcf.model.PcfConstants.ENABLE_AUTOSCALING;
import static io.harness.pcf.model.PcfConstants.PIVOTAL_CLOUD_FOUNDRY_CLIENT_EXCEPTION;
import static io.harness.pcf.model.PcfConstants.THREAD_SLEEP_INTERVAL_FOR_STEADY_STATE_CHECK;
import static java.util.Comparator.comparingInt;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.beans.Log.LogColor.White;
import static software.wings.beans.Log.LogWeight.Bold;
import static software.wings.beans.Log.color;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.ExceptionUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.cloudfoundry.operations.applications.ApplicationDetail;
import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.cloudfoundry.operations.organizations.OrganizationSummary;
import org.cloudfoundry.operations.routes.Route;
import org.zeroturnaround.exec.StartedProcess;
import software.wings.beans.Log.LogLevel;
import software.wings.beans.PcfConfig;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.helpers.ext.pcf.request.PcfAppAutoscalarRequestData;
import software.wings.helpers.ext.pcf.request.PcfCreateApplicationRequestData;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Singleton
public class PcfDeploymentManagerImpl implements PcfDeploymentManager {
  public static final String DELIMITER = "__";
  @Inject PcfClient pcfClient;

  @Override
  public List<String> getOrganizations(PcfRequestConfig pcfRequestConfig) throws PivotalClientApiException {
    try {
      List<OrganizationSummary> organizationSummaries = pcfClient.getOrganizations(pcfRequestConfig);
      return organizationSummaries.stream().map(organizationSummary -> organizationSummary.getName()).collect(toList());
    } catch (Exception e) {
      throw new PivotalClientApiException(PIVOTAL_CLOUD_FOUNDRY_CLIENT_EXCEPTION + ExceptionUtils.getMessage(e), e);
    }
  }

  @Override
  public List<String> getSpacesForOrganization(PcfRequestConfig pcfRequestConfig) throws PivotalClientApiException {
    try {
      return pcfClient.getSpacesForOrganization(pcfRequestConfig);
    } catch (Exception e) {
      throw new PivotalClientApiException(PIVOTAL_CLOUD_FOUNDRY_CLIENT_EXCEPTION + ExceptionUtils.getMessage(e), e);
    }
  }

  @Override
  public List<String> getRouteMaps(PcfRequestConfig pcfRequestConfig) throws PivotalClientApiException {
    try {
      return pcfClient.getRoutesForSpace(pcfRequestConfig);
    } catch (Exception e) {
      throw new PivotalClientApiException(PIVOTAL_CLOUD_FOUNDRY_CLIENT_EXCEPTION + ExceptionUtils.getMessage(e), e);
    }
  }

  @Override
  public ApplicationDetail createApplication(PcfCreateApplicationRequestData requestData,
      ExecutionLogCallback executionLogCallback) throws PivotalClientApiException {
    try {
      pcfClient.pushApplicationUsingManifest(requestData, executionLogCallback);
      return getApplicationByName(requestData.getPcfRequestConfig());
    } catch (Exception e) {
      throw new PivotalClientApiException(PIVOTAL_CLOUD_FOUNDRY_CLIENT_EXCEPTION + ExceptionUtils.getMessage(e), e);
    }
  }

  @Override
  public ApplicationDetail getApplicationByName(PcfRequestConfig pcfRequestConfig) throws PivotalClientApiException {
    try {
      return pcfClient.getApplicationByName(pcfRequestConfig);
    } catch (Exception e) {
      throw new PivotalClientApiException(PIVOTAL_CLOUD_FOUNDRY_CLIENT_EXCEPTION + ExceptionUtils.getMessage(e), e);
    }
  }

  @Override
  public ApplicationDetail resizeApplication(PcfRequestConfig pcfRequestConfig) throws PivotalClientApiException {
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
  public ApplicationDetail upsizeApplicationWithSteadyStateCheck(
      PcfRequestConfig pcfRequestConfig, ExecutionLogCallback executionLogCallback) throws PivotalClientApiException {
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
      PcfRequestConfig pcfRequestConfig, ExecutionLogCallback executionLogCallback, StartedProcess startedProcess) {
    if (!pcfRequestConfig.isUseCFCLI()) {
      return null;
    }

    try {
      if (startedProcess == null || startedProcess.getProcess() == null || !startedProcess.getProcess().isAlive()) {
        executionLogCallback.saveExecutionLog("# Printing next Log batch: ");
        startedProcess = pcfClient.tailLogsForPcf(pcfRequestConfig, executionLogCallback);
      }
    } catch (PivotalClientApiException e) {
      executionLogCallback.saveExecutionLog("Failed while retrieving logs in this attempt", LogLevel.WARN);
    }
    return startedProcess;
  }

  @Override
  public void unmapRouteMapForApplication(PcfRequestConfig pcfRequestConfig, List<String> paths)
      throws PivotalClientApiException {
    try {
      pcfClient.unmapRoutesForApplication(pcfRequestConfig, paths);
    } catch (Exception e) {
      throw new PivotalClientApiException(PIVOTAL_CLOUD_FOUNDRY_CLIENT_EXCEPTION + ExceptionUtils.getMessage(e), e);
    }
  }

  @Override
  public void mapRouteMapForApplication(PcfRequestConfig pcfRequestConfig, List<String> paths)
      throws PivotalClientApiException {
    try {
      pcfClient.mapRoutesForApplication(pcfRequestConfig, paths);
    } catch (Exception e) {
      throw new PivotalClientApiException(PIVOTAL_CLOUD_FOUNDRY_CLIENT_EXCEPTION + ExceptionUtils.getMessage(e), e);
    }
  }

  @Override
  public List<ApplicationSummary> getDeployedServicesWithNonZeroInstances(
      PcfRequestConfig pcfRequestConfig, String prefix) throws PivotalClientApiException {
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
  public List<ApplicationSummary> getPreviousReleases(PcfRequestConfig pcfRequestConfig, String prefix)
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
  public void deleteApplication(PcfRequestConfig pcfRequestConfig) throws PivotalClientApiException {
    try {
      pcfClient.deleteApplication(pcfRequestConfig);
    } catch (Exception e) {
      throw new PivotalClientApiException(PIVOTAL_CLOUD_FOUNDRY_CLIENT_EXCEPTION + ExceptionUtils.getMessage(e), e);
    }
  }

  @Override
  public String stopApplication(PcfRequestConfig pcfRequestConfig) throws PivotalClientApiException {
    try {
      pcfClient.stopApplication(pcfRequestConfig);
      return getDetailedApplicationState(pcfRequestConfig);
    } catch (Exception e) {
      throw new PivotalClientApiException(PIVOTAL_CLOUD_FOUNDRY_CLIENT_EXCEPTION + ExceptionUtils.getMessage(e), e);
    }
  }

  @Override
  public String createRouteMap(PcfRequestConfig pcfRequestConfig, String host, String domain, String path,
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
  public String checkConnectivity(PcfConfig pcfConfig) {
    try {
      getOrganizations(PcfRequestConfig.builder()
                           .endpointUrl(pcfConfig.getEndpointUrl())
                           .userName(pcfConfig.getUsername())
                           .password(String.valueOf(pcfConfig.getPassword()))
                           .timeOutIntervalInMins(5)
                           .build());
    } catch (PivotalClientApiException e) {
      return e.getMessage();
    }

    return "SUCCESS";
  }

  @Override
  public boolean checkIfAppAutoscalarInstalled() throws PivotalClientApiException {
    return pcfClient.checkIfAppAutoscalarInstalled();
  }

  @Override
  public boolean checkIfAppHasAutoscalarAttached(PcfAppAutoscalarRequestData appAutoscalarRequestData,
      ExecutionLogCallback executionLogCallback) throws PivotalClientApiException {
    return pcfClient.checkIfAppHasAutoscalarAttached(appAutoscalarRequestData, executionLogCallback);
  }

  @Override
  public void performConfigureAutoscalar(PcfAppAutoscalarRequestData appAutoscalarRequestData,
      ExecutionLogCallback executionLogCallback) throws PivotalClientApiException {
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
  public boolean changeAutoscalarState(PcfAppAutoscalarRequestData appAutoscalarRequestData,
      ExecutionLogCallback executionLogCallback, boolean enable) throws PivotalClientApiException {
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
  public String resolvePcfPluginHome() {
    return pcfClient.resolvePcfPluginHome();
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

  private String getDetailedApplicationState(PcfRequestConfig pcfRequestConfig)
      throws PivotalClientApiException, InterruptedException {
    ApplicationDetail applicationDetail = pcfClient.getApplicationByName(pcfRequestConfig);
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
