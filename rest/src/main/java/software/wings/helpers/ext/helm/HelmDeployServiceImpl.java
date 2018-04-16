package software.wings.helpers.ext.helm;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.common.util.concurrent.UncheckedTimeoutException;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.ReplicationControllerStatus;
import io.fabric8.kubernetes.api.model.extensions.DaemonSet;
import io.fabric8.kubernetes.api.model.extensions.DaemonSetStatus;
import io.fabric8.kubernetes.api.model.extensions.Deployment;
import io.fabric8.kubernetes.api.model.extensions.DeploymentStatus;
import io.fabric8.kubernetes.api.model.extensions.ReplicaSet;
import io.fabric8.kubernetes.api.model.extensions.ReplicaSetStatus;
import io.fabric8.kubernetes.api.model.extensions.StatefulSet;
import io.fabric8.kubernetes.api.model.extensions.StatefulSetStatus;
import io.harness.data.structure.EmptyPredicate;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.ErrorCode;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.Log.LogLevel;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.cloudprovider.ContainerInfo;
import software.wings.cloudprovider.gke.GkeClusterService;
import software.wings.cloudprovider.gke.KubernetesContainerService;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.helm.HelmClientImpl.HelmCliResponse;
import software.wings.helpers.ext.helm.request.HelmCommandRequest;
import software.wings.helpers.ext.helm.request.HelmCommandRequest.HelmCommandType;
import software.wings.helpers.ext.helm.request.HelmInstallCommandRequest;
import software.wings.helpers.ext.helm.request.HelmReleaseHistoryCommandRequest;
import software.wings.helpers.ext.helm.request.HelmRollbackCommandRequest;
import software.wings.helpers.ext.helm.response.HelmCommandResponse;
import software.wings.helpers.ext.helm.response.HelmInstallCommandResponse;
import software.wings.helpers.ext.helm.response.HelmListReleasesCommandResponse;
import software.wings.helpers.ext.helm.response.HelmReleaseHistoryCommandResponse;
import software.wings.helpers.ext.helm.response.ReleaseInfo;
import software.wings.service.impl.ContainerServiceParams;
import software.wings.service.impl.KubernetesHelperService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * Created by anubhaw on 4/1/18.
 */
@Singleton
public class HelmDeployServiceImpl implements HelmDeployService {
  @Inject private HelmClient helmClient;
  @Inject private KubernetesHelperService kubernetesHelperService;
  @Inject private KubernetesContainerService kubernetesContainerService;
  @Inject private GkeClusterService gkeClusterService;
  @Inject private TimeLimiter timeLimiter;

  private static final Logger logger = LoggerFactory.getLogger(HelmDeployService.class);

  @Override
  public HelmCommandResponse deploy(
      HelmInstallCommandRequest commandRequest, ExecutionLogCallback executionLogCallback) {
    try {
      HelmInstallCommandResponse commandResponse;
      executionLogCallback.saveExecutionLog(
          "List all existing deployed releases for release name: " + commandRequest.getReleaseName());
      HelmCliResponse helmCliResponse =
          helmClient.releaseHistory(commandRequest.getKubeConfigLocation(), commandRequest.getReleaseName());
      executionLogCallback.saveExecutionLog(helmCliResponse.getOutput());

      if (helmCliResponse.getCommandExecutionStatus().equals(CommandExecutionStatus.FAILURE)) {
        executionLogCallback.saveExecutionLog("No previous deployment found for release. Installing chart");
        commandResponse = (HelmInstallCommandResponse) helmClient.install(commandRequest);
      } else {
        executionLogCallback.saveExecutionLog("Previous release exists for chart. Upgrading chart");
        commandResponse = (HelmInstallCommandResponse) helmClient.upgrade(commandRequest);
      }
      executionLogCallback.saveExecutionLog(commandResponse.getOutput());
      List<ContainerInfo> containerInfos = new ArrayList<>();
      timeLimiter.callWithTimeout(
          ()
              -> containerInfos.addAll(fetchContainerInfo(commandRequest, executionLogCallback)),
          commandRequest.getTimeoutInMillis(), TimeUnit.MILLISECONDS, true);
      commandResponse.setContainerInfoList(containerInfos);
      return commandResponse;
    } catch (UncheckedTimeoutException e) {
      String msg = "Timed out waiting for controller to reach in steady state";
      logger.error(msg, e);
      executionLogCallback.saveExecutionLog(
          "Timed out waiting for controller to reach in steady state", LogLevel.ERROR);
      return new HelmCommandResponse(CommandExecutionStatus.FAILURE, e.getMessage());
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Exception in deploying helm chart [{}]", commandRequest, e);
      return new HelmCommandResponse(CommandExecutionStatus.FAILURE, e.getMessage());
    }
  }

