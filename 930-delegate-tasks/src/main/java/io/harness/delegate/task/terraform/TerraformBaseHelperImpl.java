/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.task.terraform;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.beans.DelegateFile.Builder.aDelegateFile;
import static io.harness.delegate.beans.connector.scm.GitAuthType.SSH;
import static io.harness.delegate.task.terraform.TerraformCommand.APPLY;
import static io.harness.delegate.task.terraform.TerraformCommand.DESTROY;
import static io.harness.eraro.ErrorCode.DEFAULT_ERROR_CODE;
import static io.harness.logging.LogLevel.ERROR;
import static io.harness.logging.LogLevel.INFO;
import static io.harness.logging.LogLevel.WARN;
import static io.harness.provision.TerraformConstants.TERRAFORM_APPLY_PLAN_FILE_VAR_NAME;
import static io.harness.provision.TerraformConstants.TERRAFORM_DESTROY_PLAN_FILE_OUTPUT_NAME;
import static io.harness.provision.TerraformConstants.TERRAFORM_DESTROY_PLAN_FILE_VAR_NAME;
import static io.harness.provision.TerraformConstants.TERRAFORM_PLAN_FILE_OUTPUT_NAME;
import static io.harness.provision.TerraformConstants.TERRAFORM_PLAN_JSON_FILE_NAME;
import static io.harness.provision.TerraformConstants.TERRAFORM_STATE_FILE_NAME;
import static io.harness.provision.TerraformConstants.TERRAFORM_VARIABLES_FILE_NAME;
import static io.harness.provision.TerraformConstants.TF_BASE_DIR;
import static io.harness.provision.TerraformConstants.TF_SCRIPT_DIR;
import static io.harness.provision.TerraformConstants.TF_WORKING_DIR;
import static io.harness.provision.TerraformConstants.USER_DIR_KEY;
import static io.harness.provision.TerraformConstants.WORKSPACE_STATE_FILE_PATH_FORMAT;

import static software.wings.beans.LogHelper.color;

import static java.lang.String.format;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cli.CliResponse;
import io.harness.connector.service.git.NGGitService;
import io.harness.connector.task.shell.SshSessionConfigMapper;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.beans.DelegateFile;
import io.harness.delegate.beans.DelegateFileManagerBase;
import io.harness.delegate.beans.FileBucket;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.storeconfig.GitStoreDelegateConfig;
import io.harness.delegate.task.git.GitFetchFilesConfig;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.TerraformCommandExecutionException;
import io.harness.exception.WingsException;
import io.harness.exception.runtime.JGitRuntimeException;
import io.harness.git.GitClientHelper;
import io.harness.git.GitClientV2;
import io.harness.git.model.DownloadFilesRequest;
import io.harness.git.model.GitBaseRequest;
import io.harness.git.model.GitRepositoryType;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;
import io.harness.logging.PlanJsonLogOutputStream;
import io.harness.secretmanagerclient.EncryptDecryptHelper;
import io.harness.security.encryption.EncryptedRecordData;
import io.harness.security.encryption.EncryptionConfig;
import io.harness.security.encryption.SecretDecryptionService;
import io.harness.shell.SshSessionConfig;
import io.harness.terraform.TerraformClient;
import io.harness.terraform.TerraformHelperUtils;
import io.harness.terraform.request.TerraformApplyCommandRequest;
import io.harness.terraform.request.TerraformDestroyCommandRequest;
import io.harness.terraform.request.TerraformExecuteStepRequest;
import io.harness.terraform.request.TerraformInitCommandRequest;
import io.harness.terraform.request.TerraformPlanCommandRequest;
import io.harness.terraform.request.TerraformRefreshCommandRequest;

import software.wings.beans.LogColor;
import software.wings.beans.LogWeight;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.input.NullInputStream;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.jetbrains.annotations.NotNull;

@Slf4j
@OwnedBy(CDP)
public class TerraformBaseHelperImpl implements TerraformBaseHelper {
  @Inject DelegateFileManagerBase delegateFileManagerBase;
  @Inject TerraformClient terraformClient;
  @Inject EncryptDecryptHelper encryptDecryptHelper;
  @Inject GitClientV2 gitClient;
  @Inject GitClientHelper gitClientHelper;
  @Inject SecretDecryptionService secretDecryptionService;
  @Inject SshSessionConfigMapper sshSessionConfigMapper;
  @Inject NGGitService ngGitService;
  @Inject DelegateFileManagerBase delegateFileManager;

