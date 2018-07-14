package software.wings.helpers.ext.helm;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.common.util.concurrent.TimeLimiter;
import com.google.common.util.concurrent.UncheckedTimeoutException;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.Log.LogLevel;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.command.LogCallback;
import software.wings.cloudprovider.ContainerInfo;
import software.wings.exception.InvalidRequestException;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.container.ContainerDeploymentDelegateHelper;
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
import software.wings.helpers.ext.helm.response.RepoListInfo;
import software.wings.service.impl.ContainerServiceParams;
import software.wings.utils.Misc;

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
  @Inject private ContainerDeploymentDelegateHelper containerDeploymentDelegateHelper;
  @Inject private TimeLimiter timeLimiter;

  private static final Logger logger = LoggerFactory.getLogger(HelmDeployService.class);

  @Override
  public HelmCommandResponse deploy(HelmInstallCommandRequest commandRequest, LogCallback executionLogCallback) {
    try {
      HelmInstallCommandResponse commandResponse;
      executionLogCallback.saveExecutionLog(
          "List all existing deployed releases for release name: " + commandRequest.getReleaseName());
      HelmCliResponse helmCliResponse =
          helmClient.releaseHistory(commandRequest.getKubeConfigLocation(), commandRequest.getReleaseName());
      executionLogCallback.saveExecutionLog(
          preProcessReleaseHistoryCommandOutput(helmCliResponse, commandRequest.getReleaseName()));

      if (helmCliResponse.getCommandExecutionStatus().equals(CommandExecutionStatus.FAILURE)) {
        executionLogCallback.saveExecutionLog("No previous deployment found for release. Installing chart");
        commandResponse = helmClient.install(commandRequest);
      } else {
        executionLogCallback.saveExecutionLog("Previous release exists for chart. Upgrading chart");
        commandResponse = helmClient.upgrade(commandRequest);
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
      return new HelmCommandResponse(CommandExecutionStatus.FAILURE, Misc.getMessage(e));
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Exception in deploying helm chart [{}]", commandRequest, e);
      return new HelmCommandResponse(CommandExecutionStatus.FAILURE, Misc.getMessage(e));
    }
  }

  private List<ContainerInfo> fetchContainerInfo(HelmCommandRequest commandRequest, LogCallback executionLogCallback) {
    ContainerServiceParams containerServiceParams = commandRequest.getContainerServiceParams();

    KubernetesConfig kubernetesConfig = containerDeploymentDelegateHelper.getKubernetesConfig(containerServiceParams);

    return containerDeploymentDelegateHelper.getContainerInfosWhenReadyByLabel(
        "release", commandRequest.getReleaseName(), containerServiceParams, kubernetesConfig, executionLogCallback);
  }

  @Override
  public HelmCommandResponse rollback(HelmRollbackCommandRequest commandRequest, LogCallback executionLogCallback) {
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
      return new HelmCommandResponse(CommandExecutionStatus.FAILURE, Misc.getMessage(e));
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Helm chart rollback failed [{}]", commandRequest, e);
      return new HelmCommandResponse(CommandExecutionStatus.FAILURE, Misc.getMessage(e));
    }
  }

  @Override
  public HelmCommandResponse ensureHelmCliAndTillerInstalled(HelmCommandRequest helmCommandRequest)
      throws InterruptedException, IOException, TimeoutException {
    HelmCliResponse cliResponse = helmClient.getClientAndServerVersion(helmCommandRequest);
    if (cliResponse.getCommandExecutionStatus().equals(CommandExecutionStatus.FAILURE)) {
      throw new InvalidRequestException(cliResponse.getOutput());
    }
    return new HelmCommandResponse(cliResponse.getCommandExecutionStatus(), cliResponse.getOutput());
  }

  @Override
  public HelmCommandResponse addPublicRepo(HelmCommandRequest commandRequest, LogCallback executionLogCallback)
      throws InterruptedException, TimeoutException, IOException {
    executionLogCallback.saveExecutionLog(
        "Checking if the repository has already been added", LogLevel.INFO, CommandExecutionStatus.RUNNING);

    HelmCliResponse cliResponse = helmClient.getHelmRepoList(commandRequest);
    List<RepoListInfo> repoListInfos = parseHelmAddRepoOutput(cliResponse.getOutput());

    boolean repoAlreadyAdded = repoListInfos.stream().anyMatch(
        repoListInfo -> repoListInfo.getRepoUrl().equals(commandRequest.getChartSpecification().getChartUrl()));

    String responseMsg;
    if (!repoAlreadyAdded) {
      executionLogCallback.saveExecutionLog("Repository not found", LogLevel.INFO, CommandExecutionStatus.RUNNING);
      executionLogCallback.saveExecutionLog("Adding repository " + commandRequest.getChartSpecification().getChartUrl()
              + " with name " + commandRequest.getRepoName(),
          LogLevel.INFO, CommandExecutionStatus.RUNNING);
      cliResponse = helmClient.addPublicRepo(commandRequest);
      if (cliResponse.getCommandExecutionStatus().equals(CommandExecutionStatus.FAILURE)) {
        throw new InvalidRequestException(cliResponse.getOutput());
      }
      responseMsg = "Successfully added repository " + commandRequest.getChartSpecification().getChartUrl()
          + " with name " + commandRequest.getRepoName() + "\n";
    } else {
      responseMsg = "Repo " + commandRequest.getChartSpecification().getChartUrl() + " already added. Ignore adding\n";
    }

    return new HelmCommandResponse(cliResponse.getCommandExecutionStatus(), responseMsg);
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
          .output(Misc.getMessage(e))
          .build();
    }
  }

  @Override
  public HelmReleaseHistoryCommandResponse releaseHistory(HelmReleaseHistoryCommandRequest helmCommandRequest) {
    List<ReleaseInfo> releaseInfoList = new ArrayList<>();
    try {
      HelmCliResponse helmCliResponse =
          helmClient.releaseHistory(helmCommandRequest.getKubeConfigLocation(), helmCommandRequest.getReleaseName());
      releaseInfoList =
          parseHelmReleaseCommandOutput(helmCliResponse.getOutput(), helmCommandRequest.getHelmCommandType());
    } catch (Exception e) {
      logger.error("Helm list releases failed", e);
    }
    return HelmReleaseHistoryCommandResponse.builder()
        .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
        .releaseInfoList(releaseInfoList)
        .build();
  }

  private List<ReleaseInfo> parseHelmReleaseCommandOutput(String listReleaseOutput, HelmCommandType helmCommandType)
      throws IOException {
    if (isEmpty(listReleaseOutput)) {
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

  private List<RepoListInfo> parseHelmAddRepoOutput(String listReleaseOutput) throws IOException {
    if (isEmpty(listReleaseOutput)) {
      return new ArrayList<>();
    }

    CSVFormat csvFormat = CSVFormat.RFC4180.withFirstRecordAsHeader().withDelimiter('\t').withTrim();
    return CSVParser.parse(listReleaseOutput, csvFormat)
        .getRecords()
        .stream()
        .map(this ::repoListCsvRecordToRepoListInfo)
        .collect(Collectors.toList());
  }

  private RepoListInfo repoListCsvRecordToRepoListInfo(CSVRecord repoListRecord) {
    return RepoListInfo.builder().repoName(repoListRecord.get("NAME")).repoUrl(repoListRecord.get("URL")).build();
  }

  private String preProcessReleaseHistoryCommandOutput(HelmCliResponse helmCliResponse, String releaseName) {
    if (helmCliResponse.getCommandExecutionStatus().equals(CommandExecutionStatus.FAILURE)) {
      return "Release: \"" + releaseName + "\" not found\n";
    }

    return helmCliResponse.getOutput();
  }
}
