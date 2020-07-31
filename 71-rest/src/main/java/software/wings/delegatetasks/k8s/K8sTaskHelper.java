package software.wings.delegatetasks.k8s;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.filesystem.FileIo.createDirectoryIfDoesNotExist;
import static io.harness.filesystem.FileIo.getFilesUnderPath;
import static io.harness.govern.Switch.unhandled;
import static io.harness.k8s.KubernetesConvention.ReleaseHistoryKeyName;
import static io.harness.k8s.kubectl.GetJobCommand.getPrintableCommand;
import static io.harness.k8s.kubectl.Utils.encloseWithQuotesIfNeeded;
import static io.harness.k8s.kubectl.Utils.parseLatestRevisionNumberFromRolloutHistory;
import static io.harness.k8s.manifest.ManifestHelper.getFirstLoadBalancerService;
import static io.harness.k8s.manifest.ManifestHelper.validateValuesFileContents;
import static io.harness.k8s.manifest.ManifestHelper.values_filename;
import static io.harness.k8s.manifest.ManifestHelper.yaml_file_extension;
import static io.harness.k8s.manifest.ManifestHelper.yml_file_extension;
import static io.harness.k8s.model.K8sExpressions.canaryDestinationExpression;
import static io.harness.k8s.model.K8sExpressions.stableDestinationExpression;
import static io.harness.k8s.model.Kind.Namespace;
import static io.harness.k8s.model.Release.Status.Failed;
import static io.harness.logging.CommandExecutionStatus.FAILURE;
import static io.harness.logging.LogLevel.ERROR;
import static io.harness.logging.LogLevel.INFO;
import static io.harness.state.StateConstants.DEFAULT_STEADY_STATE_TIMEOUT;
import static io.harness.threading.Morpheus.sleep;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.Duration.ofMinutes;
import static java.time.Duration.ofSeconds;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.beans.Log.LogColor.Gray;
import static software.wings.beans.Log.LogColor.White;
import static software.wings.beans.Log.LogColor.Yellow;
import static software.wings.beans.Log.LogWeight.Bold;
import static software.wings.beans.Log.LogWeight.Normal;
import static software.wings.beans.Log.color;
import static software.wings.delegatetasks.k8s.K8sTask.KUBECONFIG_FILENAME;
import static software.wings.delegatetasks.k8s.taskhandler.K8sTrafficSplitTaskHandler.ISTIO_DESTINATION_TEMPLATE;
import static software.wings.helpers.ext.helm.HelmConstants.HELM_PATH_PLACEHOLDER;
import static software.wings.helpers.ext.helm.HelmConstants.HELM_RELEASE_LABEL;
import static software.wings.sm.states.k8s.K8sApplyState.SKIP_FILE_FOR_DEPLOY_PLACEHOLDER_TEXT;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.common.util.concurrent.UncheckedTimeoutException;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.api.model.LoadBalancerIngress;
import io.fabric8.kubernetes.api.model.LoadBalancerStatus;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.harness.beans.FileData;
import io.harness.container.ContainerInfo;
import io.harness.delegate.service.ExecutionConfigOverrideFromFileOnDelegate;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.KubernetesValuesException;
import io.harness.exception.WingsException;
import io.harness.filesystem.FileIo;
import io.harness.k8s.KubernetesContainerService;
import io.harness.k8s.KubernetesHelperService;
import io.harness.k8s.kubectl.AbstractExecutable;
import io.harness.k8s.kubectl.ApplyCommand;
import io.harness.k8s.kubectl.DeleteCommand;
import io.harness.k8s.kubectl.DescribeCommand;
import io.harness.k8s.kubectl.GetCommand;
import io.harness.k8s.kubectl.GetJobCommand;
import io.harness.k8s.kubectl.Kubectl;
import io.harness.k8s.kubectl.RolloutHistoryCommand;
import io.harness.k8s.kubectl.RolloutStatusCommand;
import io.harness.k8s.kubectl.ScaleCommand;
import io.harness.k8s.kubectl.Utils;
import io.harness.k8s.manifest.ManifestHelper;
import io.harness.k8s.model.HarnessAnnotations;
import io.harness.k8s.model.HarnessLabelValues;
import io.harness.k8s.model.HarnessLabels;
import io.harness.k8s.model.HelmVersion;
import io.harness.k8s.model.K8sContainer;
import io.harness.k8s.model.K8sDelegateTaskParams;
import io.harness.k8s.model.K8sPod;
import io.harness.k8s.model.Kind;
import io.harness.k8s.model.KubernetesConfig;
import io.harness.k8s.model.KubernetesResource;
import io.harness.k8s.model.KubernetesResourceComparer;
import io.harness.k8s.model.KubernetesResourceId;
import io.harness.k8s.model.Release;
import io.harness.k8s.model.ReleaseHistory;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogLevel;
import io.harness.serializer.YamlUtils;
import lombok.extern.slf4j.Slf4j;
import me.snowdrop.istio.api.networking.v1alpha3.Destination;
import me.snowdrop.istio.api.networking.v1alpha3.DestinationRule;
import me.snowdrop.istio.api.networking.v1alpha3.DestinationRuleBuilder;
import me.snowdrop.istio.api.networking.v1alpha3.DestinationWeight;
import me.snowdrop.istio.api.networking.v1alpha3.DoneableDestinationRule;
import me.snowdrop.istio.api.networking.v1alpha3.DoneableVirtualService;
import me.snowdrop.istio.api.networking.v1alpha3.HTTPRoute;
import me.snowdrop.istio.api.networking.v1alpha3.PortSelector;
import me.snowdrop.istio.api.networking.v1alpha3.Subset;
import me.snowdrop.istio.api.networking.v1alpha3.TCPRoute;
import me.snowdrop.istio.api.networking.v1alpha3.TLSRoute;
import me.snowdrop.istio.api.networking.v1alpha3.VirtualService;
import me.snowdrop.istio.api.networking.v1alpha3.VirtualServiceBuilder;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.NotEmpty;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.StartedProcess;
import org.zeroturnaround.exec.stream.LogOutputStream;
import software.wings.beans.GitConfig;
import software.wings.beans.GitFileConfig;
import software.wings.beans.appmanifest.ManifestFile;
import software.wings.beans.appmanifest.StoreType;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.beans.k8s.istio.IstioDestinationWeight;
import software.wings.beans.yaml.GitFetchFilesResult;
import software.wings.beans.yaml.GitFile;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.delegatetasks.helm.HelmTaskHelper;
import software.wings.helpers.ext.helm.HelmCommandTemplateFactory;
import software.wings.helpers.ext.helm.HelmHelper;
import software.wings.helpers.ext.helm.request.HelmChartConfigParams;
import software.wings.helpers.ext.helm.request.HelmCommandRequest;
import software.wings.helpers.ext.helm.response.HelmChartInfo;
import software.wings.helpers.ext.k8s.request.K8sDelegateManifestConfig;
import software.wings.helpers.ext.k8s.request.K8sDeleteTaskParameters;
import software.wings.helpers.ext.k8s.request.K8sTaskParameters;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;
import software.wings.helpers.ext.k8s.response.K8sTaskResponse;
import software.wings.helpers.ext.kustomize.KustomizeTaskHelper;
import software.wings.helpers.ext.openshift.OpenShiftDelegateService;
import software.wings.service.intfc.GitService;
import software.wings.service.intfc.security.EncryptionService;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Singleton
@Slf4j
public class K8sTaskHelper {
  @Inject protected DelegateLogService delegateLogService;
  @Inject private transient KubernetesContainerService kubernetesContainerService;
  @Inject private TimeLimiter timeLimiter;
  @Inject private GitService gitService;
  @Inject private EncryptionService encryptionService;
  @Inject private HelmTaskHelper helmTaskHelper;
  @Inject private HelmHelper helmHelper;
  @Inject private KubernetesHelperService kubernetesHelperService;
  @Inject private KustomizeTaskHelper kustomizeTaskHelper;
  @Inject private ExecutionConfigOverrideFromFileOnDelegate delegateLocalConfigService;
  @Inject private OpenShiftDelegateService openShiftDelegateService;

  private static String eventOutputFormat =
      "custom-columns=KIND:involvedObject.kind,NAME:.involvedObject.name,MESSAGE:.message,REASON:.reason";

  private static final int FETCH_FILES_DISPLAY_LIMIT = 100;

  private static String eventWithNamespaceOutputFormat =
      "custom-columns=KIND:involvedObject.kind,NAME:.involvedObject.name,NAMESPACE:.involvedObject.namespace,MESSAGE:.message,REASON:.reason";

  private static final String ocRolloutStatusCommand =
      "{OC_COMMAND_PREFIX} rollout status {RESOURCE_ID} {NAMESPACE}--watch=true";
  private static final String ocRolloutHistoryCommand = "{OC_COMMAND_PREFIX} rollout history {RESOURCE_ID} {NAMESPACE}";

  public static final String ocRolloutUndoCommand =
      "{OC_COMMAND_PREFIX} rollout undo {RESOURCE_ID} {NAMESPACE}{REVISION}";

  public boolean dryRunManifests(Kubectl client, List<KubernetesResource> resources,
      K8sDelegateTaskParams k8sDelegateTaskParams, ExecutionLogCallback executionLogCallback) {
    try {
      executionLogCallback.saveExecutionLog(color("\nValidating manifests with Dry Run", White, Bold), INFO);

      FileIo.writeUtf8StringToFile(
          k8sDelegateTaskParams.getWorkingDirectory() + "/manifests-dry-run.yaml", ManifestHelper.toYaml(resources));

      Kubectl overriddenClient = getOverriddenClient(client, resources, k8sDelegateTaskParams);

      final ApplyCommand dryrun = overriddenClient.apply().filename("manifests-dry-run.yaml").dryrun(true);
      ProcessResult result = runK8sExecutable(k8sDelegateTaskParams, executionLogCallback, dryrun);
      if (result.getExitValue() != 0) {
        executionLogCallback.saveExecutionLog("\nFailed.", INFO, CommandExecutionStatus.FAILURE);
        return false;
      }
    } catch (Exception e) {
      logger.error("Exception in running dry-run", e);
      executionLogCallback.saveExecutionLog("\nFailed.", INFO, CommandExecutionStatus.FAILURE);
      return false;
    }

    executionLogCallback.saveExecutionLog("\nDone.", INFO, CommandExecutionStatus.SUCCESS);
    return true;
  }

  private Kubectl getOverriddenClient(
      Kubectl client, List<KubernetesResource> resources, K8sDelegateTaskParams k8sDelegateTaskParams) {
    List<KubernetesResource> openshiftResourcesList =
        resources.stream()
            .filter(kubernetesResource
                -> K8sTaskHelper.openshiftResources.contains(kubernetesResource.getResourceId().getKind()))
            .collect(Collectors.toList());
    if (isEmpty(openshiftResourcesList)) {
      return client;
    }

    return Kubectl.client(k8sDelegateTaskParams.getOcPath(), k8sDelegateTaskParams.getKubeconfigPath());
  }