  @Override
  public void downloadTfStateFile(String workspace, String accountId, String currentStateFileId, String scriptDirectory)
      throws IOException {
    File tfStateFile = (isEmpty(workspace))
        ? Paths.get(scriptDirectory, TERRAFORM_STATE_FILE_NAME).toFile()
        : Paths.get(scriptDirectory, format(WORKSPACE_STATE_FILE_PATH_FORMAT, workspace)).toFile();

    if (currentStateFileId != null) {
      try (InputStream stateRemoteInputStream =
               delegateFileManagerBase.downloadByFileId(FileBucket.TERRAFORM_STATE, currentStateFileId, accountId)) {
        FileUtils.copyInputStreamToFile(stateRemoteInputStream, tfStateFile);
      }
    } else {
      FileUtils.deleteQuietly(tfStateFile);
    }
  }

  @Override
  public List<String> parseOutput(String workspaceOutput) {
    List<String> outputs = Arrays.asList(StringUtils.split(workspaceOutput, "\n"));
    List<String> workspaces = new ArrayList<>();
    for (String output : outputs) {
      if (output.charAt(0) == '*') {
        output = output.substring(1);
      }
      output = output.trim();
      workspaces.add(output);
    }
    return workspaces;
  }

  @Override
  public CliResponse executeTerraformApplyStep(TerraformExecuteStepRequest terraformExecuteStepRequest)
      throws InterruptedException, IOException, TimeoutException, TerraformCommandExecutionException {
    CliResponse response;
    TerraformInitCommandRequest terraformInitCommandRequest =
        TerraformInitCommandRequest.builder()
            .tfBackendConfigsFilePath(terraformExecuteStepRequest.getTfBackendConfigsFile())
            .build();
    terraformClient.init(terraformInitCommandRequest, terraformExecuteStepRequest.getTimeoutInMillis(),
        terraformExecuteStepRequest.getEnvVars(), terraformExecuteStepRequest.getScriptDirectory(),
        terraformExecuteStepRequest.getLogCallback());

    String workspace = terraformExecuteStepRequest.getWorkspace();
    if (isNotEmpty(workspace)) {
      selectWorkspaceIfExist(terraformExecuteStepRequest, workspace);
    }

    if (!(terraformExecuteStepRequest.getEncryptedTfPlan() != null
            && terraformExecuteStepRequest.isSkipRefreshBeforeApplyingPlan())) {
      TerraformRefreshCommandRequest terraformRefreshCommandRequest =
          TerraformRefreshCommandRequest.builder()
              .varFilePaths(terraformExecuteStepRequest.getTfVarFilePaths())
              .varParams(terraformExecuteStepRequest.getVarParams())
              .targets(terraformExecuteStepRequest.getTargets())
              .uiLogs(terraformExecuteStepRequest.getUiLogs())
              .build();
      terraformClient.refresh(terraformRefreshCommandRequest, terraformExecuteStepRequest.getTimeoutInMillis(),
          terraformExecuteStepRequest.getEnvVars(), terraformExecuteStepRequest.getScriptDirectory(),
          terraformExecuteStepRequest.getLogCallback());
    }

    // Execute TF plan
    executeTerraformPlanCommand(terraformExecuteStepRequest);

    TerraformApplyCommandRequest terraformApplyCommandRequest =
        TerraformApplyCommandRequest.builder().planName(TERRAFORM_PLAN_FILE_OUTPUT_NAME).build();
    terraformClient.apply(terraformApplyCommandRequest, terraformExecuteStepRequest.getTimeoutInMillis(),
        terraformExecuteStepRequest.getEnvVars(), terraformExecuteStepRequest.getScriptDirectory(),
        terraformExecuteStepRequest.getLogCallback());

    response = terraformClient.output(terraformExecuteStepRequest.getTfOutputsFile(),
        terraformExecuteStepRequest.getTimeoutInMillis(), terraformExecuteStepRequest.getEnvVars(),
        terraformExecuteStepRequest.getScriptDirectory(), terraformExecuteStepRequest.getLogCallback());
    return response;
  }

