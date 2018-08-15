package software.wings.helpers.ext.helm;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.helpers.ext.helm.HelmConstants.DEFAULT_TILLER_CONNECTION_TIMEOUT;

import com.google.common.util.concurrent.TimeLimiter;
import com.google.common.util.concurrent.UncheckedTimeoutException;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.GitFileConfig;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.Log.LogLevel;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.command.LogCallback;
import software.wings.beans.container.HelmChartSpecification;
import software.wings.beans.yaml.GitFetchFilesResult;
import software.wings.beans.yaml.GitFile;
import software.wings.cloudprovider.ContainerInfo;
import software.wings.delegatetasks.helm.HarnessHelmDeployConfig;
import software.wings.delegatetasks.helm.HelmCommandHelper;
import software.wings.delegatetasks.helm.HelmDeployChartSpec;
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
import software.wings.service.intfc.GitService;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.utils.Misc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
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
  @Inject private GitService gitService;
  @Inject private EncryptionService encryptionService;
  @Inject private HelmCommandHelper helmCommandHelper;

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

      fetchValuesYamlFromGitRepo(commandRequest, executionLogCallback);
      if (!helmCommandHelper.checkValidChartSpecification(commandRequest.getChartSpecification())) {
        String msg = "Invalid chart specification "
            + (commandRequest.getChartSpecification() == null ? "NULL"
                                                              : commandRequest.getChartSpecification().toString());
        executionLogCallback.saveExecutionLog(msg);
        return HelmInstallCommandResponse.builder()
            .commandExecutionStatus(CommandExecutionStatus.FAILURE)
            .output(msg)
            .build();
      }

      if (checkNewHelmInstall(commandRequest)) {
        executionLogCallback.saveExecutionLog("No previous deployment found for release. Installing chart");
        commandResponse = helmClient.install(commandRequest);
      } else {
        executionLogCallback.saveExecutionLog("Previous release exists for chart. Upgrading chart");
        commandResponse = helmClient.upgrade(commandRequest);
      }
      executionLogCallback.saveExecutionLog(commandResponse.getOutput());

      if (commandResponse.getCommandExecutionStatus().equals(CommandExecutionStatus.SUCCESS)) {
        List<ContainerInfo> containerInfos = new ArrayList<>();
        timeLimiter.callWithTimeout(
            ()
                -> containerInfos.addAll(fetchContainerInfo(commandRequest, executionLogCallback)),
            commandRequest.getTimeoutInMillis(), TimeUnit.MILLISECONDS, true);
        commandResponse.setContainerInfoList(containerInfos);
      }

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
    } finally {
      if (checkDeleteReleaseNeeded(commandRequest)) {
        executionLogCallback.saveExecutionLog("Deployment failed.");
        deleteAndPurgeHelmRelease(commandRequest, executionLogCallback);
      }
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
  public HelmCommandResponse ensureHelmCliAndTillerInstalled(HelmCommandRequest helmCommandRequest) throws Exception {
    try {
      return timeLimiter.callWithTimeout(() -> {
        HelmCliResponse cliResponse = helmClient.getClientAndServerVersion(helmCommandRequest);
        if (cliResponse.getCommandExecutionStatus().equals(CommandExecutionStatus.FAILURE)) {
          throw new InvalidRequestException(cliResponse.getOutput());
        }
        return new HelmCommandResponse(cliResponse.getCommandExecutionStatus(), cliResponse.getOutput());
      }, Long.parseLong(DEFAULT_TILLER_CONNECTION_TIMEOUT), TimeUnit.SECONDS, true);
    } catch (UncheckedTimeoutException e) {
      String msg = "Timed out while finding helm client and server version";
      logger.error(msg, e);
      throw new InvalidRequestException(msg);
    }
  }

  @Override
  public HelmCommandResponse addPublicRepo(HelmCommandRequest commandRequest, LogCallback executionLogCallback)
      throws InterruptedException, TimeoutException, IOException {
    executionLogCallback.saveExecutionLog(
        "Checking if the repository has already been added", LogLevel.INFO, CommandExecutionStatus.RUNNING);

    HelmCliResponse cliResponse = helmClient.getHelmRepoList(commandRequest);
    if (cliResponse.getCommandExecutionStatus().equals(CommandExecutionStatus.FAILURE)) {
      throw new InvalidRequestException(cliResponse.getOutput());
    }

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
        String msg = "Failed to add repository. Reason: " + cliResponse.getOutput();
        executionLogCallback.saveExecutionLog(msg);
        throw new InvalidRequestException(msg);
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

  private void deleteAndPurgeHelmRelease(HelmInstallCommandRequest commandRequest, LogCallback executionLogCallback) {
    try {
      String message = "Cleaning up. Deleting the release with --purge option";
      executionLogCallback.saveExecutionLog(message);
      HelmCliResponse deleteCommandResponse = helmClient.deleteHelmRelease(commandRequest);
      executionLogCallback.saveExecutionLog(deleteCommandResponse.getOutput());
    } catch (Exception e) {
      logger.error("Helm delete failed", e);
    }
  }

  private boolean checkNewHelmInstall(HelmInstallCommandRequest commandRequest) {
    HelmListReleasesCommandResponse commandResponse = listReleases(commandRequest);

    logger.info(commandResponse.getOutput());
    return commandResponse.getCommandExecutionStatus().equals(CommandExecutionStatus.SUCCESS)
        && isEmpty(commandResponse.getReleaseInfoList());
  }

  private boolean checkDeleteReleaseNeeded(HelmInstallCommandRequest commandRequest) {
    HelmListReleasesCommandResponse commandResponse = listReleases(commandRequest);

    logger.info(commandResponse.getOutput());
    if (commandResponse.getCommandExecutionStatus().equals(CommandExecutionStatus.SUCCESS)) {
      if (isEmpty(commandResponse.getReleaseInfoList())) {
        return false;
      }

      return commandResponse.getReleaseInfoList().stream().anyMatch(releaseInfo
          -> releaseInfo.getRevision().equals("1") && releaseInfo.getStatus().equals("FAILED")
              && releaseInfo.getName().equals(commandRequest.getReleaseName()));
    }

    return false;
  }
  private void fetchValuesYamlFromGitRepo(HelmInstallCommandRequest commandRequest, LogCallback executionLogCallback) {
    if (commandRequest.getGitConfig() == null) {
      return;
    }

    try {
      encryptionService.decrypt(commandRequest.getGitConfig(), commandRequest.getEncryptedDataDetails());

      GitFileConfig gitFileConfig = commandRequest.getGitFileConfig();

      String msg = "Fetching values yaml files from git:\n"
          + "Git repo: " + commandRequest.getGitConfig().getRepoUrl() + "\n"
          + (isNotBlank(gitFileConfig.getBranch()) ? ("Branch: " + gitFileConfig.getBranch() + "\n") : "")
          + (isNotBlank(gitFileConfig.getCommitId()) ? ("Commit Id: " + gitFileConfig.getCommitId() + "\n") : "")
          + "File path: " + gitFileConfig.getFilePath() + "\n";
      executionLogCallback.saveExecutionLog(msg);
      logger.info(msg);

      GitFetchFilesResult gitFetchFilesResult = gitService.fetchFilesByPath(commandRequest.getGitConfig(),
          gitFileConfig.getConnectorId(), gitFileConfig.getCommitId(), gitFileConfig.getBranch(),
          Collections.singletonList(gitFileConfig.getFilePath()), gitFileConfig.isUseBranch());

      if (isNotEmpty(gitFetchFilesResult.getFiles())) {
        executionLogCallback.saveExecutionLog(
            "Found " + gitFetchFilesResult.getFiles().size() + " value yaml files from git\n");

        List<String> valuesYamlFilesFromGit = new ArrayList<>();

        for (GitFile gitFile : gitFetchFilesResult.getFiles()) {
          if (isNotBlank(gitFile.getFileContent())) {
            valuesYamlFilesFromGit.add(gitFile.getFileContent());
            boolean valueOverrriden = false;

            Optional<HarnessHelmDeployConfig> optionalHarnessHelmDeployConfig =
                helmCommandHelper.generateHelmDeployChartSpecFromYaml(gitFile.getFileContent());
            if (optionalHarnessHelmDeployConfig.isPresent()) {
              HelmDeployChartSpec helmDeployChartSpec = optionalHarnessHelmDeployConfig.get().getHelmDeployChartSpec();

              HelmChartSpecification helmChartSpecification;
              if (commandRequest.getChartSpecification() == null) {
                helmChartSpecification = HelmChartSpecification.builder().build();
              } else {
                helmChartSpecification = commandRequest.getChartSpecification();
              }

              if (isNotBlank(helmDeployChartSpec.getName())) {
                executionLogCallback.saveExecutionLog("Overriding chart name from "
                    + helmChartSpecification.getChartName() + " to " + helmDeployChartSpec.getName());
                helmChartSpecification.setChartName(helmDeployChartSpec.getName());
                valueOverrriden = true;
              }
              if (isNotBlank(helmDeployChartSpec.getUrl())) {
                executionLogCallback.saveExecutionLog("Overriding chart url from "
                    + helmChartSpecification.getChartUrl() + " to " + helmDeployChartSpec.getUrl());
                helmChartSpecification.setChartUrl(helmDeployChartSpec.getUrl());
                valueOverrriden = true;
              }
              if (isNotBlank(helmDeployChartSpec.getVersion())) {
                executionLogCallback.saveExecutionLog("Overriding chart version from "
                    + helmChartSpecification.getChartVersion() + " to " + helmDeployChartSpec.getVersion());
                helmChartSpecification.setChartVersion(helmDeployChartSpec.getVersion());
                valueOverrriden = true;
              }

              if (valueOverrriden) {
                commandRequest.setChartSpecification(helmChartSpecification);
                executionLogCallback.saveExecutionLog("");
              }
            }
          }
        }

        if (isNotEmpty(valuesYamlFilesFromGit)) {
          if (isEmpty(commandRequest.getVariableOverridesYamlFiles())) {
            commandRequest.setVariableOverridesYamlFiles(valuesYamlFilesFromGit);
          } else {
            List<String> variableOverridesYamlFiles = new ArrayList<>();
            variableOverridesYamlFiles.addAll(commandRequest.getVariableOverridesYamlFiles());
            variableOverridesYamlFiles.addAll(valuesYamlFilesFromGit);
            commandRequest.setVariableOverridesYamlFiles(variableOverridesYamlFiles);
          }
        }
      } else {
        executionLogCallback.saveExecutionLog("No values yaml file found on git");
      }
    } catch (Exception ex) {
      String msg = "Exception in adding values yaml from git. " + Misc.getMessage(ex);
      logger.error(msg);
      executionLogCallback.saveExecutionLog(msg);
      throw ex;
    }
  }
}