  public boolean applyManifests(Kubectl client, List<KubernetesResource> resources,
      K8sDelegateTaskParams k8sDelegateTaskParams, ExecutionLogCallback executionLogCallback) throws Exception {
    FileIo.writeUtf8StringToFile(
        k8sDelegateTaskParams.getWorkingDirectory() + "/manifests.yaml", ManifestHelper.toYaml(resources));

    Kubectl overriddenClient = getOverriddenClient(client, resources, k8sDelegateTaskParams);

    final ApplyCommand applyCommand = overriddenClient.apply().filename("manifests.yaml").record(true);
    ProcessResult result = runK8sExecutable(k8sDelegateTaskParams, executionLogCallback, applyCommand);
    if (result.getExitValue() != 0) {
      executionLogCallback.saveExecutionLog("\nFailed.", INFO, CommandExecutionStatus.FAILURE);
      return false;
    }

    executionLogCallback.saveExecutionLog("\nDone.", INFO, CommandExecutionStatus.SUCCESS);
    return true;
  }

  public boolean deleteManifests(Kubectl client, List<KubernetesResource> resources,
      K8sDelegateTaskParams k8sDelegateTaskParams, ExecutionLogCallback executionLogCallback) throws Exception {
    FileIo.writeUtf8StringToFile(
        k8sDelegateTaskParams.getWorkingDirectory() + "/manifests.yaml", ManifestHelper.toYaml(resources));

    Kubectl overriddenClient = getOverriddenClient(client, resources, k8sDelegateTaskParams);

    final DeleteCommand deleteCommand = overriddenClient.delete().filename("manifests.yaml");
    ProcessResult result = runK8sExecutable(k8sDelegateTaskParams, executionLogCallback, deleteCommand);
    if (result.getExitValue() != 0) {
      executionLogCallback.saveExecutionLog("\nFailed.", INFO, CommandExecutionStatus.FAILURE);
      return false;
    }

    executionLogCallback.saveExecutionLog("\nDone.", INFO, CommandExecutionStatus.SUCCESS);
    return true;
  }
  public boolean doStatusCheck(Kubectl client, KubernetesResourceId resourceId,
      K8sDelegateTaskParams k8sDelegateTaskParams, ExecutionLogCallback executionLogCallback) throws Exception {
    return doStatusCheck(client, resourceId, k8sDelegateTaskParams.getWorkingDirectory(),
        k8sDelegateTaskParams.getOcPath(), k8sDelegateTaskParams.getKubeconfigPath(), executionLogCallback);
  }

  public boolean doHelmStatusCheck(Kubectl client, KubernetesResourceId resourceId,
      HelmCommandRequest helmInstallCommandRequest, ExecutionLogCallback executionLogCallback) throws Exception {
    return doStatusCheck(client, resourceId, helmInstallCommandRequest.getWorkingDir(),
        helmInstallCommandRequest.getOcPath(), KUBECONFIG_FILENAME, executionLogCallback);
  }

  private boolean doStatusCheck(Kubectl client, KubernetesResourceId resourceId, String workingDirectory, String ocPath,
      String kubeconfigPath, ExecutionLogCallback executionLogCallback) throws Exception {
    final String eventFormat = "%-7s: %s";
    final String statusFormat = "%n%-7s: %s";

    GetCommand getEventsCommand =
        client.get().resources("events").namespace(resourceId.getNamespace()).output(eventOutputFormat).watchOnly(true);

    executionLogCallback.saveExecutionLog(GetCommand.getPrintableCommand(getEventsCommand.command()) + "\n");

    boolean success = false;

    StartedProcess eventWatchProcess = null;
    try (LogOutputStream watchInfoStream =
             new LogOutputStream() {
               @Override
               protected void processLine(String line) {
                 if (line.contains(resourceId.getName())) {
                   executionLogCallback.saveExecutionLog(format(eventFormat, "Event", line), INFO);
                 }
               }
             };
         LogOutputStream watchErrorStream =
             new LogOutputStream() {
               @Override
               protected void processLine(String line) {
                 executionLogCallback.saveExecutionLog(format(eventFormat, "Event", line), ERROR);
               }
             };
         LogOutputStream statusInfoStream =
             new LogOutputStream() {
               @Override
               protected void processLine(String line) {
                 executionLogCallback.saveExecutionLog(format(statusFormat, "Status", line), INFO);
               }
             };
         LogOutputStream statusErrorStream =
             new LogOutputStream() {
               @Override
               protected void processLine(String line) {
                 executionLogCallback.saveExecutionLog(format(statusFormat, "Status", line), ERROR);
               }
             }) {
      eventWatchProcess = getEventWatchProcess(workingDirectory, getEventsCommand, watchInfoStream, watchErrorStream);

      ProcessResult result;
      if (Kind.DeploymentConfig.name().equals(resourceId.getKind())) {
        String rolloutStatusCommand = getRolloutStatusCommandForDeploymentConfig(ocPath, kubeconfigPath, resourceId);

        executionLogCallback.saveExecutionLog(
            rolloutStatusCommand.substring(rolloutStatusCommand.indexOf("oc --kubeconfig")) + "\n");

        result = executeCommandUsingUtils(workingDirectory, statusInfoStream, statusErrorStream, rolloutStatusCommand);
      } else {
        RolloutStatusCommand rolloutStatusCommand = client.rollout()
                                                        .status()
                                                        .resource(resourceId.kindNameRef())
                                                        .namespace(resourceId.getNamespace())
                                                        .watch(true);

        executionLogCallback.saveExecutionLog(
            RolloutStatusCommand.getPrintableCommand(rolloutStatusCommand.command()) + "\n");

        result = rolloutStatusCommand.execute(workingDirectory, statusInfoStream, statusErrorStream, false);
      }

      success = result.getExitValue() == 0;

      if (!success) {
        logger.warn(result.outputUTF8());
      }
      return success;
    } catch (Exception e) {
      logger.error("Exception while doing statusCheck", e);
      executionLogCallback.saveExecutionLog("\nFailed.", INFO, CommandExecutionStatus.FAILURE);
      return false;
    } finally {
      if (eventWatchProcess != null) {
        eventWatchProcess.getProcess().destroyForcibly().waitFor();
      }
      if (success) {
        executionLogCallback.saveExecutionLog("\nDone.", INFO, CommandExecutionStatus.SUCCESS);

      } else {
        executionLogCallback.saveExecutionLog("\nFailed.", INFO, CommandExecutionStatus.FAILURE);
      }
    }
  }

  @VisibleForTesting
  StartedProcess getEventWatchProcess(String workingDirectory, GetCommand getEventsCommand,
      LogOutputStream watchInfoStream, LogOutputStream watchErrorStream) throws Exception {
    return getEventsCommand.executeInBackground(workingDirectory, watchInfoStream, watchErrorStream);
  }

  @VisibleForTesting
  ProcessResult executeCommandUsingUtils(K8sDelegateTaskParams k8sDelegateTaskParams, LogOutputStream statusInfoStream,
      LogOutputStream statusErrorStream, String command) throws Exception {
    return executeCommandUsingUtils(
        k8sDelegateTaskParams.getWorkingDirectory(), statusInfoStream, statusErrorStream, command);
  }

  @VisibleForTesting
  ProcessResult executeCommandUsingUtils(String workingDirectory, LogOutputStream statusInfoStream,
      LogOutputStream statusErrorStream, String command) throws Exception {
    return Utils.executeScript(workingDirectory, command, statusInfoStream, statusErrorStream);
  }

  public static String getOcCommandPrefix(K8sDelegateTaskParams k8sDelegateTaskParams) {
    return getOcCommandPrefix(k8sDelegateTaskParams.getOcPath(), k8sDelegateTaskParams.getKubeconfigPath());
  }

  public static String getOcCommandPrefix(String ocPath, String kubeConfigPath) {
    StringBuilder command = new StringBuilder(128);

    if (StringUtils.isNotBlank(ocPath)) {
      command.append(encloseWithQuotesIfNeeded(ocPath));
    } else {
      command.append("oc");
    }

    if (StringUtils.isNotBlank(kubeConfigPath)) {
      command.append(" --kubeconfig=").append(encloseWithQuotesIfNeeded(kubeConfigPath));
    }

    return command.toString();
  }

  private String getRolloutStatusCommandForDeploymentConfig(
      String ocPath, String kubeConfigPath, KubernetesResourceId resourceId) {
    String namespace = "";
    if (StringUtils.isNotBlank(resourceId.getNamespace())) {
      namespace = "--namespace=" + resourceId.getNamespace() + " ";
    }

    return ocRolloutStatusCommand.replace("{OC_COMMAND_PREFIX}", getOcCommandPrefix(ocPath, kubeConfigPath))
        .replace("{RESOURCE_ID}", resourceId.kindNameRef())
        .replace("{NAMESPACE}", namespace);
  }

  boolean doStatusCheckForJob(Kubectl client, KubernetesResourceId resourceId,
      K8sDelegateTaskParams k8sDelegateTaskParams, String statusFormat, ExecutionLogCallback executionLogCallback)
      throws Exception {
    try (LogOutputStream statusInfoStream =
             new LogOutputStream() {
               @Override
               protected void processLine(String line) {
                 executionLogCallback.saveExecutionLog(
                     format(statusFormat, "Status", resourceId.getName(), line), INFO);
               }
             };
         LogOutputStream statusErrorStream =
             new LogOutputStream() {
               @Override
               protected void processLine(String line) {
                 executionLogCallback.saveExecutionLog(
                     format(statusFormat, "Status", resourceId.getName(), line), ERROR);
               }
             }) {
      GetJobCommand jobCompleteCommand = client.getJobCommand(resourceId.getName(), resourceId.getNamespace())
                                             .output("jsonpath='{.status.conditions[?(@.type==\"Complete\")].status}'");
      GetJobCommand jobFailedCommand = client.getJobCommand(resourceId.getName(), resourceId.getNamespace())
                                           .output("jsonpath='{.status.conditions[?(@.type==\"Failed\")].status}'");
      GetJobCommand jobStatusCommand =
          client.getJobCommand(resourceId.getName(), resourceId.getNamespace()).output("jsonpath='{.status}'");
      GetJobCommand jobCompletionTimeCommand = client.getJobCommand(resourceId.getName(), resourceId.getNamespace())
                                                   .output("jsonpath='{.status.completionTime}'");

      executionLogCallback.saveExecutionLog(getPrintableCommand(jobStatusCommand.command()) + "\n");

      return getJobStatus(k8sDelegateTaskParams, statusInfoStream, statusErrorStream, jobCompleteCommand,
          jobFailedCommand, jobStatusCommand, jobCompletionTimeCommand);
    }
  }