  private void selectWorkspaceIfExist(TerraformExecuteStepRequest terraformExecuteStepRequest, String workspace)
      throws InterruptedException, TimeoutException, IOException {
    CliResponse response;
    response = terraformClient.getWorkspaceList(terraformExecuteStepRequest.getTimeoutInMillis(),
        terraformExecuteStepRequest.getEnvVars(), terraformExecuteStepRequest.getScriptDirectory(),
        terraformExecuteStepRequest.getLogCallback());
    if (response != null && response.getOutput() != null) {
      List<String> workspacelist = parseOutput(response.getOutput());
      // if workspace is specified in Harness but none exists in the environment, then select a new workspace
      terraformClient.workspace(workspace, workspacelist.contains(workspace),
          terraformExecuteStepRequest.getTimeoutInMillis(), terraformExecuteStepRequest.getEnvVars(),
          terraformExecuteStepRequest.getScriptDirectory(), terraformExecuteStepRequest.getLogCallback());
    }
  }

  @NotNull
  @VisibleForTesting
  public CliResponse executeTerraformPlanCommand(TerraformExecuteStepRequest terraformExecuteStepRequest)
      throws InterruptedException, TimeoutException, IOException {
    CliResponse response = null;
    if (terraformExecuteStepRequest.getEncryptedTfPlan() != null) {
      terraformExecuteStepRequest.getLogCallback().saveExecutionLog(
          color("\nDecrypting terraform plan before applying\n", LogColor.Yellow, LogWeight.Bold), INFO,
          CommandExecutionStatus.RUNNING);
      saveTerraformPlanContentToFile(terraformExecuteStepRequest.getEncryptionConfig(),
          terraformExecuteStepRequest.getEncryptedTfPlan(), terraformExecuteStepRequest.getScriptDirectory(),
          terraformExecuteStepRequest.getAccountId(), TERRAFORM_PLAN_FILE_OUTPUT_NAME);
      terraformExecuteStepRequest.getLogCallback().saveExecutionLog(
          color("\nUsing approved terraform plan \n", LogColor.Yellow, LogWeight.Bold), INFO,
          CommandExecutionStatus.RUNNING);
    } else {
      terraformExecuteStepRequest.getLogCallback().saveExecutionLog(
          color("\nGenerating terraform plan\n", LogColor.Yellow, LogWeight.Bold), INFO,
          CommandExecutionStatus.RUNNING);

      TerraformPlanCommandRequest terraformPlanCommandRequest =
          TerraformPlanCommandRequest.builder()
              .varFilePaths(terraformExecuteStepRequest.getTfVarFilePaths())
              .varParams(terraformExecuteStepRequest.getVarParams())
              .targets(terraformExecuteStepRequest.getTargets())
              .destroySet(terraformExecuteStepRequest.isTfPlanDestroy())
              .uiLogs(terraformExecuteStepRequest.getUiLogs())
              .build();
      response = terraformClient.plan(terraformPlanCommandRequest, terraformExecuteStepRequest.getTimeoutInMillis(),
          terraformExecuteStepRequest.getEnvVars(), terraformExecuteStepRequest.getScriptDirectory(),
          terraformExecuteStepRequest.getLogCallback());

      if (terraformExecuteStepRequest.isSaveTerraformJson()) {
        response =
            executeTerraformShowCommandWithTfClient(terraformExecuteStepRequest.isTfPlanDestroy() ? DESTROY : APPLY,
                terraformExecuteStepRequest.getTimeoutInMillis(), terraformExecuteStepRequest.getEnvVars(),
                terraformExecuteStepRequest.getScriptDirectory(), terraformExecuteStepRequest.getLogCallback(),
                terraformExecuteStepRequest.getPlanJsonLogOutputStream(),
                terraformExecuteStepRequest.isUseOptimizedTfPlan());
      }
    }
    return response;
  }

  @Override
  public CliResponse executeTerraformPlanStep(TerraformExecuteStepRequest terraformExecuteStepRequest)
      throws InterruptedException, IOException, TimeoutException {
    CliResponse response;
    TerraformInitCommandRequest terraformInitCommandRequest =
        TerraformInitCommandRequest.builder()
            .tfBackendConfigsFilePath(terraformExecuteStepRequest.getTfBackendConfigsFile())
            .build();
    terraformClient.init(terraformInitCommandRequest, terraformExecuteStepRequest.getTimeoutInMillis(),
        terraformExecuteStepRequest.getEnvVars(), terraformExecuteStepRequest.getScriptDirectory(),
        terraformExecuteStepRequest.getLogCallback());

    String workspace = terraformExecuteStepRequest.getWorkspace();
    if (isNotEmpty(workspace)) {
      selectWorkspaceIfExist(terraformExecuteStepRequest, workspace);
    }

    // Plan step always performs a refresh
    TerraformRefreshCommandRequest terraformRefreshCommandRequest =
        TerraformRefreshCommandRequest.builder()
            .varFilePaths(terraformExecuteStepRequest.getTfVarFilePaths())
            .varParams(terraformExecuteStepRequest.getVarParams())
            .targets(terraformExecuteStepRequest.getTargets())
            .uiLogs(terraformExecuteStepRequest.getUiLogs())
            .build();
    terraformClient.refresh(terraformRefreshCommandRequest, terraformExecuteStepRequest.getTimeoutInMillis(),
        terraformExecuteStepRequest.getEnvVars(), terraformExecuteStepRequest.getScriptDirectory(),
        terraformExecuteStepRequest.getLogCallback());

    response = executeTerraformPlanCommand(terraformExecuteStepRequest);

    return response;
  }

