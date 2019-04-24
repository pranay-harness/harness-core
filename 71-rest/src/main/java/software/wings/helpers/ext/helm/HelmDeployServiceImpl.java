package software.wings.helpers.ext.helm;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.replace;
import static software.wings.helpers.ext.helm.HelmConstants.DEFAULT_TILLER_CONNECTION_TIMEOUT_SECONDS;

import com.google.common.util.concurrent.TimeLimiter;
import com.google.common.util.concurrent.UncheckedTimeoutException;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.filesystem.FileIo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import software.wings.beans.GitConfig;
import software.wings.beans.GitConfig.GitRepositoryType;
import software.wings.beans.GitFileConfig;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.Log.LogLevel;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.beans.command.HelmDummyCommandUnit;
import software.wings.beans.command.LogCallback;
import software.wings.beans.container.HelmChartSpecification;
import software.wings.beans.yaml.GitFetchFilesResult;
import software.wings.beans.yaml.GitFile;
import software.wings.cloudprovider.ContainerInfo;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.delegatetasks.helm.HarnessHelmDeployConfig;
import software.wings.delegatetasks.helm.HelmCommandHelper;
import software.wings.delegatetasks.helm.HelmDeployChartSpec;
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
import software.wings.helpers.ext.k8s.request.K8sDelegateManifestConfig;
import software.wings.service.impl.ContainerServiceParams;
import software.wings.service.intfc.GitService;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.yaml.GitClient;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
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
@Slf4j
public class HelmDeployServiceImpl implements HelmDeployService {
  @Inject private HelmClient helmClient;
  @Inject private ContainerDeploymentDelegateHelper containerDeploymentDelegateHelper;
  @Inject private TimeLimiter timeLimiter;
  @Inject private GitService gitService;
  @Inject private EncryptionService encryptionService;
  @Inject private HelmCommandHelper helmCommandHelper;
  @Inject private DelegateLogService delegateLogService;
  @Inject private GitClient gitClient;

  private static final String ACTIVITY_ID = "ACTIVITY_ID";
  private static final String WORKING_DIR = "./repository/helm/source/${" + ACTIVITY_ID + "}";