  boolean getJobStatus(K8sDelegateTaskParams k8sDelegateTaskParams, LogOutputStream statusInfoStream,
      LogOutputStream statusErrorStream, GetJobCommand jobCompleteCommand, GetJobCommand jobFailedCommand,
      GetJobCommand jobStatusCommand, GetJobCommand jobCompletionTimeCommand) throws Exception {
    while (true) {
      jobStatusCommand.execute(k8sDelegateTaskParams.getWorkingDirectory(), statusInfoStream, statusErrorStream, false);

      ProcessResult result = jobCompleteCommand.execute(k8sDelegateTaskParams.getWorkingDirectory(), null, null, false);

      boolean success = 0 == result.getExitValue();
      if (!success) {
        logger.warn(result.outputUTF8());
        return false;
      }

      // cli command outputs with single quotes
      String jobStatus = result.outputUTF8().replace("'", "");
      if ("True".equals(jobStatus)) {
        result = jobCompletionTimeCommand.execute(k8sDelegateTaskParams.getWorkingDirectory(), null, null, false);
        success = 0 == result.getExitValue();
        if (!success) {
          logger.warn(result.outputUTF8());
          return false;
        }

        String completionTime = result.outputUTF8().replace("'", "");
        if (isNotBlank(completionTime)) {
          return true;
        }
      }

      result = jobFailedCommand.execute(k8sDelegateTaskParams.getWorkingDirectory(), null, null, false);

      success = 0 == result.getExitValue();
      if (!success) {
        logger.warn(result.outputUTF8());
        return false;
      }

      jobStatus = result.outputUTF8().replace("'", "");
      if ("True".equals(jobStatus)) {
        return false;
      }

      sleep(ofSeconds(5));
    }
  }

  private boolean doStatusCheckForWorkloads(Kubectl client, KubernetesResourceId resourceId,
      K8sDelegateTaskParams k8sDelegateTaskParams, String statusFormat, ExecutionLogCallback executionLogCallback)
      throws Exception {
    try (LogOutputStream statusErrorStream =
             new LogOutputStream() {
               @Override
               protected void processLine(String line) {
                 executionLogCallback.saveExecutionLog(
                     format(statusFormat, "Status", resourceId.getName(), line), ERROR);
               }
             };
         LogOutputStream statusInfoStream =
             new LogOutputStream() {
               @Override
               protected void processLine(String line) {
                 executionLogCallback.saveExecutionLog(
                     format(statusFormat, "Status", resourceId.getName(), line), INFO);
               }
             }) {
      ProcessResult result;

      if (Kind.DeploymentConfig.name().equals(resourceId.getKind())) {
        String rolloutStatusCommand = getRolloutStatusCommandForDeploymentConfig(
            k8sDelegateTaskParams.getOcPath(), k8sDelegateTaskParams.getKubeconfigPath(), resourceId);

        executionLogCallback.saveExecutionLog(
            rolloutStatusCommand.substring(rolloutStatusCommand.indexOf("oc --kubeconfig")) + "\n");

        result =
            executeCommandUsingUtils(k8sDelegateTaskParams, statusInfoStream, statusErrorStream, rolloutStatusCommand);
      } else {
        RolloutStatusCommand rolloutStatusCommand = client.rollout()
                                                        .status()
                                                        .resource(resourceId.kindNameRef())
                                                        .namespace(resourceId.getNamespace())
                                                        .watch(true);

        executionLogCallback.saveExecutionLog(
            RolloutStatusCommand.getPrintableCommand(rolloutStatusCommand.command()) + "\n");

        result = rolloutStatusCommand.execute(
            k8sDelegateTaskParams.getWorkingDirectory(), statusInfoStream, statusErrorStream, false);
      }

      boolean success = 0 == result.getExitValue();
      if (!success) {
        logger.warn(result.outputUTF8());
      }

      return success;
    }
  }

  public boolean doStatusCheckAllResourcesForHelm(Kubectl client, List<KubernetesResourceId> resourceIds, String ocPath,
      String workingDir, String namespace, String kubeconfigPath, ExecutionLogCallback executionLogCallback)
      throws Exception {
    return doStatusCheckForAllResources(client, resourceIds,
        K8sDelegateTaskParams.builder()
            .ocPath(ocPath)
            .workingDirectory(workingDir)
            .kubeconfigPath(kubeconfigPath)
            .build(),
        namespace, executionLogCallback, false);
  }

  public boolean doStatusCheckForAllResources(Kubectl client, List<KubernetesResourceId> resourceIds,
      K8sDelegateTaskParams k8sDelegateTaskParams, String namespace, ExecutionLogCallback executionLogCallback,
      boolean denoteOverallSuccess) throws Exception {
    if (isEmpty(resourceIds)) {
      return true;
    }

    int maxResourceNameLength = 0;
    for (KubernetesResourceId kubernetesResourceId : resourceIds) {
      maxResourceNameLength = Math.max(maxResourceNameLength, kubernetesResourceId.getName().length());
    }

    final String eventErrorFormat = "%-7s: %s";
    final String eventInfoFormat = "%-7s: %-" + maxResourceNameLength + "s   %s";
    final String statusFormat = "%n%-7s: %-" + maxResourceNameLength + "s   %s";

    Set<String> namespaces = resourceIds.stream().map(KubernetesResourceId::getNamespace).collect(toSet());
    namespaces.add(namespace);
    List<GetCommand> getEventCommands = namespaces.stream()
                                            .map(ns
                                                -> client.get()
                                                       .resources("events")
                                                       .namespace(ns)
                                                       .output(eventWithNamespaceOutputFormat)
                                                       .watchOnly(true))
                                            .collect(toList());

    for (GetCommand cmd : getEventCommands) {
      executionLogCallback.saveExecutionLog(GetCommand.getPrintableCommand(cmd.command()) + "\n");
    }

    boolean success = false;

    List<StartedProcess> eventWatchProcesses = new ArrayList<>();
    try (LogOutputStream watchInfoStream =
             new LogOutputStream() {
               @Override
               protected void processLine(String line) {
                 Optional<KubernetesResourceId> filteredResourceId =
                     resourceIds.parallelStream()
                         .filter(kubernetesResourceId
                             -> line.contains(isNotBlank(kubernetesResourceId.getNamespace())
                                        ? kubernetesResourceId.getNamespace()
                                        : namespace)
                                 && line.contains(kubernetesResourceId.getName()))
                         .findFirst();

                 filteredResourceId.ifPresent(kubernetesResourceId
                     -> executionLogCallback.saveExecutionLog(
                         format(eventInfoFormat, "Event", kubernetesResourceId.getName(), line), INFO));
               }
             };
         LogOutputStream watchErrorStream =
             new LogOutputStream() {
               @Override
               protected void processLine(String line) {
                 executionLogCallback.saveExecutionLog(format(eventErrorFormat, "Event", line), ERROR);
               }
             }) {
      for (GetCommand getEventsCommand : getEventCommands) {
        eventWatchProcesses.add(getEventWatchProcess(
            k8sDelegateTaskParams.getWorkingDirectory(), getEventsCommand, watchInfoStream, watchErrorStream));
      }

      for (KubernetesResourceId kubernetesResourceId : resourceIds) {
        if (Kind.Job.name().equals(kubernetesResourceId.getKind())) {
          success = doStatusCheckForJob(
              client, kubernetesResourceId, k8sDelegateTaskParams, statusFormat, executionLogCallback);
        } else {
          success = doStatusCheckForWorkloads(
              client, kubernetesResourceId, k8sDelegateTaskParams, statusFormat, executionLogCallback);
        }

        if (!success) {
          break;
        }
      }

      return success;
    } catch (Exception e) {
      logger.error("Exception while doing statusCheck", e);
      executionLogCallback.saveExecutionLog("\nFailed.", INFO, CommandExecutionStatus.FAILURE);
      return false;
    } finally {
      for (StartedProcess eventWatchProcess : eventWatchProcesses) {
        eventWatchProcess.getProcess().destroyForcibly().waitFor();
      }
      if (success) {
        if (denoteOverallSuccess) {
          executionLogCallback.saveExecutionLog("\nDone.", INFO, CommandExecutionStatus.SUCCESS);
        }
      } else {
        executionLogCallback.saveExecutionLog(
            format("%nStatus check for resources in namespace [%s] failed.", namespace), INFO,
            CommandExecutionStatus.FAILURE);
      }
    }
  }

  public boolean scale(Kubectl client, K8sDelegateTaskParams k8sDelegateTaskParams, KubernetesResourceId resourceId,
      int targetReplicaCount, ExecutionLogCallback executionLogCallback) throws Exception {
    executionLogCallback.saveExecutionLog("\nScaling " + resourceId.kindNameRef());

    final ScaleCommand scaleCommand = client.scale()
                                          .resource(resourceId.kindNameRef())
                                          .replicas(targetReplicaCount)
                                          .namespace(resourceId.getNamespace());
    ProcessResult result = runK8sExecutable(k8sDelegateTaskParams, executionLogCallback, scaleCommand);
    if (result.getExitValue() == 0) {
      executionLogCallback.saveExecutionLog("\nDone.", INFO, CommandExecutionStatus.SUCCESS);
      return true;
    } else {
      executionLogCallback.saveExecutionLog("\nFailed.", INFO, CommandExecutionStatus.FAILURE);
      logger.warn("Failed to scale workload. Error {}", result.getOutput());
      return false;
    }
  }

  @VisibleForTesting
  ProcessResult runK8sExecutable(K8sDelegateTaskParams k8sDelegateTaskParams, ExecutionLogCallback executionLogCallback,
      AbstractExecutable executable) throws Exception {
    return executeCommand(executable, k8sDelegateTaskParams.getWorkingDirectory(), executionLogCallback);
  }

  public void cleanup(Kubectl client, K8sDelegateTaskParams k8sDelegateTaskParams, ReleaseHistory releaseHistory,
      ExecutionLogCallback executionLogCallback) throws Exception {
    final int lastSuccessfulReleaseNumber =
        (releaseHistory.getLastSuccessfulRelease() != null) ? releaseHistory.getLastSuccessfulRelease().getNumber() : 0;

    if (lastSuccessfulReleaseNumber == 0) {
      executionLogCallback.saveExecutionLog("\nNo previous successful release found.");
    } else {
      executionLogCallback.saveExecutionLog("\nPrevious Successful Release is " + lastSuccessfulReleaseNumber);
    }

    executionLogCallback.saveExecutionLog("\nCleaning up older and failed releases");

    for (int releaseIndex = releaseHistory.getReleases().size() - 1; releaseIndex >= 0; releaseIndex--) {
      Release release = releaseHistory.getReleases().get(releaseIndex);
      if (release.getNumber() < lastSuccessfulReleaseNumber || release.getStatus() == Failed) {
        for (int resourceIndex = release.getResources().size() - 1; resourceIndex >= 0; resourceIndex--) {
          KubernetesResourceId resourceId = release.getResources().get(resourceIndex);
          if (resourceId.isVersioned()) {
            DeleteCommand deleteCommand =
                client.delete().resources(resourceId.kindNameRef()).namespace(resourceId.getNamespace());
            ProcessResult result = runK8sExecutable(k8sDelegateTaskParams, executionLogCallback, deleteCommand);
            if (result.getExitValue() != 0) {
              logger.warn("Failed to delete resource {}. Error {}", resourceId.kindNameRef(), result.getOutput());
            }
          }
        }
      }
    }
    releaseHistory.getReleases().removeIf(
        release -> release.getNumber() < lastSuccessfulReleaseNumber || release.getStatus() == Failed);
  }