  @Override
  public CliResponse executeTerraformDestroyStep(TerraformExecuteStepRequest terraformExecuteStepRequest)
      throws InterruptedException, IOException, TimeoutException {
    CliResponse response;
    TerraformInitCommandRequest terraformInitCommandRequest =
        TerraformInitCommandRequest.builder()
            .tfBackendConfigsFilePath(terraformExecuteStepRequest.getTfBackendConfigsFile())
            .build();
    terraformClient.init(terraformInitCommandRequest, terraformExecuteStepRequest.getTimeoutInMillis(),
        terraformExecuteStepRequest.getEnvVars(), terraformExecuteStepRequest.getScriptDirectory(),
        terraformExecuteStepRequest.getLogCallback());

    String workspace = terraformExecuteStepRequest.getWorkspace();
    if (isNotEmpty(workspace)) {
      selectWorkspaceIfExist(terraformExecuteStepRequest, workspace);
    }

    if (!(terraformExecuteStepRequest.getEncryptedTfPlan() != null
            && terraformExecuteStepRequest.isSkipRefreshBeforeApplyingPlan())) {
      TerraformRefreshCommandRequest terraformRefreshCommandRequest =
          TerraformRefreshCommandRequest.builder()
              .varFilePaths(terraformExecuteStepRequest.getTfVarFilePaths())
              .varParams(terraformExecuteStepRequest.getVarParams())
              .targets(terraformExecuteStepRequest.getTargets())
              .uiLogs(terraformExecuteStepRequest.getUiLogs())
              .build();
      terraformClient.refresh(terraformRefreshCommandRequest, terraformExecuteStepRequest.getTimeoutInMillis(),
          terraformExecuteStepRequest.getEnvVars(), terraformExecuteStepRequest.getScriptDirectory(),
          terraformExecuteStepRequest.getLogCallback());
    }

    if (terraformExecuteStepRequest.isRunPlanOnly()) {
      TerraformPlanCommandRequest terraformPlanCommandRequest =
          TerraformPlanCommandRequest.builder()
              .varFilePaths(terraformExecuteStepRequest.getTfVarFilePaths())
              .varParams(terraformExecuteStepRequest.getVarParams())
              .targets(terraformExecuteStepRequest.getTargets())
              .destroySet(true)
              .uiLogs(terraformExecuteStepRequest.getUiLogs())
              .build();
      response = terraformClient.plan(terraformPlanCommandRequest, terraformExecuteStepRequest.getTimeoutInMillis(),
          terraformExecuteStepRequest.getEnvVars(), terraformExecuteStepRequest.getScriptDirectory(),
          terraformExecuteStepRequest.getLogCallback());

      if (terraformExecuteStepRequest.isSaveTerraformJson()) {
        response = executeTerraformShowCommandWithTfClient(DESTROY, terraformExecuteStepRequest.getTimeoutInMillis(),
            terraformExecuteStepRequest.getEnvVars(), terraformExecuteStepRequest.getScriptDirectory(),
            terraformExecuteStepRequest.getLogCallback(), terraformExecuteStepRequest.getPlanJsonLogOutputStream(),
            terraformExecuteStepRequest.isUseOptimizedTfPlan());
      }
    } else {
      if (terraformExecuteStepRequest.getEncryptedTfPlan() == null) {
        TerraformDestroyCommandRequest terraformDestroyCommandRequest =
            TerraformDestroyCommandRequest.builder()
                .varFilePaths(terraformExecuteStepRequest.getTfVarFilePaths())
                .varParams(terraformExecuteStepRequest.getVarParams())
                .uiLogs(terraformExecuteStepRequest.getUiLogs())
                .targets(terraformExecuteStepRequest.getTargets())
                .build();
        response = terraformClient.destroy(terraformDestroyCommandRequest,
            terraformExecuteStepRequest.getTimeoutInMillis(), terraformExecuteStepRequest.getEnvVars(),
            terraformExecuteStepRequest.getScriptDirectory(), terraformExecuteStepRequest.getLogCallback());
      } else {
        saveTerraformPlanContentToFile(terraformExecuteStepRequest.getEncryptionConfig(),
            terraformExecuteStepRequest.getEncryptedTfPlan(), terraformExecuteStepRequest.getScriptDirectory(),
            terraformExecuteStepRequest.getAccountId(), TERRAFORM_DESTROY_PLAN_FILE_OUTPUT_NAME);
        TerraformApplyCommandRequest terraformApplyCommandRequest =
            TerraformApplyCommandRequest.builder().planName(TERRAFORM_DESTROY_PLAN_FILE_OUTPUT_NAME).build();
        response = terraformClient.apply(terraformApplyCommandRequest, terraformExecuteStepRequest.getTimeoutInMillis(),
            terraformExecuteStepRequest.getEnvVars(), terraformExecuteStepRequest.getScriptDirectory(),
            terraformExecuteStepRequest.getLogCallback());
      }
    }
    return response;
  }

