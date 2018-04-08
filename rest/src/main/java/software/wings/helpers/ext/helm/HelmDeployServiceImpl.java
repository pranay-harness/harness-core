package software.wings.helpers.ext.helm;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.extensions.Deployment;
import io.harness.data.structure.EmptyPredicate;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.cloudprovider.ContainerInfo;
import software.wings.cloudprovider.gke.GkeClusterService;
import software.wings.cloudprovider.gke.KubernetesContainerService;
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

      List<ContainerInfo> containerInfos = fetchContainerInfo(commandRequest, executionLogCallback);
      commandResponse.setContainerInfoList(containerInfos);
      return commandResponse;
    } catch (Exception e) {
      logger.error("Exception in deploying helm chart [{}]", commandRequest, e);
      return new HelmCommandResponse(CommandExecutionStatus.FAILURE, e.getMessage());
    }
  }

  private List<ContainerInfo> fetchContainerInfo(
      HelmInstallCommandRequest commandRequest, ExecutionLogCallback executionLogCallback) {
    ContainerServiceParams containerServiceParams = commandRequest.getContainerServiceParams();

    KubernetesConfig kubernetesConfig = gkeClusterService.getCluster(containerServiceParams.getSettingAttribute(),
        containerServiceParams.getEncryptionDetails(), containerServiceParams.getClusterName(),
        containerServiceParams.getNamespace());

    List<? extends HasMetadata> controllers = kubernetesContainerService.getControllers(kubernetesConfig,
        containerServiceParams.getEncryptionDetails(), ImmutableMap.of("release", commandRequest.getReleaseName()));

    List<Deployment> deployments =
        kubernetesHelperService.getKubernetesClient(kubernetesConfig, containerServiceParams.getEncryptionDetails())
            .extensions()
            .deployments()
            .inNamespace(kubernetesConfig.getNamespace())
            .withLabel("release", commandRequest.getReleaseName())
            .list()
            .getItems();

    List<ContainerInfo> containerInfosWhenReady = new ArrayList<>();

    deployments.forEach(deployment -> {
      List<ContainerInfo> containerInfos = kubernetesContainerService.getContainerInfosWhenReady(kubernetesConfig,
          containerServiceParams.getEncryptionDetails(), deployment.getMetadata().getName(), 0,
          deployment.getSpec().getReplicas(), (int) TimeUnit.MINUTES.toMinutes(30), new ArrayList<>(), false,
          executionLogCallback, true, 0);

      containerInfosWhenReady.addAll(containerInfos);
    });
    return containerInfosWhenReady;
  }

  @Override
  public HelmCommandResponse rollback(
      HelmRollbackCommandRequest commandRequest, ExecutionLogCallback executionLogCallback) {
    try {
      HelmCommandResponse commandResponse = helmClient.rollback(commandRequest);
      executionLogCallback.saveExecutionLog(commandResponse.getOutput());
      return commandResponse;
    } catch (Exception e) {
      logger.error("Helm chart rollback failed [{}]", commandRequest, e);
      return new HelmCommandResponse(CommandExecutionStatus.FAILURE, e.getMessage());
    }
  }

  @Override
  public HelmCommandResponse ensureHelmCliAndTillerInstalled(
      HelmCommandRequest helmCommandRequest, ExecutionLogCallback executionLogCallback) {
    try {
      HelmCliResponse cliResponse = helmClient.getClientAndServerVersion(helmCommandRequest);
      return new HelmCommandResponse(cliResponse.getCommandExecutionStatus(), cliResponse.getOutput());
    } catch (Exception e) {
      logger.error("Helm version fetch failed", e);
      return new HelmCommandResponse(CommandExecutionStatus.FAILURE, e.getMessage());
    }
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
}