  @Override
  public HelmCommandResponse deploy(HelmInstallCommandRequest commandRequest) throws IOException {
    LogCallback executionLogCallback = commandRequest.getExecutionLogCallback();

    try {
      HelmInstallCommandResponse commandResponse;
      executionLogCallback.saveExecutionLog(
          "List all existing deployed releases for release name: " + commandRequest.getReleaseName());

      HelmCliResponse helmCliResponse = helmClient.releaseHistory(commandRequest);
      executionLogCallback.saveExecutionLog(
          preProcessReleaseHistoryCommandOutput(helmCliResponse, commandRequest.getReleaseName()));

      fetchValuesYamlFromGitRepo(commandRequest, executionLogCallback);
      if (commandRequest.getRepoConfig() == null) {
        addRepoForCommand(commandRequest);
        repoUpdate(commandRequest);
      }
      fetchSourceRepo(commandRequest);

      if (commandRequest.getRepoConfig() == null
          && !helmCommandHelper.checkValidChartSpecification(commandRequest.getChartSpecification())) {
        String msg =
            new StringBuilder("Couldn't find valid helm chart specification from service or values.yaml from git\n")
                .append((commandRequest.getChartSpecification() != null) ? commandRequest.getChartSpecification() + "\n"
                                                                         : "")
                .append("Please specify helm chart specification either in service or git repo\n")
                .toString();

        logger.info(msg);
        executionLogCallback.saveExecutionLog(msg);
        return HelmInstallCommandResponse.builder()
            .commandExecutionStatus(CommandExecutionStatus.FAILURE)
            .output(msg)
            .build();
      }

      executionLogCallback =
          markDoneAndStartNew(commandRequest, executionLogCallback, HelmDummyCommandUnit.InstallUpgrade);

      if (checkNewHelmInstall(commandRequest)) {
        executionLogCallback.saveExecutionLog("No previous deployment found for release. Installing chart");
        commandResponse = helmClient.install(commandRequest);
      } else {
        executionLogCallback.saveExecutionLog("Previous release exists for chart. Upgrading chart");
        commandResponse = helmClient.upgrade(commandRequest);
      }
      executionLogCallback.saveExecutionLog(commandResponse.getOutput());

      if (commandResponse.getCommandExecutionStatus() != CommandExecutionStatus.SUCCESS) {
        return commandResponse;
      }

      executionLogCallback =
          markDoneAndStartNew(commandRequest, executionLogCallback, HelmDummyCommandUnit.WaitForSteadyState);

      List<ContainerInfo> containerInfos = new ArrayList<>();
      LogCallback finalExecutionLogCallback = executionLogCallback;
      timeLimiter.callWithTimeout(
          ()
              -> containerInfos.addAll(fetchContainerInfo(commandRequest, finalExecutionLogCallback)),
          commandRequest.getTimeoutInMillis(), TimeUnit.MILLISECONDS, true);
      commandResponse.setContainerInfoList(containerInfos);

      executionLogCallback = markDoneAndStartNew(commandRequest, executionLogCallback, HelmDummyCommandUnit.WrapUp);

      return commandResponse;
    } catch (UncheckedTimeoutException e) {
      String msg = "Timed out waiting for controller to reach in steady state";
      logger.error(msg, e);
      executionLogCallback.saveExecutionLog(
          "Timed out waiting for controller to reach in steady state", LogLevel.ERROR);
      return new HelmCommandResponse(CommandExecutionStatus.FAILURE, ExceptionUtils.getMessage(e));
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      String exceptionMessage = ExceptionUtils.getMessage(e);
      String msg = format("Exception in deploying helm chart " + exceptionMessage);
      logger.error(msg, e);
      executionLogCallback.saveExecutionLog(msg, LogLevel.ERROR);
      return new HelmCommandResponse(CommandExecutionStatus.FAILURE, exceptionMessage);
    } finally {
      if (checkDeleteReleaseNeeded(commandRequest)) {
        executionLogCallback.saveExecutionLog("Deployment failed.");
        deleteAndPurgeHelmRelease(commandRequest, executionLogCallback);
      }
      FileIo.deleteDirectoryAndItsContentIfExists(getWorkingDirectory(commandRequest));
    }
  }

  private void fetchSourceRepo(HelmInstallCommandRequest commandRequest) throws IOException {
    K8sDelegateManifestConfig sourceRepoConfig = commandRequest.getRepoConfig();
    if (sourceRepoConfig == null) {
      return;
    }
    GitConfig gitConfig = sourceRepoConfig.getGitConfig();
    GitFileConfig gitFileConfig = sourceRepoConfig.getGitFileConfig();
    gitConfig.setGitRepoType(GitRepositoryType.HELM);
    gitConfig.setBranch(gitFileConfig.getBranch());
    if (!gitFileConfig.isUseBranch()) {
      gitConfig.setReference(gitFileConfig.getCommitId());
    }
    encryptionService.decrypt(gitConfig, sourceRepoConfig.getEncryptedDataDetails());
    GitFetchFilesResult gitFetchFilesResult = gitService.fetchFilesByPath(gitConfig, gitFileConfig.getConnectorId(),
        gitFileConfig.getCommitId(), gitFileConfig.getBranch(), Collections.singletonList(gitFileConfig.getFilePath()),
        gitFileConfig.isUseBranch());

    String workingDirectory = getWorkingDirectory(commandRequest);
    FileIo.createDirectoryIfDoesNotExist(workingDirectory);
    FileIo.waitForDirectoryToBeAccessibleOutOfProcess(workingDirectory, 10);
    for (GitFile file : gitFetchFilesResult.getFiles()) {
      Path filePath = Paths.get(workingDirectory, file.getFilePath());
      Path parentPath = filePath.getParent();
      if (parentPath == null) {
        throw new WingsException("Parent path not found for file: " + file.getFilePath());
      }
      FileIo.createDirectoryIfDoesNotExist(parentPath.toString());
      FileIo.waitForDirectoryToBeAccessibleOutOfProcess(workingDirectory, 10);
      FileIo.writeUtf8StringToFile(filePath.toString(), file.getFileContent());
    }
    commandRequest.setWorkingDir(Paths.get(workingDirectory, gitFileConfig.getFilePath()).toString());
    commandRequest.getExecutionLogCallback().saveExecutionLog("Repo checked-out locally");
  }