  private CliResponse executeTerraformShowCommandWithTfClient(TerraformCommand terraformCommand, long timeoutInMillis,
      Map<String, String> envVars, String scriptDirectory, LogCallback logCallback,
      PlanJsonLogOutputStream planJsonLogOutputStream, boolean useOptimizedTfPlan)
      throws IOException, InterruptedException, TimeoutException {
    String planName =
        terraformCommand == APPLY ? TERRAFORM_PLAN_FILE_OUTPUT_NAME : TERRAFORM_DESTROY_PLAN_FILE_OUTPUT_NAME;
    logCallback.saveExecutionLog(
        format("%nGenerating json representation of %s %n", planName), INFO, CommandExecutionStatus.RUNNING);
    CliResponse response =
        terraformClient.show(planName, timeoutInMillis, envVars, scriptDirectory, logCallback, planJsonLogOutputStream);
    if (!useOptimizedTfPlan) {
      logCallback.saveExecutionLog(
          format("%nJson representation of %s is exported as a variable %s %n", planName,
              terraformCommand == APPLY ? TERRAFORM_APPLY_PLAN_FILE_VAR_NAME : TERRAFORM_DESTROY_PLAN_FILE_VAR_NAME),
          INFO, CommandExecutionStatus.RUNNING);
    }

    return response;
  }

  @VisibleForTesting
  public void saveTerraformPlanContentToFile(EncryptionConfig encryptionConfig, EncryptedRecordData encryptedTfPlan,
      String scriptDirectory, String accountId, String terraformOutputFileName) throws IOException {
    File tfPlanFile = Paths.get(scriptDirectory, terraformOutputFileName).toFile();
    byte[] decryptedTerraformPlan =
        encryptDecryptHelper.getDecryptedContent(encryptionConfig, encryptedTfPlan, accountId);
    FileUtils.copyInputStreamToFile(new ByteArrayInputStream(decryptedTerraformPlan), tfPlanFile);
  }

  public EncryptedRecordData encryptPlan(byte[] content, String planName, EncryptionConfig encryptionConfig) {
    return (EncryptedRecordData) encryptDecryptHelper.encryptContent(content, planName, encryptionConfig);
  }

  @NotNull
  public String getPlanName(TerraformCommand command) {
    switch (command) {
      case APPLY:
        return TERRAFORM_PLAN_FILE_OUTPUT_NAME;
      case DESTROY:
        return TERRAFORM_DESTROY_PLAN_FILE_OUTPUT_NAME;
      default:
        throw new IllegalArgumentException("Invalid Terraform Command : " + command.toString());
    }
  }

  @NonNull
  public String resolveBaseDir(String accountId, String provisionerId) {
    return TF_BASE_DIR.replace("${ACCOUNT_ID}", accountId).replace("${ENTITY_ID}", provisionerId);
  }

  public String resolveScriptDirectory(String workingDir, String scriptPath) {
    return Paths
        .get(Paths.get(System.getProperty(USER_DIR_KEY)).toString(), workingDir, scriptPath == null ? "" : scriptPath)
        .toString();
  }

  public String getLatestCommitSHA(File repoDir) {
    if (repoDir.exists()) {
      try (Git git = Git.open(repoDir)) {
        Iterator<RevCommit> commits = git.log().call().iterator();
        if (commits.hasNext()) {
          RevCommit firstCommit = commits.next();

          return firstCommit.toString().split(" ")[1];
        }
      } catch (IOException | GitAPIException e) {
        log.error("Failed to extract the commit id from the cloned repo.", e);
      }
    }
    return null;
  }