  private List<ContainerInfo> fetchContainerInfo(
      HelmCommandRequest commandRequest, ExecutionLogCallback executionLogCallback) {
    ContainerServiceParams containerServiceParams = commandRequest.getContainerServiceParams();

    KubernetesConfig kubernetesConfig = gkeClusterService.getCluster(containerServiceParams.getSettingAttribute(),
        containerServiceParams.getEncryptionDetails(), containerServiceParams.getClusterName(),
        containerServiceParams.getNamespace());

    List<? extends HasMetadata> controllers = kubernetesContainerService.getControllers(kubernetesConfig,
        containerServiceParams.getEncryptionDetails(), ImmutableMap.of("release", commandRequest.getReleaseName()));

    List<KubeControllerStatus> controllerStatuses = controllers.stream()
                                                        .map(this ::getControllerStatus)
                                                        .filter(this ::steadyStateCheckRequired)
                                                        .collect(Collectors.toList());

    executionLogCallback.saveExecutionLog(String.format("Deployed Controllers [%s]:", controllerStatuses.size()));
    controllerStatuses.forEach(kubeControllerStatus
        -> executionLogCallback.saveExecutionLog(String.format("Kind:%s, Name:%s (desired: %s)",
            kubeControllerStatus.getKind(), kubeControllerStatus.getName(), kubeControllerStatus.getDesiredCount())));

    List<ContainerInfo> containerInfoList = new ArrayList<>();
    if (controllerStatuses.size() > 0) {
      containerInfoList =
          controllerStatuses.stream()
              .flatMap(controllerStatus
                  -> kubernetesContainerService
                         .getContainerInfosWhenReady(kubernetesConfig, containerServiceParams.getEncryptionDetails(),
                             controllerStatus.getName(), 0, controllerStatus.getDesiredCount(),
                             (int) TimeUnit.MINUTES.toMinutes(30), new ArrayList<>(),
                             controllerStatus.getKind().equals("DaemonSet"), executionLogCallback, true, 0)
                         .stream())
              .collect(Collectors.toList());
    }
    return containerInfoList;
  }

  private boolean steadyStateCheckRequired(KubeControllerStatus controllerStatus) {
    boolean noSteadyCheckRequire = controllerStatus.getDesiredCount() == 0 && controllerStatus.getRunningCount() == 0;
    if (noSteadyCheckRequire) {
      logger.info("Controller doesn't need steady state check. [{}]", controllerStatus);
    }
    return !noSteadyCheckRequire;
  }

  private KubeControllerStatus getControllerStatus(HasMetadata hasMetadata) {
    if ("Deployment".equals(hasMetadata.getKind())) {
      DeploymentStatus status = ((Deployment) hasMetadata).getStatus();
      return KubeControllerStatus.builder()
          .name(hasMetadata.getMetadata().getName())
          .kind(hasMetadata.getKind())
          .runningCount(status.getReadyReplicas() == null ? 0 : status.getReadyReplicas())
          .desiredCount(status.getReplicas())
          .build();
    } else if ("StatefulSet".equals(hasMetadata.getKind())) {
      StatefulSetStatus status = ((StatefulSet) hasMetadata).getStatus();
      return KubeControllerStatus.builder()
          .name(hasMetadata.getMetadata().getName())
          .kind(hasMetadata.getKind())
          .runningCount(status.getReadyReplicas() == null ? 0 : status.getReadyReplicas())
          .desiredCount(status.getReplicas())
          .build();
    } else if ("ReplicaSet".equals(hasMetadata.getKind())) {
      ReplicaSetStatus status = ((ReplicaSet) hasMetadata).getStatus();
      return KubeControllerStatus.builder()
          .name(hasMetadata.getMetadata().getName())
          .kind(hasMetadata.getKind())
          .runningCount(status.getReadyReplicas() == null ? 0 : status.getReadyReplicas())
          .desiredCount(status.getReplicas())
          .build();
    } else if ("ReplicationController".equals(hasMetadata.getKind())) {
      ReplicationControllerStatus status = ((ReplicationController) hasMetadata).getStatus();
      return KubeControllerStatus.builder()
          .name(hasMetadata.getMetadata().getName())
          .kind(hasMetadata.getKind())
          .runningCount(status.getReadyReplicas() == null ? 0 : status.getReadyReplicas())
          .desiredCount(status.getReplicas())
          .build();
    } else if ("DaemonSet".equals(hasMetadata.getKind())) {
      DaemonSetStatus status = ((DaemonSet) hasMetadata).getStatus();
      return KubeControllerStatus.builder()
          .name(hasMetadata.getMetadata().getName())
          .kind(hasMetadata.getKind())
          .runningCount(status.getNumberReady() == null ? 0 : status.getNumberReady())
          .desiredCount(status.getDesiredNumberScheduled())
          .build();
    } else {
      throw new WingsException(ErrorCode.INVALID_REQUEST)
          .addParam("message", "Unhandled resource type" + hasMetadata.getKind());
    }
  }

