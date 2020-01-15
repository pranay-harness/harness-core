package software.wings.delegatetasks.k8s;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.FAILURE;
import static io.harness.exception.WingsException.USER;
import static io.harness.filesystem.FileIo.createDirectoryIfDoesNotExist;
import static io.harness.filesystem.FileIo.getFilesUnderPath;
import static io.harness.govern.Switch.unhandled;
import static io.harness.k8s.kubectl.Utils.encloseWithQuotesIfNeeded;
import static io.harness.k8s.kubectl.Utils.parseLatestRevisionNumberFromRolloutHistory;
import static io.harness.k8s.manifest.ManifestHelper.getFirstLoadBalancerService;
import static io.harness.k8s.manifest.ManifestHelper.validateValuesFileContents;
import static io.harness.k8s.manifest.ManifestHelper.values_filename;
import static io.harness.k8s.manifest.ManifestHelper.yaml_file_extension;
import static io.harness.k8s.manifest.ManifestHelper.yml_file_extension;
import static io.harness.k8s.model.K8sExpressions.canaryDestinationExpression;
import static io.harness.k8s.model.K8sExpressions.stableDestinationExpression;
import static io.harness.k8s.model.Release.Status.Failed;
import static io.harness.threading.Morpheus.sleep;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.Duration.ofSeconds;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.beans.Log.LogColor.Gray;
import static software.wings.beans.Log.LogColor.White;
import static software.wings.beans.Log.LogColor.Yellow;
import static software.wings.beans.Log.LogLevel.ERROR;
import static software.wings.beans.Log.LogLevel.INFO;
import static software.wings.beans.Log.LogWeight.Bold;
import static software.wings.beans.Log.color;
import static software.wings.delegatetasks.k8s.taskhandler.K8sTrafficSplitTaskHandler.ISTIO_DESTINATION_TEMPLATE;
import static software.wings.sm.states.k8s.K8sApplyState.SKIP_FILE_FOR_DEPLOY_PLACEHOLDER_TEXT;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.common.util.concurrent.UncheckedTimeoutException;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.api.model.LoadBalancerIngress;
import io.fabric8.kubernetes.api.model.LoadBalancerStatus;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.harness.beans.FileData;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.KubernetesValuesException;
import io.harness.exception.KubernetesYamlException;
import io.harness.exception.WingsException;
import io.harness.filesystem.FileIo;
import io.harness.k8s.kubectl.AbstractExecutable;
import io.harness.k8s.kubectl.GetCommand;
import io.harness.k8s.kubectl.GetPodCommand;
import io.harness.k8s.kubectl.Kubectl;
import io.harness.k8s.kubectl.RolloutHistoryCommand;
import io.harness.k8s.kubectl.RolloutStatusCommand;
import io.harness.k8s.manifest.ManifestHelper;
import io.harness.k8s.model.HarnessAnnotations;
import io.harness.k8s.model.HarnessLabelValues;
import io.harness.k8s.model.HarnessLabels;
import io.harness.k8s.model.K8sContainer;
import io.harness.k8s.model.K8sPod;
import io.harness.k8s.model.Kind;
import io.harness.k8s.model.KubernetesResource;
import io.harness.k8s.model.KubernetesResourceComparer;
import io.harness.k8s.model.KubernetesResourceId;
import io.harness.k8s.model.Release;
import io.harness.k8s.model.ReleaseHistory;
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
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.StartedProcess;
import org.zeroturnaround.exec.stream.LogOutputStream;
import software.wings.beans.GitConfig;
import software.wings.beans.GitFileConfig;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.Log.LogLevel;
import software.wings.beans.appmanifest.ManifestFile;
import software.wings.beans.appmanifest.StoreType;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.beans.k8s.istio.IstioDestinationWeight;
import software.wings.beans.yaml.GitFetchFilesResult;
import software.wings.beans.yaml.GitFile;
import software.wings.cloudprovider.gke.KubernetesContainerService;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.delegatetasks.helm.HelmTaskHelper;
import software.wings.helpers.ext.helm.request.HelmChartConfigParams;
import software.wings.helpers.ext.k8s.request.K8sDelegateManifestConfig;
import software.wings.helpers.ext.k8s.request.K8sTaskParameters;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;
import software.wings.helpers.ext.k8s.response.K8sTaskResponse;
import software.wings.service.impl.KubernetesHelperService;
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
  @Inject private KubernetesHelperService kubernetesHelperService;

  private static String eventOutputFormat =
      "custom-columns=KIND:involvedObject.kind,NAME:.involvedObject.name,MESSAGE:.message,REASON:.reason";

  private static String eventWithNamespaceOutputFormat =
      "custom-columns=KIND:involvedObject.kind,NAME:.involvedObject.name,NAMESPACE:.involvedObject.namespace,MESSAGE:.message,REASON:.reason";

  public boolean dryRunManifests(Kubectl client, List<KubernetesResource> resources,
      K8sDelegateTaskParams k8sDelegateTaskParams, ExecutionLogCallback executionLogCallback) {
    try {
      executionLogCallback.saveExecutionLog(color("\nValidating manifests with Dry Run", White, Bold), INFO);

      FileIo.writeUtf8StringToFile(
          k8sDelegateTaskParams.getWorkingDirectory() + "/manifests-dry-run.yaml", ManifestHelper.toYaml(resources));

      ProcessResult result = executeCommand(client.apply().filename("manifests-dry-run.yaml").dryrun(true),
          k8sDelegateTaskParams.getWorkingDirectory(), executionLogCallback);
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

  public boolean applyManifests(Kubectl client, List<KubernetesResource> resources,
      K8sDelegateTaskParams k8sDelegateTaskParams, ExecutionLogCallback executionLogCallback) throws Exception {
    FileIo.writeUtf8StringToFile(
        k8sDelegateTaskParams.getWorkingDirectory() + "/manifests.yaml", ManifestHelper.toYaml(resources));

    ProcessResult result = executeCommand(client.apply().filename("manifests.yaml").record(true),
        k8sDelegateTaskParams.getWorkingDirectory(), executionLogCallback);
    if (result.getExitValue() != 0) {
      executionLogCallback.saveExecutionLog("\nFailed.", INFO, CommandExecutionStatus.FAILURE);
      return false;
    }

    executionLogCallback.saveExecutionLog("\nDone.", INFO, CommandExecutionStatus.SUCCESS);
    return true;
  }

  public boolean doStatusCheck(Kubectl client, KubernetesResourceId resourceId,
      K8sDelegateTaskParams k8sDelegateTaskParams, ExecutionLogCallback executionLogCallback) throws Exception {
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
      eventWatchProcess = getEventsCommand.executeInBackground(
          k8sDelegateTaskParams.getWorkingDirectory(), watchInfoStream, watchErrorStream);

      RolloutStatusCommand rolloutStatusCommand =
          client.rollout().status().resource(resourceId.kindNameRef()).namespace(resourceId.getNamespace()).watch(true);

      executionLogCallback.saveExecutionLog(
          RolloutStatusCommand.getPrintableCommand(rolloutStatusCommand.command()) + "\n");

      ProcessResult result = rolloutStatusCommand.execute(
          k8sDelegateTaskParams.getWorkingDirectory(), statusInfoStream, statusErrorStream, false);

      success = result.getExitValue() == 0;

      if (!success) {
        logger.warn(result.outputString());
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

  private boolean doStatusCheckForJob(Kubectl client, KubernetesResourceId resourceId,
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
      GetPodCommand getPodCommand =
          client.getPod().selector("job-name=" + resourceId.getName()).output("jsonpath='{.items[*].status.phase}'");

      executionLogCallback.saveExecutionLog(GetPodCommand.getPrintableCommand(getPodCommand.command()) + "\n");

      while (true) {
        ProcessResult result = getPodCommand.execute(
            k8sDelegateTaskParams.getWorkingDirectory(), statusInfoStream, statusErrorStream, false);

        boolean success = 0 == result.getExitValue();
        if (!success) {
          logger.warn(result.outputString());
          return false;
        }

        // cli command outputs with single quotes
        String jobStatus = result.outputString().replace("'", "");
        if ("Failed".equals(jobStatus) || "Unknown".equals(jobStatus)) {
          logger.warn(result.outputString());
          return false;
        }

        if ("Succeeded".equals(jobStatus)) {
          break;
        }

        sleep(ofSeconds(5));
      }

      return true;
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
      RolloutStatusCommand rolloutStatusCommand =
          client.rollout().status().resource(resourceId.kindNameRef()).namespace(resourceId.getNamespace()).watch(true);

      executionLogCallback.saveExecutionLog(
          RolloutStatusCommand.getPrintableCommand(rolloutStatusCommand.command()) + "\n");

      ProcessResult result = rolloutStatusCommand.execute(
          k8sDelegateTaskParams.getWorkingDirectory(), statusInfoStream, statusErrorStream, false);

      boolean success = 0 == result.getExitValue();
      if (!success) {
        logger.warn(result.outputString());
      }

      return success;
    }
  }

  public boolean doStatusCheckForAllResources(Kubectl client, List<KubernetesResourceId> resourceIds,
      K8sDelegateTaskParams k8sDelegateTaskParams, String namespace, ExecutionLogCallback executionLogCallback)
      throws Exception {
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
        eventWatchProcesses.add(getEventsCommand.executeInBackground(
            k8sDelegateTaskParams.getWorkingDirectory(), watchInfoStream, watchErrorStream));
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
        executionLogCallback.saveExecutionLog("\nDone.", INFO, CommandExecutionStatus.SUCCESS);
      } else {
        executionLogCallback.saveExecutionLog("\nFailed.", INFO, CommandExecutionStatus.FAILURE);
      }
    }
  }

  public boolean scale(Kubectl client, K8sDelegateTaskParams k8sDelegateTaskParams, KubernetesResourceId resourceId,
      int targetReplicaCount, ExecutionLogCallback executionLogCallback) throws Exception {
    executionLogCallback.saveExecutionLog("\nScaling " + resourceId.kindNameRef());

    ProcessResult result = executeCommand(client.scale()
                                              .resource(resourceId.kindNameRef())
                                              .replicas(targetReplicaCount)
                                              .namespace(resourceId.getNamespace()),
        k8sDelegateTaskParams.getWorkingDirectory(), executionLogCallback);
    if (result.getExitValue() == 0) {
      executionLogCallback.saveExecutionLog("\nDone.", INFO, CommandExecutionStatus.SUCCESS);
      return true;
    } else {
      executionLogCallback.saveExecutionLog("\nFailed.", INFO, CommandExecutionStatus.FAILURE);
      logger.warn("Failed to scale workload. Error {}", result.getOutput());
      return false;
    }
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
            ProcessResult result =
                executeCommand(client.delete().resources(resourceId.kindNameRef()).namespace(resourceId.getNamespace()),
                    k8sDelegateTaskParams.getWorkingDirectory(), executionLogCallback);
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
      ProcessResult result =
          executeCommand(client.delete().resources(resourceId.kindNameRef()).namespace(resourceId.getNamespace()),
              k8sDelegateTaskParams.getWorkingDirectory(), executionLogCallback);
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
    executeCommand(client.describe().filename("manifests.yaml"), k8sDelegateTaskParams.getWorkingDirectory(),
        executionLogCallback);
  }

  public String getLatestRevision(
      Kubectl client, KubernetesResourceId resourceId, K8sDelegateTaskParams k8sDelegateTaskParams) throws Exception {
    RolloutHistoryCommand rolloutHistoryCommand =
        client.rollout().history().resource(resourceId.kindNameRef()).namespace(resourceId.getNamespace());
    ProcessResult result = executeCommandSilent(rolloutHistoryCommand, k8sDelegateTaskParams.getWorkingDirectory());
    if (result.getExitValue() == 0) {
      return parseLatestRevisionNumberFromRolloutHistory(result.outputString());
    }
    return "";
  }

  public Integer getCurrentReplicas(
      Kubectl client, KubernetesResourceId resourceId, K8sDelegateTaskParams k8sDelegateTaskParams) throws Exception {
    GetCommand getCommand = client.get()
                                .resources(resourceId.kindNameRef())
                                .namespace(resourceId.getNamespace())
                                .output("jsonpath={$.spec.replicas}");
    ProcessResult result = executeCommandSilent(getCommand, k8sDelegateTaskParams.getWorkingDirectory());
    if (result.getExitValue() == 0) {
      return Integer.valueOf(result.outputString());
    } else {
      return null;
    }
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

  private List<ManifestFile> renderTemplateForHelm(K8sDelegateTaskParams k8sDelegateTaskParams,
      String manifestFilesDirectory, List<String> valuesFiles, String releaseName, String namespace,
      ExecutionLogCallback executionLogCallback) throws Exception {
    String valuesFileOptions = writeValuesToFile(manifestFilesDirectory, valuesFiles);
    logger.info("Values file options: " + valuesFileOptions);

    executionLogCallback.saveExecutionLog(color("Rendering chart using Helm", White, Bold));

    List<ManifestFile> result = new ArrayList<>();
    try (LogOutputStream logErrorStream = getExecutionLogOutputStream(executionLogCallback, ERROR)) {
      ProcessExecutor processExecutor =
          new ProcessExecutor()
              .timeout(10, TimeUnit.SECONDS)
              .directory(new File(manifestFilesDirectory))
              .commandSplit(encloseWithQuotesIfNeeded(k8sDelegateTaskParams.getHelmPath()) + " template "
                  + manifestFilesDirectory + " --name " + releaseName + " --namespace " + namespace + valuesFileOptions)
              .readOutput(true)
              .redirectError(logErrorStream);

      ProcessResult processResult = processExecutor.execute();
      if (processResult.getExitValue() != 0) {
        throw new WingsException(format("Failed to render helm chart. Error %s", processResult.getOutput().getUTF8()));
      }

      result.add(ManifestFile.builder().fileName("manifest.yaml").fileContent(processResult.outputString()).build());
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
      List<ManifestFile> manifestFiles, List<String> valuesFiles, ExecutionLogCallback executionLogCallback)
      throws Exception {
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
        ProcessExecutor processExecutor =
            new ProcessExecutor()
                .timeout(10, TimeUnit.SECONDS)
                .directory(new File(k8sDelegateTaskParams.getWorkingDirectory()))
                .commandSplit(encloseWithQuotesIfNeeded(k8sDelegateTaskParams.getGoTemplateClientPath())
                    + " -t template.yaml " + valuesFileOptions)
                .readOutput(true)
                .redirectError(logErrorStream);
        ProcessResult processResult = processExecutor.execute();

        if (processResult.getExitValue() != 0) {
          throw new WingsException(format("Failed to render template for %s. Error %s", manifestFile.getFileName(),
              processResult.getOutput().getUTF8()));
        }

        result.add(ManifestFile.builder()
                       .fileName(manifestFile.getFileName())
                       .fileContent(processResult.outputString())
                       .build());
      }
    }

    return result;
  }

  public List<ManifestFile> renderTemplate(K8sDelegateTaskParams k8sDelegateTaskParams,
      K8sDelegateManifestConfig k8sDelegateManifestConfig, String manifestFilesDirectory, List<String> valuesFiles,
      String releaseName, String namespace, ExecutionLogCallback executionLogCallback) throws Exception {
    StoreType storeType = k8sDelegateManifestConfig.getManifestStoreTypes();

    switch (storeType) {
      case Local:
      case Remote:
        List<ManifestFile> manifestFiles = readManifestFilesFromDirectory(manifestFilesDirectory);
        return renderManifestFilesForGoTemplate(
            k8sDelegateTaskParams, manifestFiles, valuesFiles, executionLogCallback);

      case HelmSourceRepo:
        return renderTemplateForHelm(
            k8sDelegateTaskParams, manifestFilesDirectory, valuesFiles, releaseName, namespace, executionLogCallback);

      case HelmChartRepo:
        manifestFilesDirectory =
            Paths.get(manifestFilesDirectory, k8sDelegateManifestConfig.getHelmChartConfigParams().getChartName())
                .toString();
        return renderTemplateForHelm(
            k8sDelegateTaskParams, manifestFilesDirectory, valuesFiles, releaseName, namespace, executionLogCallback);

      default:
        unhandled(storeType);
    }

    return new ArrayList<>();
  }

  public List<ManifestFile> renderTemplateForApply(K8sDelegateTaskParams k8sDelegateTaskParams,
      K8sDelegateManifestConfig k8sDelegateManifestConfig, String manifestFilesDirectory, List<String> filesToApply,
      List<String> valuesFiles, String releaseName, String namespace, ExecutionLogCallback executionLogCallback)
      throws Exception {
    StoreType storeType = k8sDelegateManifestConfig.getManifestStoreTypes();

    switch (storeType) {
      case Local:
      case Remote:
        List<ManifestFile> manifestFiles =
            readFilesFromDirectory(manifestFilesDirectory, filesToApply, executionLogCallback);
        return renderManifestFilesForGoTemplate(
            k8sDelegateTaskParams, manifestFiles, valuesFiles, executionLogCallback);

      case HelmSourceRepo:
        return renderTemplateForHelmChartFiles(k8sDelegateTaskParams, manifestFilesDirectory, filesToApply, valuesFiles,
            releaseName, namespace, executionLogCallback);

      case HelmChartRepo:
        manifestFilesDirectory =
            Paths.get(manifestFilesDirectory, k8sDelegateManifestConfig.getHelmChartConfigParams().getChartName())
                .toString();
        return renderTemplateForHelmChartFiles(k8sDelegateTaskParams, manifestFilesDirectory, filesToApply, valuesFiles,
            releaseName, namespace, executionLogCallback);

      default:
        unhandled(storeType);
    }

    return new ArrayList<>();
  }

  private static boolean isValidManifestFile(String filename) {
    return (StringUtils.endsWith(filename, yaml_file_extension) || StringUtils.endsWith(filename, yml_file_extension))
        && !StringUtils.equals(filename, values_filename);
  }

  public List<KubernetesResource> readManifests(
      List<ManifestFile> manifestFiles, ExecutionLogCallback executionLogCallback) {
    List<KubernetesResource> result = new ArrayList<>();

    for (ManifestFile manifestFile : manifestFiles) {
      if (isValidManifestFile(manifestFile.getFileName())) {
        try {
          result.addAll(ManifestHelper.processYaml(manifestFile.getFileContent()));
        } catch (KubernetesYamlException e) {
          executionLogCallback.saveExecutionLog("Exception while processing " + manifestFile.getFileName(), ERROR);
          String message = ExceptionUtils.getMessage(e);
          if (e.getCause() != null) {
            message += e.getCause().getMessage();
          }
          executionLogCallback.saveExecutionLog(message, ERROR);
          throw e;
        } catch (Exception e) {
          executionLogCallback.saveExecutionLog("Exception while processing " + manifestFile.getFileName(), ERROR);
          executionLogCallback.saveExecutionLog(ExceptionUtils.getMessage(e), ERROR);
          throw e;
        }
      }
    }

    return result.stream().sorted(new KubernetesResourceComparer()).collect(toList());
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

  private String getManifestFileNamesInLogFormat(String manifestFilesDirectory) throws Exception {
    StringBuilder sb = new StringBuilder(1024);

    Path basePath = Paths.get(manifestFilesDirectory);
    try (Stream<Path> paths = Files.walk(basePath)) {
      paths.filter(Files::isRegularFile)
          .forEach(each
              -> sb.append(color(format("- %s", getRelativePath(each.toString(), basePath.toString())), Gray))
                     .append(System.lineSeparator()));
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
      executionLogCallback.saveExecutionLog(
          "Failed to download manifest files from git. " + ExceptionUtils.getMessage(e), ERROR,
          CommandExecutionStatus.FAILURE);
      return false;
    }
  }

  public boolean fetchManifestFilesAndWriteToDirectory(K8sDelegateManifestConfig delegateManifestConfig,
      String manifestFilesDirectory, ExecutionLogCallback executionLogCallback) {
    StoreType storeType = delegateManifestConfig.getManifestStoreTypes();
    switch (storeType) {
      case Local:
        return writeManifestFilesToDirectory(
            delegateManifestConfig.getManifestFiles(), manifestFilesDirectory, executionLogCallback);

      case Remote:
      case HelmSourceRepo:
        return downloadManifestFilesFromGit(delegateManifestConfig, manifestFilesDirectory, executionLogCallback);

      case HelmChartRepo:
        return downloadFilesFromChartRepo(delegateManifestConfig, manifestFilesDirectory, executionLogCallback);

      default:
        unhandled(storeType);
    }

    return false;
  }

  private static String getRelativePath(String filePath, String prefixPath) {
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

  public List<K8sPod> getPodDetailsWithTrack(
      KubernetesConfig kubernetesConfig, String namespace, String releaseName, String track) throws Exception {
    Map<String, String> labels = ImmutableMap.of(HarnessLabels.releaseName, releaseName, HarnessLabels.track, track);
    return getPodDetailsWithLabels(kubernetesConfig, namespace, releaseName, labels);
  }

  public List<K8sPod> getPodDetailsWithColor(
      KubernetesConfig kubernetesConfig, String namespace, String releaseName, String color) throws Exception {
    Map<String, String> labels = ImmutableMap.of(HarnessLabels.releaseName, releaseName, HarnessLabels.color, color);
    return getPodDetailsWithLabels(kubernetesConfig, namespace, releaseName, labels);
  }

  public List<K8sPod> getPodDetails(KubernetesConfig kubernetesConfig, String namespace, String releaseName)
      throws Exception {
    Map<String, String> labels = ImmutableMap.of(HarnessLabels.releaseName, releaseName);
    return getPodDetailsWithLabels(kubernetesConfig, namespace, releaseName, labels);
  }

  public List<K8sPod> getPodDetailsWithLabels(KubernetesConfig kubernetesConfig, String namespace, String releaseName,
      Map<String, String> labels) throws Exception {
    return timeLimiter.callWithTimeout(() -> {
      return kubernetesContainerService.getRunningPodsWithLabels(kubernetesConfig, emptyList(), namespace, labels)
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
                     .build())
          .collect(toList());
    }, 10, TimeUnit.SECONDS, true);
  }

  public String getLoadBalancerEndpoint(KubernetesConfig kubernetesConfig, List<KubernetesResource> resources) {
    KubernetesResource loadBalancerResource = getFirstLoadBalancerService(resources);
    if (loadBalancerResource == null) {
      return null;
    }

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
          Service service =
              kubernetesContainerService.getService(kubernetesConfig, emptyList(), serviceName, namespace);

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
      String destinationDirectory, ExecutionLogCallback executionLogCallback) {
    HelmChartConfigParams helmChartConfigParams = delegateManifestConfig.getHelmChartConfigParams();

    try {
      executionLogCallback.saveExecutionLog(color(format("%nFetching files from helm chart repo"), White, Bold));
      helmTaskHelper.printHelmChartInfoInExecutionLogs(helmChartConfigParams, executionLogCallback);

      helmTaskHelper.downloadChartFiles(helmChartConfigParams, destinationDirectory);

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
      throw new WingsException("No routes exist in VirtualService", USER);
    }

    if (null == routes.get(0).getDestination()) {
      throw new WingsException("No destination exist in VirtualService", USER);
    }

    if (isBlank(routes.get(0).getDestination().getHost())) {
      throw new WingsException("No host exist in VirtualService", USER);
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
      throw new WingsException("Http route is not present in VirtualService. Only Http routes are allowed", USER);
    }

    if (isNotEmpty(tcp) || isNotEmpty(tls)) {
      throw new WingsException("Only Http routes are allowed in VirtualService for Traffic split", USER);
    }

    if (http.size() > 1) {
      throw new WingsException("Only one route is allowed in VirtualService", USER);
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
      throw new WingsException(msg, USER);
    }

    KubernetesClient kubernetesClient = kubernetesHelperService.getKubernetesClient(kubernetesConfig, emptyList());
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
      throw new WingsException(msg, USER);
    }

    KubernetesClient kubernetesClient = kubernetesHelperService.getKubernetesClient(kubernetesConfig, emptyList());
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
      String namespace, ExecutionLogCallback executionLogCallback) throws Exception {
    String valuesFileOptions = writeValuesToFile(manifestFilesDirectory, valuesFiles);
    logger.info("Values file options: " + valuesFileOptions);

    executionLogCallback.saveExecutionLog(color("Rendering chart files using Helm", White, Bold));

    List<ManifestFile> result = new ArrayList<>();

    for (String chartFile : chartFiles) {
      if (isValidManifestFile(chartFile)) {
        try (LogOutputStream logErrorStream = getExecutionLogOutputStream(executionLogCallback, ERROR)) {
          ProcessExecutor processExecutor =
              new ProcessExecutor()
                  .timeout(10, TimeUnit.SECONDS)
                  .directory(new File(manifestFilesDirectory))
                  .commandSplit(encloseWithQuotesIfNeeded(k8sDelegateTaskParams.getHelmPath()) + " template "
                      + manifestFilesDirectory + " -x " + chartFile + " --name " + releaseName + " --namespace "
                      + namespace + valuesFileOptions)
                  .readOutput(true)
                  .redirectError(logErrorStream);

          ProcessResult processResult = processExecutor.execute();
          if (processResult.getExitValue() != 0) {
            throw new WingsException(format("Failed to render chart file [%s]", chartFile));
          }

          result.add(ManifestFile.builder().fileName(chartFile).fileContent(processResult.outputString()).build());
        }
      } else {
        executionLogCallback.saveExecutionLog(
            color(format("Ignoring file [%s] with unsupported extension", chartFile), Yellow, Bold));
      }
    }

    return result;
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
}