  public void delete(Kubectl client, K8sDelegateTaskParams k8sDelegateTaskParams,
      List<KubernetesResourceId> kubernetesResourceIds, ExecutionLogCallback executionLogCallback) throws Exception {
    for (KubernetesResourceId resourceId : kubernetesResourceIds) {
      DeleteCommand deleteCommand =
          client.delete().resources(resourceId.kindNameRef()).namespace(resourceId.getNamespace());
      ProcessResult result = runK8sExecutable(k8sDelegateTaskParams, executionLogCallback, deleteCommand);
      if (result.getExitValue() != 0) {
        logger.warn("Failed to delete resource {}. Error {}", resourceId.kindNameRef(), result.getOutput());
      }
    }

    executionLogCallback.saveExecutionLog("Done", INFO, CommandExecutionStatus.SUCCESS);
  }

  public static LogOutputStream getExecutionLogOutputStream(
      ExecutionLogCallback executionLogCallback, LogLevel logLevel) {
    return new LogOutputStream() {
      @Override
      protected void processLine(String line) {
        executionLogCallback.saveExecutionLog(line, logLevel);
      }
    };
  }

  public static LogOutputStream getEmptyLogOutputStream() {
    return new LogOutputStream() {
      @Override
      protected void processLine(String line) {}
    };
  }

  public static ProcessResult executeCommand(
      AbstractExecutable command, String workingDirectory, ExecutionLogCallback executionLogCallback) throws Exception {
    try (LogOutputStream logOutputStream = getExecutionLogOutputStream(executionLogCallback, INFO);
         LogOutputStream logErrorStream = getExecutionLogOutputStream(executionLogCallback, ERROR);) {
      return command.execute(workingDirectory, logOutputStream, logErrorStream, true);
    }
  }

  public static ProcessResult executeCommandSilent(AbstractExecutable command, String workingDirectory)
      throws Exception {
    try (LogOutputStream emptyLogOutputStream = getEmptyLogOutputStream();) {
      return command.execute(workingDirectory, emptyLogOutputStream, emptyLogOutputStream, false);
    }
  }

  public void describe(Kubectl client, K8sDelegateTaskParams k8sDelegateTaskParams,
      ExecutionLogCallback executionLogCallback) throws Exception {
    final DescribeCommand describeCommand = client.describe().filename("manifests.yaml");
    runK8sExecutable(k8sDelegateTaskParams, executionLogCallback, describeCommand);
  }

  private String getRolloutHistoryCommandForDeploymentConfig(
      K8sDelegateTaskParams k8sDelegateTaskParams, KubernetesResourceId resourceId) {
    String namespace = "";
    if (StringUtils.isNotBlank(resourceId.getNamespace())) {
      namespace = "--namespace=" + resourceId.getNamespace() + " ";
    }

    return ocRolloutHistoryCommand.replace("{OC_COMMAND_PREFIX}", getOcCommandPrefix(k8sDelegateTaskParams))
        .replace("{RESOURCE_ID}", resourceId.kindNameRef())
        .replace("{NAMESPACE}", namespace)
        .trim();
  }

  public String getLatestRevision(
      Kubectl client, KubernetesResourceId resourceId, K8sDelegateTaskParams k8sDelegateTaskParams) throws Exception {
    if (Kind.DeploymentConfig.name().equals(resourceId.getKind())) {
      String rolloutHistoryCommand = getRolloutHistoryCommandForDeploymentConfig(k8sDelegateTaskParams, resourceId);

      try (LogOutputStream emptyLogOutputStream = getEmptyLogOutputStream();) {
        ProcessResult result = executeCommandUsingUtils(
            k8sDelegateTaskParams, emptyLogOutputStream, emptyLogOutputStream, rolloutHistoryCommand);

        if (result.getExitValue() == 0) {
          String[] lines = result.outputUTF8().split("\\r?\\n");
          return lines[lines.length - 1].split("\t")[0];
        }
      }

    } else {
      RolloutHistoryCommand rolloutHistoryCommand =
          client.rollout().history().resource(resourceId.kindNameRef()).namespace(resourceId.getNamespace());
      ProcessResult result = runK8sExecutableSilent(k8sDelegateTaskParams, rolloutHistoryCommand);
      if (result.getExitValue() == 0) {
        return parseLatestRevisionNumberFromRolloutHistory(result.outputUTF8());
      }
    }

    return "";
  }

  public Integer getCurrentReplicas(
      Kubectl client, KubernetesResourceId resourceId, K8sDelegateTaskParams k8sDelegateTaskParams) throws Exception {
    GetCommand getCommand = client.get()
                                .resources(resourceId.kindNameRef())
                                .namespace(resourceId.getNamespace())
                                .output("jsonpath={$.spec.replicas}");
    ProcessResult result = runK8sExecutableSilent(k8sDelegateTaskParams, getCommand);
    if (result.getExitValue() == 0) {
      return Integer.valueOf(result.outputUTF8());
    } else {
      return null;
    }
  }

  @VisibleForTesting
  ProcessResult runK8sExecutableSilent(K8sDelegateTaskParams k8sDelegateTaskParams, AbstractExecutable executable)
      throws Exception {
    return executeCommandSilent(executable, k8sDelegateTaskParams.getWorkingDirectory());
  }

  private String writeValuesToFile(String directoryPath, List<String> valuesFiles) throws Exception {
    StringBuilder valuesFilesOptionsBuilder = new StringBuilder(128);

    for (int i = 0; i < valuesFiles.size(); i++) {
      validateValuesFileContents(valuesFiles.get(i));
      String valuesFileName = format("values-%d.yaml", i);
      FileIo.writeUtf8StringToFile(directoryPath + '/' + valuesFileName, valuesFiles.get(i));
      valuesFilesOptionsBuilder.append(" -f ").append(valuesFileName);
    }

    return valuesFilesOptionsBuilder.toString();
  }

  private boolean writeManifestFilesToDirectory(
      List<ManifestFile> manifestFiles, String manifestFilesDirectory, ExecutionLogCallback executionLogCallback) {
    String directoryPath = Paths.get(manifestFilesDirectory).toString();

    try {
      for (int i = 0; i < manifestFiles.size(); i++) {
        ManifestFile manifestFile = manifestFiles.get(i);
        if (StringUtils.equals(values_filename, manifestFile.getFileName())) {
          continue;
        }

        Path filePath = Paths.get(directoryPath, manifestFile.getFileName());
        Path parent = filePath.getParent();
        if (parent == null) {
          throw new WingsException("Failed to create file at path " + filePath.toString());
        }

        createDirectoryIfDoesNotExist(parent.toString());
        FileIo.writeUtf8StringToFile(filePath.toString(), manifestFile.getFileContent());
      }

      return true;
    } catch (Exception ex) {
      executionLogCallback.saveExecutionLog(ExceptionUtils.getMessage(ex), ERROR, CommandExecutionStatus.FAILURE);
      return false;
    }
  }

  public List<ManifestFile> renderTemplateForHelm(String helmPath, String manifestFilesDirectory,
      List<String> valuesFiles, String releaseName, String namespace, ExecutionLogCallback executionLogCallback,
      HelmVersion helmVersion, long timeoutInMillis) throws Exception {
    String valuesFileOptions = writeValuesToFile(manifestFilesDirectory, valuesFiles);
    logger.info("Values file options: " + valuesFileOptions);

    printHelmPath(executionLogCallback, helmPath);

    List<ManifestFile> result = new ArrayList<>();
    try (LogOutputStream logErrorStream = getExecutionLogOutputStream(executionLogCallback, ERROR)) {
      String helmTemplateCommand = getHelmCommandForRender(
          helmPath, manifestFilesDirectory, releaseName, namespace, valuesFileOptions, helmVersion);
      printHelmTemplateCommand(executionLogCallback, helmTemplateCommand);

      ProcessResult processResult =
          executeShellCommand(manifestFilesDirectory, helmTemplateCommand, logErrorStream, timeoutInMillis);
      if (processResult.getExitValue() != 0) {
        throw new WingsException(format("Failed to render helm chart. Error %s", processResult.getOutput().getUTF8()));
      }

      result.add(ManifestFile.builder().fileName("manifest.yaml").fileContent(processResult.outputUTF8()).build());
    }

    return result;
  }

  private List<ManifestFile> readManifestFilesFromDirectory(String manifestFilesDirectory) {
    List<FileData> fileDataList;
    Path directory = Paths.get(manifestFilesDirectory);

    try {
      fileDataList = getFilesUnderPath(directory.toString());
    } catch (Exception ex) {
      logger.error(ExceptionUtils.getMessage(ex));
      throw new WingsException("Failed to get files. Error: " + ExceptionUtils.getMessage(ex));
    }

    List<ManifestFile> manifestFiles = new ArrayList<>();
    for (FileData fileData : fileDataList) {
      if (isValidManifestFile(fileData.getFilePath())) {
        manifestFiles.add(ManifestFile.builder()
                              .fileName(fileData.getFilePath())
                              .fileContent(new String(fileData.getFileBytes(), StandardCharsets.UTF_8))
                              .build());
      } else {
        logger.info("Found file [{}] with unsupported extension", fileData.getFilePath());
      }
    }

    return manifestFiles;
  }

  private List<ManifestFile> renderManifestFilesForGoTemplate(K8sDelegateTaskParams k8sDelegateTaskParams,
      List<ManifestFile> manifestFiles, List<String> valuesFiles, ExecutionLogCallback executionLogCallback,
      long timeoutInMillis) throws Exception {
    if (isEmpty(valuesFiles)) {
      executionLogCallback.saveExecutionLog("No values.yaml file found. Skipping template rendering.");
      return manifestFiles;
    }

    String valuesFileOptions = null;
    try {
      valuesFileOptions = writeValuesToFile(k8sDelegateTaskParams.getWorkingDirectory(), valuesFiles);
    } catch (KubernetesValuesException kvexception) {
      String message = kvexception.getParams().get("reason").toString();
      executionLogCallback.saveExecutionLog(message, ERROR);
      throw new KubernetesValuesException(message, kvexception.getCause());
    }

    logger.info("Values file options: " + valuesFileOptions);

    List<ManifestFile> result = new ArrayList<>();

    executionLogCallback.saveExecutionLog(color("\nRendering manifest files using go template", White, Bold));
    executionLogCallback.saveExecutionLog(
        color("Only manifest files with [.yaml] or [.yml] extension will be processed", White, Bold));

    for (ManifestFile manifestFile : manifestFiles) {
      if (StringUtils.equals(values_filename, manifestFile.getFileName())) {
        continue;
      }

      FileIo.writeUtf8StringToFile(
          k8sDelegateTaskParams.getWorkingDirectory() + "/template.yaml", manifestFile.getFileContent());

      try (LogOutputStream logErrorStream = getExecutionLogOutputStream(executionLogCallback, ERROR)) {
        String goTemplateCommand = encloseWithQuotesIfNeeded(k8sDelegateTaskParams.getGoTemplateClientPath())
            + " -t template.yaml " + valuesFileOptions;
        ProcessResult processResult = executeShellCommand(
            k8sDelegateTaskParams.getWorkingDirectory(), goTemplateCommand, logErrorStream, timeoutInMillis);

        if (processResult.getExitValue() != 0) {
          throw new InvalidRequestException(format("Failed to render template for %s. Error %s",
                                                manifestFile.getFileName(), processResult.getOutput().getUTF8()),
              USER);
        }

        result.add(ManifestFile.builder()
                       .fileName(manifestFile.getFileName())
                       .fileContent(processResult.outputUTF8())
                       .build());
      }
    }

    return result;
  }