  @Override
  public HelmCommandResponse rollback(
      HelmRollbackCommandRequest commandRequest, ExecutionLogCallback executionLogCallback) {
    try {
      HelmInstallCommandResponse commandResponse = helmClient.rollback(commandRequest);
      executionLogCallback.saveExecutionLog(commandResponse.getOutput());
      List<ContainerInfo> containerInfos = new ArrayList<>();
      timeLimiter.callWithTimeout(
          ()
              -> containerInfos.addAll(fetchContainerInfo(commandRequest, executionLogCallback)),
          commandRequest.getTimeoutInMillis(), TimeUnit.MILLISECONDS, true);
      commandResponse.setContainerInfoList(containerInfos);
      return commandResponse;
    } catch (UncheckedTimeoutException e) {
      String msg = "Timed out waiting for controller to reach in steady state";
      logger.error(msg, e);
      executionLogCallback.saveExecutionLog(
          "Timed out waiting for controller to reach in steady state", LogLevel.ERROR);
      return new HelmCommandResponse(CommandExecutionStatus.FAILURE, e.getMessage());
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Helm chart rollback failed [{}]", commandRequest, e);
      return new HelmCommandResponse(CommandExecutionStatus.FAILURE, e.getMessage());
    }
  }

  @Override
  public HelmCommandResponse ensureHelmCliAndTillerInstalled(HelmCommandRequest helmCommandRequest)
      throws InterruptedException, IOException, TimeoutException {
    HelmCliResponse cliResponse = helmClient.getClientAndServerVersion(helmCommandRequest);
    if (cliResponse.getCommandExecutionStatus().equals(CommandExecutionStatus.FAILURE)) {
      throw new WingsException(ErrorCode.INVALID_REQUEST)
          .addParam("message", "Helm client not installed or not initialized");
    }
    return new HelmCommandResponse(cliResponse.getCommandExecutionStatus(), cliResponse.getOutput());
  }

  @Override
  public HelmListReleasesCommandResponse listReleases(HelmInstallCommandRequest helmCommandRequest) {
    try {
      HelmCliResponse helmCliResponse = helmClient.listReleases(helmCommandRequest);
      List<ReleaseInfo> releaseInfoList =
          parseHelmReleaseCommandOutput(helmCliResponse.getOutput(), HelmCommandType.LIST_RELEASE);
      return HelmListReleasesCommandResponse.builder()
          .commandExecutionStatus(helmCliResponse.getCommandExecutionStatus())
          .output(helmCliResponse.getOutput())
          .releaseInfoList(releaseInfoList)
          .build();
    } catch (Exception e) {
      logger.error("Helm list releases failed", e);
      return HelmListReleasesCommandResponse.builder()
          .commandExecutionStatus(CommandExecutionStatus.FAILURE)
          .output(e.getMessage())
          .build();
    }
  }

  @Override
  public HelmReleaseHistoryCommandResponse releaseHistory(HelmReleaseHistoryCommandRequest helmCommandRequest) {
    try {
      HelmCliResponse helmCliResponse =
          helmClient.releaseHistory(helmCommandRequest.getKubeConfigLocation(), helmCommandRequest.getReleaseName());
      List<ReleaseInfo> releaseInfoList =
          parseHelmReleaseCommandOutput(helmCliResponse.getOutput(), helmCommandRequest.getHelmCommandType());
      return HelmReleaseHistoryCommandResponse.builder()
          .commandExecutionStatus(helmCliResponse.getCommandExecutionStatus())
          .output(helmCliResponse.getOutput())
          .releaseInfoList(releaseInfoList)
          .build();
    } catch (Exception e) {
      logger.error("Helm list releases failed", e);
      return HelmReleaseHistoryCommandResponse.builder()
          .commandExecutionStatus(CommandExecutionStatus.FAILURE)
          .output(e.getMessage())
          .build();
    }
  }

  private List<ReleaseInfo> parseHelmReleaseCommandOutput(String listReleaseOutput, HelmCommandType helmCommandType)
      throws IOException {
    if (EmptyPredicate.isEmpty(listReleaseOutput)) {
      return new ArrayList<>();
    }
    CSVFormat csvFormat = CSVFormat.RFC4180.withFirstRecordAsHeader().withDelimiter('\t').withTrim();
    return CSVParser.parse(listReleaseOutput, csvFormat)
        .getRecords()
        .stream()
        .map(helmCommandType.equals(HelmCommandType.RELEASE_HISTORY) ? this ::releaseHistoryCsvRecordToReleaseInfo
                                                                     : this ::listReleaseCsvRecordToReleaseInfo)
        .collect(Collectors.toList());
  }

  private ReleaseInfo listReleaseCsvRecordToReleaseInfo(CSVRecord releaseRecord) {
    return ReleaseInfo.builder()
        .name(releaseRecord.get("NAME"))
        .revision(releaseRecord.get("REVISION"))
        .status(releaseRecord.get("STATUS"))
        .chart(releaseRecord.get("CHART"))
        .namespace(releaseRecord.get("NAMESPACE"))
        .build();
  }

  private ReleaseInfo releaseHistoryCsvRecordToReleaseInfo(CSVRecord releaseRecord) {
    return ReleaseInfo.builder()
        .revision(releaseRecord.get("REVISION"))
        .status(releaseRecord.get("STATUS"))
        .chart(releaseRecord.get("CHART"))
        .build();
  }

  @Data
  @Builder
  public static class KubeControllerStatus {
    private String name;
    private String kind;
    private int runningCount;
    private int desiredCount;
  }
}