  private LogCallback markDoneAndStartNew(
      HelmCommandRequest commandRequest, LogCallback executionLogCallback, String newName) {
    executionLogCallback.saveExecutionLog("\nDone", LogLevel.INFO, CommandExecutionStatus.SUCCESS);
    executionLogCallback = getExecutionLogCallback(commandRequest, newName);
    commandRequest.setExecutionLogCallback(executionLogCallback);
    return executionLogCallback;
  }

  private List<ContainerInfo> fetchContainerInfo(HelmCommandRequest commandRequest, LogCallback executionLogCallback) {
    ContainerServiceParams containerServiceParams = commandRequest.getContainerServiceParams();

    KubernetesConfig kubernetesConfig = containerDeploymentDelegateHelper.getKubernetesConfig(containerServiceParams);

    return containerDeploymentDelegateHelper.getContainerInfosWhenReadyByLabel(
        "release", commandRequest.getReleaseName(), containerServiceParams, kubernetesConfig, executionLogCallback);
  }

  @Override
  public HelmCommandResponse rollback(HelmRollbackCommandRequest commandRequest) {
    LogCallback executionLogCallback = getExecutionLogCallback(commandRequest, HelmDummyCommandUnit.Rollback);
    commandRequest.setExecutionLogCallback(executionLogCallback);

    try {
      HelmInstallCommandResponse commandResponse = helmClient.rollback(commandRequest);
      executionLogCallback.saveExecutionLog(commandResponse.getOutput());
      if (commandResponse.getCommandExecutionStatus() != CommandExecutionStatus.SUCCESS) {
        return commandResponse;
      }
      executionLogCallback =
          markDoneAndStartNew(commandRequest, executionLogCallback, HelmDummyCommandUnit.WaitForSteadyState);

      List<ContainerInfo> containerInfos = new ArrayList<>();
      LogCallback finalExecutionLogCallback = executionLogCallback;
      timeLimiter.callWithTimeout(
          ()
              -> containerInfos.addAll(fetchContainerInfo(commandRequest, finalExecutionLogCallback)),
          commandRequest.getTimeoutInMillis(), TimeUnit.MILLISECONDS, true);
      commandResponse.setContainerInfoList(containerInfos);

      executionLogCallback.saveExecutionLog("\nDone", LogLevel.INFO, CommandExecutionStatus.SUCCESS);
      return commandResponse;
    } catch (UncheckedTimeoutException e) {
      String msg = "Timed out waiting for controller to reach in steady state";
      logger.error(msg, e);
      executionLogCallback.saveExecutionLog(
          "Timed out waiting for controller to reach in steady state", LogLevel.ERROR);
      return new HelmCommandResponse(CommandExecutionStatus.FAILURE, ExceptionUtils.getMessage(e));
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      logger.error(format("Helm chart rollback failed [%s]", commandRequest.toString()), e);
      return new HelmCommandResponse(CommandExecutionStatus.FAILURE, ExceptionUtils.getMessage(e));
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
      }, Long.parseLong(DEFAULT_TILLER_CONNECTION_TIMEOUT_SECONDS), TimeUnit.SECONDS, true);
    } catch (UncheckedTimeoutException e) {
      String msg = "Timed out while finding helm client and server version";
      logger.error(msg, e);
      throw new InvalidRequestException(msg);
    }
  }

  @Override
  public HelmCommandResponse addPublicRepo(HelmCommandRequest commandRequest)
      throws InterruptedException, TimeoutException, IOException {
    LogCallback executionLogCallback = commandRequest.getExecutionLogCallback();

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
          .output(ExceptionUtils.getMessage(e))
          .build();
    }
  }

  @Override
  public HelmReleaseHistoryCommandResponse releaseHistory(HelmReleaseHistoryCommandRequest helmCommandRequest) {
    List<ReleaseInfo> releaseInfoList = new ArrayList<>();
    try {
      HelmCliResponse helmCliResponse = helmClient.releaseHistory(helmCommandRequest);
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
                String chartNameMsg = isNotBlank(helmChartSpecification.getChartName())
                    ? " from " + helmChartSpecification.getChartName()
                    : "";

                executionLogCallback.saveExecutionLog(
                    "Overriding chart name" + chartNameMsg + " to " + helmDeployChartSpec.getName());
                helmChartSpecification.setChartName(helmDeployChartSpec.getName());
                valueOverrriden = true;
              }
              if (isNotBlank(helmDeployChartSpec.getUrl())) {
                String chartUrlMsg = isNotBlank(helmChartSpecification.getChartUrl())
                    ? " from " + helmChartSpecification.getChartUrl()
                    : "";

                executionLogCallback.saveExecutionLog(
                    "Overriding chart url" + chartUrlMsg + " to " + helmDeployChartSpec.getUrl());
                helmChartSpecification.setChartUrl(helmDeployChartSpec.getUrl());
                valueOverrriden = true;
              }
              if (isNotBlank(helmDeployChartSpec.getVersion())) {
                String chartVersionMsg = isNotBlank(helmChartSpecification.getChartVersion())
                    ? " from " + helmChartSpecification.getChartVersion()
                    : "";

                executionLogCallback.saveExecutionLog(
                    "Overriding chart version" + chartVersionMsg + " to " + helmDeployChartSpec.getVersion());
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
      String msg = "Exception in adding values yaml from git. " + ExceptionUtils.getMessage(ex);
      logger.error(msg);
      executionLogCallback.saveExecutionLog(msg);
      throw ex;
    }
  }

  private void addRepoForCommand(HelmInstallCommandRequest helmCommandRequest)
      throws InterruptedException, IOException, TimeoutException {
    LogCallback executionLogCallback = helmCommandRequest.getExecutionLogCallback();

    if (helmCommandRequest.getHelmCommandType() != HelmCommandType.INSTALL) {
      return;
    }

    if (helmCommandRequest.getChartSpecification() != null
        && isNotEmpty(helmCommandRequest.getChartSpecification().getChartUrl())
        && isNotEmpty(helmCommandRequest.getRepoName())) {
      executionLogCallback.saveExecutionLog(
          "Adding helm repository " + helmCommandRequest.getChartSpecification().getChartUrl(), LogLevel.INFO,
          CommandExecutionStatus.RUNNING);
      HelmCommandResponse helmCommandResponse = addPublicRepo(helmCommandRequest);
      executionLogCallback.saveExecutionLog(helmCommandResponse.getOutput());
    }
  }

  private void repoUpdate(HelmInstallCommandRequest helmCommandRequest)
      throws InterruptedException, TimeoutException, IOException {
    if (HelmCommandType.INSTALL != helmCommandRequest.getHelmCommandType()) {
      return;
    }

    LogCallback executionLogCallback = helmCommandRequest.getExecutionLogCallback();
    executionLogCallback.saveExecutionLog("Updating information about charts from the respective chart repositories");

    try {
      HelmCliResponse helmCliResponse = helmClient.repoUpdate(helmCommandRequest);
      executionLogCallback.saveExecutionLog(helmCliResponse.getOutput());
    } catch (Exception ex) {
      executionLogCallback.saveExecutionLog(
          "Failed to update information about charts with message " + ExceptionUtils.getMessage(ex));
      throw ex;
    }
  }

  protected LogCallback getExecutionLogCallback(HelmCommandRequest helmCommandRequest, String name) {
    return new ExecutionLogCallback(delegateLogService, helmCommandRequest.getAccountId(),
        helmCommandRequest.getAppId(), helmCommandRequest.getActivityId(), name);
  }

  public String getWorkingDirectory(HelmCommandRequest commandRequest) {
    return replace(WORKING_DIR, "${" + ACTIVITY_ID + "}", commandRequest.getActivityId());
  }
}