  public List<ManifestFile> renderTemplate(K8sDelegateTaskParams k8sDelegateTaskParams,
      K8sDelegateManifestConfig k8sDelegateManifestConfig, String manifestFilesDirectory, List<String> valuesFiles,
      String releaseName, String namespace, ExecutionLogCallback executionLogCallback,
      K8sTaskParameters k8sTaskParameters) throws Exception {
    StoreType storeType = k8sDelegateManifestConfig.getManifestStoreTypes();
    long timeoutInMillis = getTimeoutMillisFromMinutes(k8sTaskParameters.getTimeoutIntervalInMin());

    switch (storeType) {
      case Local:
      case Remote:
        List<ManifestFile> manifestFiles = readManifestFilesFromDirectory(manifestFilesDirectory);
        return renderManifestFilesForGoTemplate(
            k8sDelegateTaskParams, manifestFiles, valuesFiles, executionLogCallback, timeoutInMillis);

      case HelmSourceRepo:
        return renderTemplateForHelm(k8sDelegateTaskParams.getHelmPath(), manifestFilesDirectory, valuesFiles,
            releaseName, namespace, executionLogCallback, k8sTaskParameters.getHelmVersion(), timeoutInMillis);

      case HelmChartRepo:
        manifestFilesDirectory =
            Paths.get(manifestFilesDirectory, k8sDelegateManifestConfig.getHelmChartConfigParams().getChartName())
                .toString();
        return renderTemplateForHelm(k8sDelegateTaskParams.getHelmPath(), manifestFilesDirectory, valuesFiles,
            releaseName, namespace, executionLogCallback, k8sTaskParameters.getHelmVersion(), timeoutInMillis);

      case KustomizeSourceRepo:
        return kustomizeTaskHelper.build(manifestFilesDirectory, k8sDelegateTaskParams.getKustomizeBinaryPath(),
            k8sDelegateManifestConfig.getKustomizeConfig(), executionLogCallback);
      case OC_TEMPLATES:
        return openShiftDelegateService.processTemplatization(manifestFilesDirectory, k8sDelegateTaskParams.getOcPath(),
            k8sDelegateManifestConfig.getGitFileConfig().getFilePath(), executionLogCallback, valuesFiles);

      default:
        unhandled(storeType);
    }

    return new ArrayList<>();
  }

  public List<ManifestFile> renderTemplateForGivenFiles(K8sDelegateTaskParams k8sDelegateTaskParams,
      K8sDelegateManifestConfig k8sDelegateManifestConfig, String manifestFilesDirectory,
      @NotEmpty List<String> filesList, List<String> valuesFiles, String releaseName, String namespace,
      ExecutionLogCallback executionLogCallback, K8sTaskParameters k8sTaskParameters) throws Exception {
    StoreType storeType = k8sDelegateManifestConfig.getManifestStoreTypes();
    long timeoutInMillis = getTimeoutMillisFromMinutes(k8sTaskParameters.getTimeoutIntervalInMin());

    switch (storeType) {
      case Local:
      case Remote:
        List<ManifestFile> manifestFiles =
            readFilesFromDirectory(manifestFilesDirectory, filesList, executionLogCallback);
        return renderManifestFilesForGoTemplate(
            k8sDelegateTaskParams, manifestFiles, valuesFiles, executionLogCallback, timeoutInMillis);

      case HelmSourceRepo:
        return renderTemplateForHelmChartFiles(k8sDelegateTaskParams, manifestFilesDirectory, filesList, valuesFiles,
            releaseName, namespace, executionLogCallback, k8sTaskParameters.getHelmVersion(), timeoutInMillis);

      case HelmChartRepo:
        manifestFilesDirectory =
            Paths.get(manifestFilesDirectory, k8sDelegateManifestConfig.getHelmChartConfigParams().getChartName())
                .toString();
        return renderTemplateForHelmChartFiles(k8sDelegateTaskParams, manifestFilesDirectory, filesList, valuesFiles,
            releaseName, namespace, executionLogCallback, k8sTaskParameters.getHelmVersion(), timeoutInMillis);
      case KustomizeSourceRepo:
        return kustomizeTaskHelper.buildForApply(k8sDelegateTaskParams.getKustomizeBinaryPath(),
            k8sDelegateManifestConfig.getKustomizeConfig(), manifestFilesDirectory, filesList, executionLogCallback);

      default:
        unhandled(storeType);
    }

    return new ArrayList<>();
  }

  private static boolean isValidManifestFile(String filename) {
    return (StringUtils.endsWith(filename, yaml_file_extension) || StringUtils.endsWith(filename, yml_file_extension))
        && !StringUtils.equals(filename, values_filename);
  }

  public List<KubernetesResource> readManifestAndOverrideLocalSecrets(
      List<ManifestFile> manifestFiles, ExecutionLogCallback executionLogCallback, boolean overrideLocalSecrets) {
    if (overrideLocalSecrets) {
      replaceManifestPlaceholdersWithLocalDelegateSecrets(manifestFiles);
    }
    return readManifests(manifestFiles, executionLogCallback);
  }

  public List<KubernetesResource> getResourcesFromManifests(K8sDelegateTaskParams k8sDelegateTaskParams,
      K8sDelegateManifestConfig k8sDelegateManifestConfig, String manifestFilesDirectory,
      @NotEmpty List<String> filesList, List<String> valuesFiles, String releaseName, String namespace,
      ExecutionLogCallback executionLogCallback, K8sTaskParameters k8sTaskParameters) throws Exception {
    List<ManifestFile> manifestFiles =
        renderTemplateForGivenFiles(k8sDelegateTaskParams, k8sDelegateManifestConfig, manifestFilesDirectory, filesList,
            valuesFiles, releaseName, namespace, executionLogCallback, k8sTaskParameters);
    if (isEmpty(manifestFiles)) {
      return new ArrayList<>();
    }

    List<KubernetesResource> resources = readManifests(manifestFiles, executionLogCallback);
    setNamespaceToKubernetesResourcesIfRequired(resources, namespace);

    return resources;
  }
  public List<KubernetesResource> readManifests(
      List<ManifestFile> manifestFiles, ExecutionLogCallback executionLogCallback) {
    List<KubernetesResource> result = new ArrayList<>();

    for (ManifestFile manifestFile : manifestFiles) {
      if (isValidManifestFile(manifestFile.getFileName())) {
        try {
          result.addAll(ManifestHelper.processYaml(manifestFile.getFileContent()));
        } catch (Exception e) {
          executionLogCallback.saveExecutionLog("Exception while processing " + manifestFile.getFileName(), ERROR);
          throw e;
        }
      }
    }

    return result.stream().sorted(new KubernetesResourceComparer()).collect(toList());
  }

  private void replaceManifestPlaceholdersWithLocalDelegateSecrets(List<ManifestFile> manifestFiles) {
    for (ManifestFile manifestFile : manifestFiles) {
      manifestFile.setFileContent(
          delegateLocalConfigService.replacePlaceholdersWithLocalConfig(manifestFile.getFileContent()));
    }
  }

  public void setNamespaceToKubernetesResourcesIfRequired(
      List<KubernetesResource> kubernetesResources, String namespace) {
    if (isEmpty(kubernetesResources)) {
      return;
    }

    for (KubernetesResource kubernetesResource : kubernetesResources) {
      if (isBlank(kubernetesResource.getResourceId().getNamespace())) {
        kubernetesResource.getResourceId().setNamespace(namespace);
      }
    }
  }

  public String getResourcesInTableFormat(List<KubernetesResource> resources) {
    int maxKindLength = 16;
    int maxNameLength = 36;
    for (KubernetesResource resource : resources) {
      KubernetesResourceId id = resource.getResourceId();
      if (id.getKind().length() > maxKindLength) {
        maxKindLength = id.getKind().length();
      }

      if (id.getName().length() > maxNameLength) {
        maxNameLength = id.getName().length();
      }
    }

    maxKindLength += 4;
    maxNameLength += 4;

    StringBuilder sb = new StringBuilder(1024);
    String tableFormat = "%-" + maxKindLength + "s%-" + maxNameLength + "s%-10s";
    sb.append(System.lineSeparator())
        .append(color(format(tableFormat, "Kind", "Name", "Versioned"), White, Bold))
        .append(System.lineSeparator());

    for (KubernetesResource resource : resources) {
      KubernetesResourceId id = resource.getResourceId();
      sb.append(color(format(tableFormat, id.getKind(), id.getName(), id.isVersioned()), Gray))
          .append(System.lineSeparator());
    }

    return sb.toString();
  }

  public static String getResourcesInStringFormat(List<KubernetesResourceId> resourceIds) {
    StringBuilder sb = new StringBuilder(1024);
    resourceIds.forEach(resourceId -> { sb.append("\n- ").append(resourceId.namespaceKindNameRef()); });
    return sb.toString();
  }

  @VisibleForTesting
  String getManifestFileNamesInLogFormat(String manifestFilesDirectory) throws IOException {
    Path basePath = Paths.get(manifestFilesDirectory);
    try (Stream<Path> paths = Files.walk(basePath)) {
      return generateTruncatedFileListForLogging(basePath, paths);
    }
  }

  @VisibleForTesting
  String generateTruncatedFileListForLogging(Path basePath, Stream<Path> paths) {
    StringBuilder sb = new StringBuilder(1024);
    AtomicInteger filesTraversed = new AtomicInteger(0);
    paths.filter(Files::isRegularFile).forEach(each -> {
      if (filesTraversed.getAndIncrement() <= FETCH_FILES_DISPLAY_LIMIT) {
        sb.append(color(format("- %s", getRelativePath(each.toString(), basePath.toString())), Gray))
            .append(System.lineSeparator());
      }
    });
    if (filesTraversed.get() > FETCH_FILES_DISPLAY_LIMIT) {
      sb.append(color(format("- ..%d more", filesTraversed.get() - FETCH_FILES_DISPLAY_LIMIT), Gray))
          .append(System.lineSeparator());
    }

    return sb.toString();
  }

  private void printGitConfigInExecutionLogs(
      GitConfig gitConfig, GitFileConfig gitFileConfig, ExecutionLogCallback executionLogCallback) {
    executionLogCallback.saveExecutionLog("\n" + color("Fetching manifest files", White, Bold));
    executionLogCallback.saveExecutionLog("Git connector Url: " + gitConfig.getRepoUrl());
    if (gitFileConfig.isUseBranch()) {
      executionLogCallback.saveExecutionLog("Branch: " + gitFileConfig.getBranch());
    } else {
      executionLogCallback.saveExecutionLog("CommitId: " + gitFileConfig.getCommitId());
    }
    executionLogCallback.saveExecutionLog("\nFetching manifest files at path: "
        + (isBlank(gitFileConfig.getFilePath()) ? "." : gitFileConfig.getFilePath()));
  }