  public GitBaseRequest getGitBaseRequestForConfigFile(
      String accountId, GitStoreDelegateConfig confileFileGitStore, GitConfigDTO configFileGitConfigDTO) {
    secretDecryptionService.decrypt(configFileGitConfigDTO.getGitAuth(), confileFileGitStore.getEncryptedDataDetails());

    SshSessionConfig sshSessionConfig = null;
    if (configFileGitConfigDTO.getGitAuthType() == SSH) {
      sshSessionConfig = getSshSessionConfig(confileFileGitStore);
    }

    return GitBaseRequest.builder()
        .branch(confileFileGitStore.getBranch())
        .commitId(confileFileGitStore.getCommitId())
        .repoUrl(configFileGitConfigDTO.getUrl())
        .repoType(GitRepositoryType.TERRAFORM)
        .authRequest(
            ngGitService.getAuthRequest((GitConfigDTO) confileFileGitStore.getGitConfigDTO(), sshSessionConfig))
        .accountId(accountId)
        .connectorId(confileFileGitStore.getConnectorName())
        .build();
  }

  public Map<String, String> buildcommitIdToFetchedFilesMap(String accountId, String configFileIdentifier,
      GitBaseRequest gitBaseRequestForConfigFile, List<TerraformVarFileInfo> varFileInfo) {
    Map<String, String> commitIdForConfigFilesMap = new HashMap<>();
    // Config File
    commitIdForConfigFilesMap.put(configFileIdentifier, getLatestCommitSHAFromLocalRepo(gitBaseRequestForConfigFile));
    // Add remote var files
    if (isNotEmpty(varFileInfo)) {
      addVarFilescommitIdstoMap(accountId, varFileInfo, commitIdForConfigFilesMap);
    }
    return commitIdForConfigFilesMap;
  }

  private void addVarFilescommitIdstoMap(
      String accountId, List<TerraformVarFileInfo> varFileInfo, Map<String, String> commitIdForConfigFilesMap) {
    for (TerraformVarFileInfo varFile : varFileInfo) {
      if (varFile instanceof RemoteTerraformVarFileInfo) {
        GitFetchFilesConfig gitFetchFilesConfig = ((RemoteTerraformVarFileInfo) varFile).getGitFetchFilesConfig();
        GitStoreDelegateConfig gitStoreDelegateConfig = gitFetchFilesConfig.getGitStoreDelegateConfig();
        GitConfigDTO gitConfigDTO = (GitConfigDTO) gitStoreDelegateConfig.getGitConfigDTO();

        SshSessionConfig sshSessionConfig = null;
        if (gitConfigDTO.getGitAuthType() == SSH) {
          sshSessionConfig = getSshSessionConfig(gitStoreDelegateConfig);
        }

        GitBaseRequest gitBaseRequest =
            GitBaseRequest.builder()
                .branch(gitStoreDelegateConfig.getBranch())
                .commitId(gitStoreDelegateConfig.getCommitId())
                .repoUrl(gitConfigDTO.getUrl())
                .connectorId(gitStoreDelegateConfig.getConnectorName())
                .authRequest(ngGitService.getAuthRequest(
                    (GitConfigDTO) gitStoreDelegateConfig.getGitConfigDTO(), sshSessionConfig))
                .accountId(accountId)
                .repoType(GitRepositoryType.TERRAFORM)
                .build();
        commitIdForConfigFilesMap.putIfAbsent(
            gitFetchFilesConfig.getIdentifier(), getLatestCommitSHAFromLocalRepo(gitBaseRequest));
      }
    }
  }

  public String getLatestCommitSHAFromLocalRepo(GitBaseRequest gitBaseRequest) {
    return getLatestCommitSHA(new File(gitClientHelper.getRepoDirectory(gitBaseRequest)));
  }

  public String fetchConfigFileAndPrepareScriptDir(GitBaseRequest gitBaseRequestForConfigFile, String accountId,
      String workspace, String currentStateFileId, GitStoreDelegateConfig confileFileGitStore, LogCallback logCallback,
      String scriptPath, String baseDir) {
    fetchConfigFileAndCloneLocally(gitBaseRequestForConfigFile, logCallback);

    String workingDir = getWorkingDir(baseDir);

    copyConfigFilestoWorkingDirectory(logCallback, gitBaseRequestForConfigFile, baseDir, workingDir);

    String scriptDirectory = resolveScriptDirectory(workingDir, scriptPath);
    log.info("Script Directory: " + scriptDirectory);
    logCallback.saveExecutionLog(
        format("Script Directory: [%s]", scriptDirectory), INFO, CommandExecutionStatus.RUNNING);

    try {
      TerraformHelperUtils.ensureLocalCleanup(scriptDirectory);
      downloadTfStateFile(workspace, accountId, currentStateFileId, scriptDirectory);
    } catch (IOException ioException) {
      log.warn("Exception Occurred when cleaning Terraform local directory", ioException);
    }
    return scriptDirectory;
  }

