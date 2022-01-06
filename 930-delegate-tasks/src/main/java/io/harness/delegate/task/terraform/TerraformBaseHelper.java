/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.task.terraform;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cli.CliResponse;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.storeconfig.GitStoreDelegateConfig;
import io.harness.git.model.GitBaseRequest;
import io.harness.logging.LogCallback;
import io.harness.security.encryption.EncryptedRecordData;
import io.harness.security.encryption.EncryptionConfig;
import io.harness.terraform.request.TerraformExecuteStepRequest;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@OwnedBy(CDP)
public interface TerraformBaseHelper {
  void downloadTfStateFile(String workspace, String accountId, String currentStateFileId, String scriptDirectory)
      throws IOException;
  List<String> parseOutput(String workspaceOutput);

  CliResponse executeTerraformApplyStep(TerraformExecuteStepRequest terraformExecuteStepRequest)
      throws InterruptedException, IOException, TimeoutException;

  CliResponse executeTerraformPlanStep(TerraformExecuteStepRequest terraformExecuteStepRequest)
      throws InterruptedException, IOException, TimeoutException;

  CliResponse executeTerraformDestroyStep(TerraformExecuteStepRequest terraformExecuteStepRequest)
      throws InterruptedException, IOException, TimeoutException;

  String resolveBaseDir(String accountId, String provisionerId);

  String resolveScriptDirectory(String workingDir, String scriptPath);

  String getLatestCommitSHA(File repoDir);

  GitBaseRequest getGitBaseRequestForConfigFile(
      String accountId, GitStoreDelegateConfig confileFileGitStore, GitConfigDTO configFileGitConfigDTO);

  Map<String, String> buildcommitIdToFetchedFilesMap(String accountId, String configFileIdentifier,
      GitBaseRequest gitBaseRequestForConfigFile, List<TerraformVarFileInfo> varFileInfo);

  String fetchConfigFileAndPrepareScriptDir(GitBaseRequest gitBaseRequestForConfigFile, String accountId,
      String workspace, String currentStateFileId, GitStoreDelegateConfig confileFileGitStore, LogCallback logCallback,
      String scriptPath, String workingDir);

  void fetchConfigFileAndCloneLocally(GitBaseRequest gitBaseRequestForConfigFile, LogCallback logCallback);

  String uploadTfStateFile(String accountId, String delegateId, String taskId, String entityId, File tfStateFile)
      throws IOException;

  void copyConfigFilestoWorkingDirectory(
      LogCallback logCallback, GitBaseRequest gitBaseRequestForConfigFile, String baseDir, String workingDir);

  List<String> checkoutRemoteVarFileAndConvertToVarFilePaths(List<TerraformVarFileInfo> varFileInfo, String scriptDir,
      LogCallback logCallback, String accountId, String tfVarDirectory) throws IOException;

  EncryptedRecordData encryptPlan(byte[] content, String planName, EncryptionConfig encryptionConfig);

  String getPlanName(TerraformCommand terraformCommand);

  void performCleanupOfTfDirs(TerraformTaskNGParameters parameters, LogCallback logCallback);

  String getBaseDir(String entityId);

  String uploadTfPlanJson(String accountId, String delegateId, String taskId, String entityId, String planName,
      String localFilePath) throws IOException;
}