  private boolean downloadManifestFilesFromGit(K8sDelegateManifestConfig delegateManifestConfig,
      String manifestFilesDirectory, ExecutionLogCallback executionLogCallback) {
    if (isBlank(delegateManifestConfig.getGitFileConfig().getFilePath())) {
      delegateManifestConfig.getGitFileConfig().setFilePath(StringUtils.EMPTY);
    }

    try {
      GitFileConfig gitFileConfig = delegateManifestConfig.getGitFileConfig();
      GitConfig gitConfig = delegateManifestConfig.getGitConfig();
      printGitConfigInExecutionLogs(gitConfig, gitFileConfig, executionLogCallback);

      encryptionService.decrypt(gitConfig, delegateManifestConfig.getEncryptedDataDetails());

      gitService.downloadFiles(delegateManifestConfig.getGitConfig(), gitFileConfig.getConnectorId(),
          gitFileConfig.getCommitId(), gitFileConfig.getBranch(), asList(gitFileConfig.getFilePath()),
          gitFileConfig.isUseBranch(), manifestFilesDirectory);

      executionLogCallback.saveExecutionLog(color("Successfully fetched following files:", White, Bold));
      executionLogCallback.saveExecutionLog(getManifestFileNamesInLogFormat(manifestFilesDirectory));
      executionLogCallback.saveExecutionLog("Done.", INFO, CommandExecutionStatus.SUCCESS);

      return true;
    } catch (Exception e) {
      logger.error("Failure in fetching files from git", e);
      executionLogCallback.saveExecutionLog(
          "Failed to download manifest files from git. " + ExceptionUtils.getMessage(e), ERROR,
          CommandExecutionStatus.FAILURE);
      return false;
    }
  }

  public boolean fetchManifestFilesAndWriteToDirectory(K8sDelegateManifestConfig delegateManifestConfig,
      String manifestFilesDirectory, ExecutionLogCallback executionLogCallback, long timeoutInMillis) {
    StoreType storeType = delegateManifestConfig.getManifestStoreTypes();
    switch (storeType) {
      case Local:
        return writeManifestFilesToDirectory(
            delegateManifestConfig.getManifestFiles(), manifestFilesDirectory, executionLogCallback);

      case OC_TEMPLATES:
      case Remote:
      case HelmSourceRepo:
      case KustomizeSourceRepo:
        return downloadManifestFilesFromGit(delegateManifestConfig, manifestFilesDirectory, executionLogCallback);

      case HelmChartRepo:
        return downloadFilesFromChartRepo(
            delegateManifestConfig, manifestFilesDirectory, executionLogCallback, timeoutInMillis);

      default:
        unhandled(storeType);
    }

    return false;
  }

  public HelmChartInfo getHelmChartDetails(K8sDelegateManifestConfig delegateManifestConfig, String workingDirectory) {
    HelmChartInfo helmChartInfo = null;
    try {
      if (delegateManifestConfig != null) {
        StoreType manifestStoreType = delegateManifestConfig.getManifestStoreTypes();
        if (StoreType.HelmSourceRepo == manifestStoreType || StoreType.HelmChartRepo == manifestStoreType) {
          String chartName = Optional.ofNullable(delegateManifestConfig.getHelmChartConfigParams())
                                 .map(HelmChartConfigParams::getChartName)
                                 .orElse("");
          helmChartInfo =
              helmTaskHelper.getHelmChartInfoFromChartDirectory(Paths.get(workingDirectory, chartName).toString());
        }
      }
    } catch (IOException ex) {
      logger.error("Error while fetching helm chart info", ex);
    }

    return helmChartInfo;
  }

  @VisibleForTesting
  static String getRelativePath(String filePath, String prefixPath) {
    Path fileAbsolutePath = Paths.get(filePath).toAbsolutePath();
    Path prefixAbsolutePath = Paths.get(prefixPath).toAbsolutePath();
    return prefixAbsolutePath.relativize(fileAbsolutePath).toString();
  }

  public static List<ManifestFile> manifestFilesFromGitFetchFilesResult(
      GitFetchFilesResult gitFetchFilesResult, String prefixPath) {
    List<ManifestFile> manifestFiles = new ArrayList<>();

    if (isNotEmpty(gitFetchFilesResult.getFiles())) {
      List<GitFile> files = gitFetchFilesResult.getFiles();

      for (GitFile gitFile : files) {
        String filePath = getRelativePath(gitFile.getFilePath(), prefixPath);
        manifestFiles.add(ManifestFile.builder().fileName(filePath).fileContent(gitFile.getFileContent()).build());
      }
    }

    return manifestFiles;
  }

  public K8sTaskExecutionResponse getK8sTaskExecutionResponse(
      K8sTaskResponse k8sTaskResponse, CommandExecutionStatus commandExecutionStatus) {
    return K8sTaskExecutionResponse.builder()
        .k8sTaskResponse(k8sTaskResponse)
        .commandExecutionStatus(commandExecutionStatus)
        .build();
  }

  public ExecutionLogCallback getExecutionLogCallback(K8sTaskParameters request, String commandUnit) {
    return new ExecutionLogCallback(
        delegateLogService, request.getAccountId(), request.getAppId(), request.getActivityId(), commandUnit);
  }

  public List<K8sPod> getPodDetailsWithTrack(KubernetesConfig kubernetesConfig, String namespace, String releaseName,
      String track, long timeoutInMillis) throws Exception {
    Map<String, String> labels = ImmutableMap.of(HarnessLabels.releaseName, releaseName, HarnessLabels.track, track);
    return getPodDetailsWithLabels(kubernetesConfig, namespace, releaseName, labels, timeoutInMillis);
  }

  public List<K8sPod> getPodDetailsWithColor(KubernetesConfig kubernetesConfig, String namespace, String releaseName,
      String color, long timeoutInMillis) throws Exception {
    Map<String, String> labels = ImmutableMap.of(HarnessLabels.releaseName, releaseName, HarnessLabels.color, color);
    return getPodDetailsWithLabels(kubernetesConfig, namespace, releaseName, labels, timeoutInMillis);
  }

  public List<K8sPod> getPodDetails(
      KubernetesConfig kubernetesConfig, String namespace, String releaseName, long timeoutInMillis) throws Exception {
    Map<String, String> labels = ImmutableMap.of(HarnessLabels.releaseName, releaseName);
    return getPodDetailsWithLabels(kubernetesConfig, namespace, releaseName, labels, timeoutInMillis);
  }

  @VisibleForTesting
  List<K8sPod> getHelmPodDetails(
      KubernetesConfig kubernetesConfig, String namespace, String releaseName, long timeoutInMillis) throws Exception {
    Map<String, String> labels = ImmutableMap.of(HELM_RELEASE_LABEL, releaseName);
    return getPodDetailsWithLabels(kubernetesConfig, namespace, releaseName, labels, timeoutInMillis);
  }

  public List<K8sPod> getPodDetailsWithLabels(KubernetesConfig kubernetesConfig, String namespace, String releaseName,
      Map<String, String> labels, long timeoutInMillis) throws Exception {
    return timeLimiter.callWithTimeout(() -> {
      return kubernetesContainerService.getRunningPodsWithLabels(kubernetesConfig, namespace, labels)
          .stream()
          .map(pod
              -> K8sPod.builder()
                     .uid(pod.getMetadata().getUid())
                     .name(pod.getMetadata().getName())
                     .namespace(pod.getMetadata().getNamespace())
                     .releaseName(releaseName)
                     .podIP(pod.getStatus().getPodIP())
                     .containerList(pod.getStatus()
                                        .getContainerStatuses()
                                        .stream()
                                        .map(container
                                            -> K8sContainer.builder()
                                                   .containerId(container.getContainerID())
                                                   .name(container.getName())
                                                   .image(container.getImage())
                                                   .build())
                                        .collect(toList()))
                     .labels(pod.getMetadata().getLabels())
                     .build())
          .collect(toList());
    }, timeoutInMillis, TimeUnit.MILLISECONDS, true);
  }

  public String getLoadBalancerEndpoint(
      KubernetesConfig kubernetesConfig, List<KubernetesResource> resources, long timeoutInMillis) {
    KubernetesResource loadBalancerResource = getFirstLoadBalancerService(resources);
    if (loadBalancerResource == null) {
      return null;
    }

    // NOTE(hindwani): We are not using timeOutInMillis for waiting because of the bug: CDP-13872
    Service service = waitForLoadBalancerService(kubernetesConfig, loadBalancerResource.getResourceId().getName(),
        loadBalancerResource.getResourceId().getNamespace(), 60);

    if (service == null) {
      logger.warn("Could not get the Service Status {} from cluster.", loadBalancerResource.getResourceId().getName());
      return null;
    }

    LoadBalancerIngress loadBalancerIngress = service.getStatus().getLoadBalancer().getIngress().get(0);
    String loadBalancerHost =
        isNotBlank(loadBalancerIngress.getHostname()) ? loadBalancerIngress.getHostname() : loadBalancerIngress.getIp();

    boolean port80Found = false;
    boolean port443Found = false;
    Integer firstPort = null;

    for (ServicePort servicePort : service.getSpec().getPorts()) {
      firstPort = servicePort.getPort();

      if (servicePort.getPort() == 80) {
        port80Found = true;
      }
      if (servicePort.getPort() == 443) {
        port443Found = true;
      }
    }

    if (port443Found) {
      return "https://" + loadBalancerHost + "/";
    } else if (port80Found) {
      return "http://" + loadBalancerHost + "/";
    } else if (firstPort != null) {
      return loadBalancerHost + ":" + firstPort;
    } else {
      return loadBalancerHost;
    }
  }

  private Service waitForLoadBalancerService(
      KubernetesConfig kubernetesConfig, String serviceName, String namespace, int timeoutInSeconds) {
    try {
      return timeLimiter.callWithTimeout(() -> {
        while (true) {
          Service service = kubernetesContainerService.getService(kubernetesConfig, serviceName, namespace);

          LoadBalancerStatus loadBalancerStatus = service.getStatus().getLoadBalancer();
          if (!loadBalancerStatus.getIngress().isEmpty()) {
            return service;
          }
          int sleepTimeInSeconds = 5;
          logger.info("waitForLoadBalancerService: LoadBalancer Service {} not ready. Sleeping for {} seconds",
              serviceName, sleepTimeInSeconds);
          sleep(ofSeconds(sleepTimeInSeconds));
        }
      }, timeoutInSeconds, TimeUnit.SECONDS, true);
    } catch (UncheckedTimeoutException e) {
      logger.error("Timed out waiting for LoadBalancer service. Moving on.", e);
    } catch (Exception e) {
      logger.error("Exception while trying to get LoadBalancer service", e);
    }
    return null;
  }

  private boolean downloadFilesFromChartRepo(K8sDelegateManifestConfig delegateManifestConfig,
      String destinationDirectory, ExecutionLogCallback executionLogCallback, long timeoutInMillis) {
    HelmChartConfigParams helmChartConfigParams = delegateManifestConfig.getHelmChartConfigParams();

    try {
      executionLogCallback.saveExecutionLog(color(format("%nFetching files from helm chart repo"), White, Bold));
      helmTaskHelper.printHelmChartInfoInExecutionLogs(helmChartConfigParams, executionLogCallback);

      helmTaskHelper.downloadChartFiles(helmChartConfigParams, destinationDirectory, timeoutInMillis);

      executionLogCallback.saveExecutionLog(color("Successfully fetched following files:", White, Bold));
      executionLogCallback.saveExecutionLog(getManifestFileNamesInLogFormat(destinationDirectory));
      executionLogCallback.saveExecutionLog("Done.", INFO, CommandExecutionStatus.SUCCESS);

      return true;
    } catch (Exception e) {
      executionLogCallback.saveExecutionLog(ExceptionUtils.getMessage(e), ERROR, CommandExecutionStatus.FAILURE);
      return false;
    }
  }