  public String getWorkingDir(String baseDir) {
    return Paths.get(baseDir, TF_SCRIPT_DIR).toString();
  }

  public String getBaseDir(String entityId) {
    return TF_WORKING_DIR + entityId;
  }

  public void fetchConfigFileAndCloneLocally(GitBaseRequest gitBaseRequestForConfigFile, LogCallback logCallback) {
    try {
      gitClient.ensureRepoLocallyClonedAndUpdated(gitBaseRequestForConfigFile);
    } catch (RuntimeException ex) {
      String msg = isNotEmpty(ex.getMessage()) ? format("Failed performing git operation. Reason: %s", ex.getMessage())
                                               : "Failed performing git operation.";
      logCallback.saveExecutionLog(msg, ERROR, CommandExecutionStatus.RUNNING);
      throw new JGitRuntimeException(msg, ex.getCause(), DEFAULT_ERROR_CODE, gitBaseRequestForConfigFile.getCommitId(),
          gitBaseRequestForConfigFile.getBranch());
    }
  }

  public String uploadTfStateFile(String accountId, String delegateId, String taskId, String entityId, File tfStateFile)
      throws IOException {
    final DelegateFile delegateFile = aDelegateFile()
                                          .withAccountId(accountId)
                                          .withDelegateId(delegateId)
                                          .withTaskId(taskId)
                                          .withEntityId(entityId)
                                          .withBucket(FileBucket.TERRAFORM_STATE)
                                          .withFileName(TERRAFORM_STATE_FILE_NAME)
                                          .build();

    if (tfStateFile != null) {
      try (InputStream initialStream = new FileInputStream(tfStateFile)) {
        delegateFileManager.upload(delegateFile, initialStream);
      }
    } else {
      try (InputStream nullInputStream = new NullInputStream(0)) {
        delegateFileManager.upload(delegateFile, nullInputStream);
      }
    }
    return delegateFile.getFileId();
  }

  public void copyConfigFilestoWorkingDirectory(
      LogCallback logCallback, GitBaseRequest gitBaseRequestForConfigFile, String baseDir, String workingDir) {
    try {
      TerraformHelperUtils.copyFilesToWorkingDirectory(
          gitClientHelper.getRepoDirectory(gitBaseRequestForConfigFile), workingDir);
    } catch (Exception ex) {
      log.error(String.format("Exception in copying files to provisioner specific directory", ex.getMessage()), ex);
      FileUtils.deleteQuietly(new File(baseDir));
      logCallback.saveExecutionLog(
          "Failed copying files to provisioner specific directory", ERROR, CommandExecutionStatus.RUNNING);
      throw new TerraformCommandExecutionException(
          "Error encountered when copying files to provisioner specific directory", WingsException.USER);
    }
  }