  public List<Subset> generateSubsetsForDestinationRule(List<String> subsetNames) {
    List<Subset> subsets = new ArrayList<>();

    for (String subsetName : subsetNames) {
      Subset subset = new Subset();
      subset.setName(subsetName);

      if (subsetName.equals(HarnessLabelValues.trackCanary)) {
        Map<String, String> labels = new HashMap<>();
        labels.put(HarnessLabels.track, HarnessLabelValues.trackCanary);
        subset.setLabels(labels);
      } else if (subsetName.equals(HarnessLabelValues.trackStable)) {
        Map<String, String> labels = new HashMap<>();
        labels.put(HarnessLabels.track, HarnessLabelValues.trackStable);
        subset.setLabels(labels);
      } else if (subsetName.equals(HarnessLabelValues.colorBlue)) {
        Map<String, String> labels = new HashMap<>();
        labels.put(HarnessLabels.color, HarnessLabelValues.colorBlue);
        subset.setLabels(labels);
      } else if (subsetName.equals(HarnessLabelValues.colorGreen)) {
        Map<String, String> labels = new HashMap<>();
        labels.put(HarnessLabels.color, HarnessLabelValues.colorGreen);
        subset.setLabels(labels);
      }

      subsets.add(subset);
    }

    return subsets;
  }

  private String getHostFromRoute(List<DestinationWeight> routes) {
    if (isEmpty(routes)) {
      throw new InvalidRequestException("No routes exist in VirtualService", USER);
    }

    if (null == routes.get(0).getDestination()) {
      throw new InvalidRequestException("No destination exist in VirtualService", USER);
    }

    if (isBlank(routes.get(0).getDestination().getHost())) {
      throw new InvalidRequestException("No host exist in VirtualService", USER);
    }

    return routes.get(0).getDestination().getHost();
  }

  private PortSelector getPortSelectorFromRoute(List<DestinationWeight> routes) {
    return routes.get(0).getDestination().getPort();
  }

  public void updateVirtualServiceWithDestinationWeights(List<IstioDestinationWeight> istioDestinationWeights,
      VirtualService virtualService, ExecutionLogCallback executionLogCallback) throws IOException {
    validateRoutesInVirtualService(virtualService);

    executionLogCallback.saveExecutionLog("\nUpdating VirtualService with destination weights");

    List<HTTPRoute> http = virtualService.getSpec().getHttp();
    if (isNotEmpty(http)) {
      String host = getHostFromRoute(http.get(0).getRoute());
      PortSelector portSelector = getPortSelectorFromRoute(http.get(0).getRoute());
      http.get(0).setRoute(generateDestinationWeights(istioDestinationWeights, host, portSelector));
    }
  }

  private List<DestinationWeight> generateDestinationWeights(
      List<IstioDestinationWeight> istioDestinationWeights, String host, PortSelector portSelector) throws IOException {
    List<DestinationWeight> destinationWeights = new ArrayList<>();

    for (IstioDestinationWeight istioDestinationWeight : istioDestinationWeights) {
      String destinationYaml = getDestinationYaml(istioDestinationWeight.getDestination(), host);
      Destination destination = new YamlUtils().read(destinationYaml, Destination.class);
      destination.setPort(portSelector);

      DestinationWeight destinationWeight = new DestinationWeight();
      destinationWeight.setWeight(Integer.parseInt(istioDestinationWeight.getWeight()));
      destinationWeight.setDestination(destination);

      destinationWeights.add(destinationWeight);
    }

    return destinationWeights;
  }

  private String getDestinationYaml(String destination, String host) {
    if (canaryDestinationExpression.equals(destination)) {
      return generateDestination(host, HarnessLabelValues.trackCanary);
    } else if (stableDestinationExpression.equals(destination)) {
      return generateDestination(host, HarnessLabelValues.trackStable);
    } else {
      return destination;
    }
  }

  private String generateDestination(String host, String subset) {
    return ISTIO_DESTINATION_TEMPLATE.replace("$ISTIO_DESTINATION_HOST_NAME", host)
        .replace("$ISTIO_DESTINATION_SUBSET_NAME", subset);
  }

  private void validateRoutesInVirtualService(VirtualService virtualService) {
    List<HTTPRoute> http = virtualService.getSpec().getHttp();
    List<TCPRoute> tcp = virtualService.getSpec().getTcp();
    List<TLSRoute> tls = virtualService.getSpec().getTls();

    if (isEmpty(http)) {
      throw new InvalidRequestException(
          "Http route is not present in VirtualService. Only Http routes are allowed", USER);
    }

    if (isNotEmpty(tcp) || isNotEmpty(tls)) {
      throw new InvalidRequestException("Only Http routes are allowed in VirtualService for Traffic split", USER);
    }

    if (http.size() > 1) {
      throw new InvalidRequestException("Only one route is allowed in VirtualService", USER);
    }
  }

  public DestinationRule updateDestinationRuleManifestFilesWithSubsets(List<KubernetesResource> resources,
      List<String> subsets, KubernetesConfig kubernetesConfig, ExecutionLogCallback executionLogCallback)
      throws IOException {
    List<KubernetesResource> destinationRuleResources =
        resources.stream()
            .filter(
                kubernetesResource -> kubernetesResource.getResourceId().getKind().equals(Kind.DestinationRule.name()))
            .filter(KubernetesResource::isManaged)
            .collect(toList());

    if (isEmpty(destinationRuleResources)) {
      return null;
    }

    if (destinationRuleResources.size() > 1) {
      String msg = "More than one DestinationRule found. Only one DestinationRule can be marked with annotation "
          + HarnessAnnotations.managed + ": true";
      executionLogCallback.saveExecutionLog(msg + "\n", ERROR, FAILURE);
      throw new InvalidRequestException(msg, USER);
    }

    KubernetesClient kubernetesClient = kubernetesHelperService.getKubernetesClient(kubernetesConfig);
    kubernetesClient.customResources(
        kubernetesContainerService.getCustomResourceDefinition(kubernetesClient, new DestinationRuleBuilder().build()),
        DestinationRule.class, KubernetesResourceList.class, DoneableDestinationRule.class);

    KubernetesResource kubernetesResource = destinationRuleResources.get(0);
    InputStream inputStream = IOUtils.toInputStream(kubernetesResource.getSpec(), UTF_8);
    DestinationRule destinationRule = (DestinationRule) kubernetesClient.load(inputStream).get().get(0);
    destinationRule.getSpec().setSubsets(generateSubsetsForDestinationRule(subsets));

    kubernetesResource.setSpec(KubernetesHelper.toYaml(destinationRule));

    return destinationRule;
  }

  public VirtualService updateVirtualServiceManifestFilesWithRoutesForCanary(List<KubernetesResource> resources,
      KubernetesConfig kubernetesConfig, ExecutionLogCallback executionLogCallback) throws IOException {
    List<IstioDestinationWeight> istioDestinationWeights = new ArrayList<>();
    istioDestinationWeights.add(
        IstioDestinationWeight.builder().destination(stableDestinationExpression).weight("100").build());
    istioDestinationWeights.add(
        IstioDestinationWeight.builder().destination(canaryDestinationExpression).weight("0").build());

    return updateVirtualServiceManifestFilesWithRoutes(
        resources, kubernetesConfig, istioDestinationWeights, executionLogCallback);
  }

  private VirtualService updateVirtualServiceManifestFilesWithRoutes(List<KubernetesResource> resources,
      KubernetesConfig kubernetesConfig, List<IstioDestinationWeight> istioDestinationWeights,
      ExecutionLogCallback executionLogCallback) throws IOException {
    List<KubernetesResource> virtualServiceResources =
        resources.stream()
            .filter(
                kubernetesResource -> kubernetesResource.getResourceId().getKind().equals(Kind.VirtualService.name()))
            .filter(KubernetesResource::isManaged)
            .collect(toList());

    if (isEmpty(virtualServiceResources)) {
      return null;
    }

    if (virtualServiceResources.size() > 1) {
      String msg = "\nMore than one VirtualService found. Only one VirtualService can be marked with annotation "
          + HarnessAnnotations.managed + ": true";
      executionLogCallback.saveExecutionLog(msg + "\n", ERROR, FAILURE);
      throw new InvalidRequestException(msg, USER);
    }

    KubernetesClient kubernetesClient = kubernetesHelperService.getKubernetesClient(kubernetesConfig);
    kubernetesClient.customResources(
        kubernetesContainerService.getCustomResourceDefinition(kubernetesClient, new VirtualServiceBuilder().build()),
        VirtualService.class, KubernetesResourceList.class, DoneableVirtualService.class);

    KubernetesResource kubernetesResource = virtualServiceResources.get(0);
    InputStream inputStream = IOUtils.toInputStream(kubernetesResource.getSpec(), UTF_8);
    VirtualService virtualService = (VirtualService) kubernetesClient.load(inputStream).get().get(0);
    updateVirtualServiceWithDestinationWeights(istioDestinationWeights, virtualService, executionLogCallback);

    kubernetesResource.setSpec(KubernetesHelper.toYaml(virtualService));

    return virtualService;
  }

  public void deleteSkippedManifestFiles(String manifestFilesDirectory, ExecutionLogCallback executionLogCallback)
      throws Exception {
    List<FileData> files;
    Path directory = Paths.get(manifestFilesDirectory);

    try {
      files = getFilesUnderPath(directory.toString());
    } catch (Exception ex) {
      logger.info(ExceptionUtils.getMessage(ex));
      throw new WingsException("Failed to get files. Error: " + ExceptionUtils.getMessage(ex));
    }

    List<String> skippedFilesList = new ArrayList<>();

    for (FileData fileData : files) {
      try {
        String fileContent = new String(fileData.getFileBytes(), StandardCharsets.UTF_8);

        if (isNotBlank(fileContent)
            && fileContent.split("\\r?\\n")[0].contains(SKIP_FILE_FOR_DEPLOY_PLACEHOLDER_TEXT)) {
          skippedFilesList.add(fileData.getFilePath());
        }
      } catch (Exception ex) {
        logger.info("Could not convert to string for file" + fileData.getFilePath(), ex);
      }
    }

    if (isNotEmpty(skippedFilesList)) {
      executionLogCallback.saveExecutionLog("Following manifest files are skipped for applying");
      for (String file : skippedFilesList) {
        executionLogCallback.saveExecutionLog(color(file, Yellow, Bold));

        String filePath = Paths.get(manifestFilesDirectory, file).toString();
        FileIo.deleteFileIfExists(filePath);
      }

      executionLogCallback.saveExecutionLog("\n");
    }
  }