  public List<String> checkoutRemoteVarFileAndConvertToVarFilePaths(List<TerraformVarFileInfo> varFileInfo,
      String scriptDir, LogCallback logCallback, String accountId, String tfVarDirectory) throws IOException {
    if (EmptyPredicate.isNotEmpty(varFileInfo)) {
      List<String> varFilePaths = new ArrayList<>();
      Path tfVarDirAbsPath = Paths.get(tfVarDirectory).toAbsolutePath();
      for (TerraformVarFileInfo varFile : varFileInfo) {
        if (varFile instanceof InlineTerraformVarFileInfo) {
          varFilePaths.add(TerraformHelperUtils.createFileFromStringContent(
              ((InlineTerraformVarFileInfo) varFile).getVarFileContent(), scriptDir, TERRAFORM_VARIABLES_FILE_NAME));
        } else if (varFile instanceof RemoteTerraformVarFileInfo) {
          GitStoreDelegateConfig gitStoreDelegateConfig =
              ((RemoteTerraformVarFileInfo) varFile).getGitFetchFilesConfig().getGitStoreDelegateConfig();
          GitConfigDTO gitConfigDTO = (GitConfigDTO) gitStoreDelegateConfig.getGitConfigDTO();
          if (EmptyPredicate.isNotEmpty(gitStoreDelegateConfig.getPaths())) {
            logCallback.saveExecutionLog(format("Fetching Var files from Git repository: [%s]", gitConfigDTO.getUrl()),
                INFO, CommandExecutionStatus.RUNNING);

            secretDecryptionService.decrypt(
                gitConfigDTO.getGitAuth(), gitStoreDelegateConfig.getEncryptedDataDetails());

            SshSessionConfig sshSessionConfig = null;
            if (gitConfigDTO.getGitAuthType() == SSH) {
              sshSessionConfig = getSshSessionConfig(gitStoreDelegateConfig);
            }

            gitClient.downloadFiles(DownloadFilesRequest.builder()
                                        .branch(gitStoreDelegateConfig.getBranch())
                                        .commitId(gitStoreDelegateConfig.getCommitId())
                                        .filePaths(gitStoreDelegateConfig.getPaths())
                                        .connectorId(gitStoreDelegateConfig.getConnectorName())
                                        .repoUrl(gitConfigDTO.getUrl())
                                        .accountId(accountId)
                                        .recursive(true)
                                        .authRequest(ngGitService.getAuthRequest(gitConfigDTO, sshSessionConfig))
                                        .repoType(GitRepositoryType.TERRAFORM)
                                        .destinationDirectory(tfVarDirectory)
                                        .build());

            // One remote file can have multiple different var file paths provided
            // Combine them here and add to list.
            for (String paths : gitStoreDelegateConfig.getPaths()) {
              varFilePaths.add(tfVarDirAbsPath + "/" + paths);
            }
          }
        }
      }
      logCallback.saveExecutionLog(
          format("Var File directory: [%s]", tfVarDirAbsPath, INFO, CommandExecutionStatus.RUNNING));
      return varFilePaths;
    }
    return Collections.emptyList();
  }

  private SshSessionConfig getSshSessionConfig(GitStoreDelegateConfig gitStoreDelegateConfig) {
    if (gitStoreDelegateConfig.getSshKeySpecDTO() == null) {
      throw new InvalidRequestException(
          format("SSHKeySpecDTO is null for connector %s", gitStoreDelegateConfig.getConnectorName()));
    }
    return sshSessionConfigMapper.getSSHSessionConfig(
        gitStoreDelegateConfig.getSshKeySpecDTO(), gitStoreDelegateConfig.getEncryptedDataDetails());
  }

  public void performCleanupOfTfDirs(TerraformTaskNGParameters parameters, LogCallback logCallback) {
    {
      FileUtils.deleteQuietly(new File(getBaseDir(parameters.getEntityId())));
      if (parameters.getEncryptedTfPlan() != null) {
        try {
          boolean isSafelyDeleted = encryptDecryptHelper.deleteEncryptedRecord(
              parameters.getEncryptionConfig(), parameters.getEncryptedTfPlan());
          if (isSafelyDeleted) {
            log.info("Terraform Plan has been safely deleted from vault");
          }
        } catch (Exception ex) {
          logCallback.saveExecutionLog(
              color(format("Failed to delete secret: [%s] from vault: [%s], please clean it up",
                        parameters.getEncryptedTfPlan().getEncryptionKey(), parameters.getEncryptionConfig().getName()),
                  LogColor.Yellow, LogWeight.Bold),
              WARN, CommandExecutionStatus.RUNNING);
          logCallback.saveExecutionLog(ex.getMessage(), WARN);
          log.error("Exception occurred while deleting Terraform Plan from vault", ex);
        }
      }
      logCallback.saveExecutionLog("Done cleaning up directories.", INFO, CommandExecutionStatus.SUCCESS);
    }
  }

  @Override
  public String uploadTfPlanJson(String accountId, String delegateId, String taskId, String entityId, String planName,
      String localFilePath) throws IOException {
    final DelegateFile delegateFile = aDelegateFile()
                                          .withAccountId(accountId)
                                          .withDelegateId(delegateId)
                                          .withTaskId(taskId)
                                          .withEntityId(entityId)
                                          .withBucket(FileBucket.TERRAFORM_PLAN_JSON)
                                          .withFileName(format(TERRAFORM_PLAN_JSON_FILE_NAME, planName))
                                          .build();

    try (InputStream fileStream = new FileInputStream(localFilePath)) {
      delegateFileManagerBase.upload(delegateFile, fileStream);
    }

    return delegateFile.getFileId();
  }
}