  private List<ManifestFile> readFilesFromDirectory(
      String directory, List<String> filePaths, ExecutionLogCallback executionLogCallback) {
    List<ManifestFile> manifestFiles = new ArrayList<>();

    for (String filepath : filePaths) {
      if (isValidManifestFile(filepath)) {
        Path path = Paths.get(directory, filepath);
        byte[] fileBytes;

        try {
          fileBytes = Files.readAllBytes(path);
        } catch (Exception ex) {
          logger.info(ExceptionUtils.getMessage(ex));
          throw new InvalidRequestException(
              format("Failed to read file at path [%s].%nError: %s", filepath, ExceptionUtils.getMessage(ex)));
        }

        manifestFiles.add(ManifestFile.builder()
                              .fileName(filepath)
                              .fileContent(new String(fileBytes, StandardCharsets.UTF_8))
                              .build());
      } else {
        executionLogCallback.saveExecutionLog(
            color(format("Ignoring file [%s] with unsupported extension", filepath), Yellow, Bold));
      }
    }

    return manifestFiles;
  }

  private List<ManifestFile> renderTemplateForHelmChartFiles(K8sDelegateTaskParams k8sDelegateTaskParams,
      String manifestFilesDirectory, List<String> chartFiles, List<String> valuesFiles, String releaseName,
      String namespace, ExecutionLogCallback executionLogCallback, HelmVersion helmVersion, long timeoutInMillis)
      throws Exception {
    String valuesFileOptions = writeValuesToFile(manifestFilesDirectory, valuesFiles);
    String helmPath = k8sDelegateTaskParams.getHelmPath();
    logger.info("Values file options: " + valuesFileOptions);

    printHelmPath(executionLogCallback, helmPath);

    List<ManifestFile> result = new ArrayList<>();

    for (String chartFile : chartFiles) {
      if (isValidManifestFile(chartFile)) {
        try (LogOutputStream logErrorStream = getExecutionLogOutputStream(executionLogCallback, ERROR)) {
          String helmTemplateCommand = getHelmCommandForRender(
              helmPath, manifestFilesDirectory, releaseName, namespace, valuesFileOptions, chartFile, helmVersion);
          printHelmTemplateCommand(executionLogCallback, helmTemplateCommand);

          ProcessResult processResult =
              executeShellCommand(manifestFilesDirectory, helmTemplateCommand, logErrorStream, timeoutInMillis);
          if (processResult.getExitValue() != 0) {
            throw new WingsException(format("Failed to render chart file [%s]", chartFile));
          }

          result.add(ManifestFile.builder().fileName(chartFile).fileContent(processResult.outputUTF8()).build());
        }
      } else {
        executionLogCallback.saveExecutionLog(
            color(format("Ignoring file [%s] with unsupported extension", chartFile), Yellow, Bold));
      }
    }

    return result;
  }

  private void printHelmPath(ExecutionLogCallback executionLogCallback, final String helmPath) {
    executionLogCallback.saveExecutionLog(color("Rendering chart files using Helm", White, Bold));
    executionLogCallback.saveExecutionLog(color(format("Using helm binary %s", helmPath), White, Normal));
  }

  private void printHelmTemplateCommand(ExecutionLogCallback executionLogCallback, final String helmTemplateCommand) {
    executionLogCallback.saveExecutionLog(color("Running Helm command", White, Bold));
    executionLogCallback.saveExecutionLog(color(helmTemplateCommand, White, Normal));
  }

  @VisibleForTesting
  ProcessResult executeShellCommand(String commandDirectory, String command, LogOutputStream logErrorStream,
      long timeoutInMillis) throws IOException, InterruptedException, TimeoutException {
    ProcessExecutor processExecutor = new ProcessExecutor()
                                          .timeout(timeoutInMillis, TimeUnit.MILLISECONDS)
                                          .directory(new File(commandDirectory))
                                          .commandSplit(command)
                                          .readOutput(true)
                                          .redirectError(logErrorStream);

    return processExecutor.execute();
  }

  @VisibleForTesting
  String getHelmCommandForRender(String helmPath, String manifestFilesDirectory, String releaseName, String namespace,
      String valuesFileOptions, String chartFile, HelmVersion helmVersion) {
    String helmTemplateCommand = HelmCommandTemplateFactory.getHelmCommandTemplate(
        HelmCommandTemplateFactory.HelmCliCommandType.RENDER_SPECIFIC_CHART_FILE, helmVersion);
    return replacePlaceHoldersInHelmTemplateCommand(
        helmTemplateCommand, helmPath, manifestFilesDirectory, releaseName, namespace, chartFile, valuesFileOptions);
  }

  @VisibleForTesting
  String getHelmCommandForRender(String helmPath, String manifestFilesDirectory, String releaseName, String namespace,
      String valuesFileOptions, HelmVersion helmVersion) {
    String helmTemplateCommand = HelmCommandTemplateFactory.getHelmCommandTemplate(
        HelmCommandTemplateFactory.HelmCliCommandType.RENDER_CHART, helmVersion);
    return replacePlaceHoldersInHelmTemplateCommand(
        helmTemplateCommand, helmPath, manifestFilesDirectory, releaseName, namespace, EMPTY, valuesFileOptions);
  }

  private String replacePlaceHoldersInHelmTemplateCommand(String unrenderedCommand, String helmPath,
      String chartLocation, String releaseName, String namespace, String chartFile, String valueOverrides) {
    return unrenderedCommand.replace(HELM_PATH_PLACEHOLDER, helmPath)
        .replace("${CHART_LOCATION}", chartLocation)
        .replace("${CHART_FILE}", chartFile)
        .replace("${RELEASE_NAME}", releaseName)
        .replace("${NAMESPACE}", namespace)
        .replace("${OVERRIDE_VALUES}", valueOverrides);
  }

  public Integer getTargetInstancesForCanary(
      Integer percentInstancesInDelegateRequest, Integer maxInstances, ExecutionLogCallback logCallback) {
    Integer targetInstances = (int) Math.round(percentInstancesInDelegateRequest * maxInstances / 100.0);
    if (targetInstances < 1) {
      logCallback.saveExecutionLog("\nTarget instances computed to be less than 1. Bumped up to 1");
      targetInstances = 1;
    }
    return targetInstances;
  }

  public List<KubernetesResourceId> fetchAllResourcesForRelease(K8sDeleteTaskParameters k8sDeleteTaskParameters,
      KubernetesConfig kubernetesConfig, ExecutionLogCallback executionLogCallback) throws IOException {
    String releaseName = k8sDeleteTaskParameters.getReleaseName();
    executionLogCallback.saveExecutionLog("Fetching all resources created for release: " + releaseName);

    ConfigMap configMap = kubernetesContainerService.getConfigMap(kubernetesConfig, releaseName);

    if (configMap == null || isEmpty(configMap.getData()) || isBlank(configMap.getData().get(ReleaseHistoryKeyName))) {
      executionLogCallback.saveExecutionLog("No resource history was available");
      return emptyList();
    }

    String releaseHistoryDataString = configMap.getData().get(ReleaseHistoryKeyName);
    ReleaseHistory releaseHistory = ReleaseHistory.createFromData(releaseHistoryDataString);

    if (isEmpty(releaseHistory.getReleases())) {
      return emptyList();
    }

    Map<String, KubernetesResourceId> kubernetesResourceIdMap = new HashMap<>();
    for (Release release : releaseHistory.getReleases()) {
      if (isNotEmpty(release.getResources())) {
        release.getResources().forEach(
            resource -> kubernetesResourceIdMap.put(generateResourceIdentifier(resource), resource));
      }
    }

    KubernetesResourceId harnessGeneratedCMResource = KubernetesResourceId.builder()
                                                          .kind(configMap.getKind())
                                                          .name(releaseName)
                                                          .namespace(kubernetesConfig.getNamespace())
                                                          .build();
    kubernetesResourceIdMap.put(generateResourceIdentifier(harnessGeneratedCMResource), harnessGeneratedCMResource);
    return new ArrayList<>(kubernetesResourceIdMap.values());
  }

  /**
   * This method arranges resources to be deleted in the reverse order of their creation.
   * To see order of create, please refer to KubernetesResourceComparer.kindOrder
   */
  public List<KubernetesResourceId> arrangeResourceIdsInDeletionOrder(List<KubernetesResourceId> resourceIdsToDelete) {
    List<KubernetesResource> kubernetesResources =
        resourceIdsToDelete.stream()
            .map(resourceId -> KubernetesResource.builder().resourceId(resourceId).build())
            .collect(Collectors.toList());
    kubernetesResources =
        kubernetesResources.stream().sorted(new KubernetesResourceComparer().reversed()).collect(Collectors.toList());
    return kubernetesResources.stream()
        .map(kubernetesResource -> kubernetesResource.getResourceId())
        .collect(Collectors.toList());
  }

  public List<KubernetesResourceId> getResourceIdsForDeletion(K8sDeleteTaskParameters k8sDeleteTaskParameters,
      KubernetesConfig kubernetesConfig, ExecutionLogCallback executionLogCallback) throws IOException {
    List<KubernetesResourceId> kubernetesResourceIds =
        fetchAllResourcesForRelease(k8sDeleteTaskParameters, kubernetesConfig, executionLogCallback);

    // If namespace deletion is NOT selected,remove all Namespace resources from deletion list
    if (!k8sDeleteTaskParameters.isDeleteNamespacesForRelease()) {
      kubernetesResourceIds =
          kubernetesResourceIds.stream()
              .filter(kubernetesResourceId -> !Namespace.name().equals(kubernetesResourceId.getKind()))
              .collect(toList());
    }

    return arrangeResourceIdsInDeletionOrder(kubernetesResourceIds);
  }

  String generateResourceIdentifier(KubernetesResourceId resourceId) {
    return new StringBuilder(128)
        .append(resourceId.getNamespace())
        .append('/')
        .append(resourceId.getKind())
        .append('/')
        .append(resourceId.getName())
        .toString();
  }

  public List<ContainerInfo> getContainerInfos(
      KubernetesConfig kubernetesConfig, String releaseName, String namespace, long timeoutInMillis) throws Exception {
    List<K8sPod> helmPods = getHelmPodDetails(kubernetesConfig, namespace, releaseName, timeoutInMillis);

    return helmPods.stream()
        .map(pod
            -> ContainerInfo.builder()
                   .hostName(pod.getName())
                   .ip(pod.getPodIP())
                   .containerId(getPodContainerId(pod))
                   .podName(pod.getName())
                   .newContainer(true)
                   .status(ContainerInfo.Status.SUCCESS)
                   .releaseName(releaseName)
                   .build())
        .collect(Collectors.toList());
  }

  private String getPodContainerId(K8sPod pod) {
    return isEmpty(pod.getContainerList()) ? EMPTY : pod.getContainerList().get(0).getContainerId();
  }

  private static final Set<String> openshiftResources = ImmutableSet.of("Route");

  public static long getTimeoutMillisFromMinutes(Integer timeoutMinutes) {
    if (timeoutMinutes == null || timeoutMinutes <= 0) {
      timeoutMinutes = DEFAULT_STEADY_STATE_TIMEOUT;
    }

    return ofMinutes(timeoutMinutes).toMillis();
  }
}
