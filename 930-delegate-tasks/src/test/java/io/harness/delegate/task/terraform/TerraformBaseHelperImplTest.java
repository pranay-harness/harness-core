/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.task.terraform;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.provision.TerraformConstants.TERRAFORM_PLAN_FILE_OUTPUT_NAME;
import static io.harness.provision.TerraformConstants.TERRAFORM_PLAN_JSON_FILE_NAME;
import static io.harness.rule.OwnerRule.ABOSII;
import static io.harness.rule.OwnerRule.ROHITKARELIA;
import static io.harness.rule.OwnerRule.VAIBHAV_SI;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.FileData;
import io.harness.category.element.UnitTests;
import io.harness.cli.CliResponse;
import io.harness.connector.service.git.NGGitService;
import io.harness.connector.task.shell.SshSessionConfigMapper;
import io.harness.delegate.beans.DelegateFile;
import io.harness.delegate.beans.DelegateFileManagerBase;
import io.harness.delegate.beans.FileBucket;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.storeconfig.GitStoreDelegateConfig;
import io.harness.delegate.task.git.GitFetchFilesConfig;
import io.harness.filesystem.FileIo;
import io.harness.git.GitClientHelper;
import io.harness.git.GitClientV2;
import io.harness.git.model.GitBaseRequest;
import io.harness.logging.LogCallback;
import io.harness.logging.PlanJsonLogOutputStream;
import io.harness.ng.core.dto.secrets.SSHKeySpecDTO;
import io.harness.rule.Owner;
import io.harness.secretmanagerclient.EncryptDecryptHelper;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.EncryptedRecordData;
import io.harness.security.encryption.EncryptionConfig;
import io.harness.security.encryption.SecretDecryptionService;
import io.harness.terraform.TerraformClientImpl;
import io.harness.terraform.request.TerraformApplyCommandRequest;
import io.harness.terraform.request.TerraformDestroyCommandRequest;
import io.harness.terraform.request.TerraformExecuteStepRequest;
import io.harness.terraform.request.TerraformExecuteStepRequest.TerraformExecuteStepRequestBuilder;
import io.harness.terraform.request.TerraformInitCommandRequest;
import io.harness.terraform.request.TerraformPlanCommandRequest;
import io.harness.terraform.request.TerraformRefreshCommandRequest;

import com.google.inject.Inject;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

@OwnedBy(CDP)
public class TerraformBaseHelperImplTest extends CategoryTest {
  @InjectMocks @Inject TerraformBaseHelperImpl terraformBaseHelper;
  @Mock private LogCallback logCallback;
  @Mock private PlanJsonLogOutputStream planJsonLogOutputStream;
  @Mock private TerraformClientImpl terraformClient;
  private TerraformBaseHelperImpl spyTerraformBaseHelper;
  @Mock EncryptionConfig encryptionConfig;
  @Mock GitClientHelper gitClientHelper;
  @Mock SshSessionConfigMapper sshSessionConfigMapper;
  @Mock NGGitService ngGitService;
  @Mock SecretDecryptionService secretDecryptionService;
  @Mock GitClientV2 gitClient;
  @Mock private EncryptDecryptHelper encryptDecryptHelper;
  @Mock private DelegateFileManagerBase delegateFileManager;

  private final EncryptedRecordData encryptedPlanContent =
      EncryptedRecordData.builder().name("planName").encryptedValue("encryptedPlan".toCharArray()).build();

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    spyTerraformBaseHelper = spy(terraformBaseHelper);
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testParseOutput() {
    String workspaceCommandOutput = "* w1\n w2\n w3";
    assertThat(Arrays.asList("w1", "w2", "w3").equals(terraformBaseHelper.parseOutput(workspaceCommandOutput)))
        .isTrue();
  }

  @Test
  @Owner(developers = ROHITKARELIA)
  @Category(UnitTests.class)
  public void testexecuteTerraformApplyStep() throws InterruptedException, TimeoutException, IOException {
    TerraformExecuteStepRequest terraformExecuteStepRequest = getTerraformExecuteStepRequest().build();

    doReturn(Arrays.asList("w1")).when(spyTerraformBaseHelper).parseOutput("* w1\n");

    when(terraformClient.getWorkspaceList(terraformExecuteStepRequest.getTimeoutInMillis(),
             terraformExecuteStepRequest.getEnvVars(), terraformExecuteStepRequest.getScriptDirectory(),
             terraformExecuteStepRequest.getLogCallback()))
        .thenReturn(CliResponse.builder().output("workspace").build());

    spyTerraformBaseHelper.executeTerraformApplyStep(terraformExecuteStepRequest);

    Mockito.verify(terraformClient, times(1))
        .init(TerraformInitCommandRequest.builder()
                  .tfBackendConfigsFilePath(terraformExecuteStepRequest.getTfBackendConfigsFile())
                  .build(),
            terraformExecuteStepRequest.getTimeoutInMillis(), terraformExecuteStepRequest.getEnvVars(),
            terraformExecuteStepRequest.getScriptDirectory(), terraformExecuteStepRequest.getLogCallback());
    Mockito.verify(terraformClient, times(1))
        .getWorkspaceList(terraformExecuteStepRequest.getTimeoutInMillis(), terraformExecuteStepRequest.getEnvVars(),
            terraformExecuteStepRequest.getScriptDirectory(), terraformExecuteStepRequest.getLogCallback());
    Mockito.verify(terraformClient, times(1))
        .workspace(terraformExecuteStepRequest.getWorkspace(), true, terraformExecuteStepRequest.getTimeoutInMillis(),
            terraformExecuteStepRequest.getEnvVars(), terraformExecuteStepRequest.getScriptDirectory(),
            terraformExecuteStepRequest.getLogCallback());
    Mockito.verify(terraformClient, times(1))
        .apply(TerraformApplyCommandRequest.builder().planName("tfplan").build(),
            terraformExecuteStepRequest.getTimeoutInMillis(), terraformExecuteStepRequest.getEnvVars(),
            terraformExecuteStepRequest.getScriptDirectory(), terraformExecuteStepRequest.getLogCallback());
    Mockito.verify(terraformClient, times(1))
        .plan(TerraformPlanCommandRequest.builder().build(), terraformExecuteStepRequest.getTimeoutInMillis(),
            terraformExecuteStepRequest.getEnvVars(), terraformExecuteStepRequest.getScriptDirectory(),
            terraformExecuteStepRequest.getLogCallback());
  }

  @Test
  @Owner(developers = ROHITKARELIA)
  @Category(UnitTests.class)
  public void testexecuteTerraformPlanStep() throws InterruptedException, TimeoutException, IOException {
    TerraformExecuteStepRequest terraformExecuteStepRequest = getTerraformExecuteStepRequest().build();

    doReturn(Arrays.asList("w1")).when(spyTerraformBaseHelper).parseOutput("* w1\n");

    when(terraformClient.getWorkspaceList(terraformExecuteStepRequest.getTimeoutInMillis(),
             terraformExecuteStepRequest.getEnvVars(), terraformExecuteStepRequest.getScriptDirectory(),
             terraformExecuteStepRequest.getLogCallback()))
        .thenReturn(CliResponse.builder().output("workspace").build());

    terraformBaseHelper.executeTerraformPlanStep(terraformExecuteStepRequest);

    Mockito.verify(terraformClient, times(1))
        .init(TerraformInitCommandRequest.builder()
                  .tfBackendConfigsFilePath(terraformExecuteStepRequest.getTfBackendConfigsFile())
                  .build(),
            terraformExecuteStepRequest.getTimeoutInMillis(), terraformExecuteStepRequest.getEnvVars(),
            terraformExecuteStepRequest.getScriptDirectory(), terraformExecuteStepRequest.getLogCallback());

    Mockito.verify(terraformClient, times(1))
        .getWorkspaceList(terraformExecuteStepRequest.getTimeoutInMillis(), terraformExecuteStepRequest.getEnvVars(),
            terraformExecuteStepRequest.getScriptDirectory(), terraformExecuteStepRequest.getLogCallback());

    Mockito.verify(terraformClient, times(1))
        .workspace(terraformExecuteStepRequest.getWorkspace(), true, terraformExecuteStepRequest.getTimeoutInMillis(),
            terraformExecuteStepRequest.getEnvVars(), terraformExecuteStepRequest.getScriptDirectory(),
            terraformExecuteStepRequest.getLogCallback());

    Mockito.verify(terraformClient, times(1))
        .refresh(TerraformRefreshCommandRequest.builder().build(), terraformExecuteStepRequest.getTimeoutInMillis(),
            terraformExecuteStepRequest.getEnvVars(), terraformExecuteStepRequest.getScriptDirectory(),
            terraformExecuteStepRequest.getLogCallback());

    Mockito.verify(terraformClient, times(1))
        .plan(TerraformPlanCommandRequest.builder().build(), terraformExecuteStepRequest.getTimeoutInMillis(),
            terraformExecuteStepRequest.getEnvVars(), terraformExecuteStepRequest.getScriptDirectory(),
            terraformExecuteStepRequest.getLogCallback());
  }

  @Test
  @Owner(developers = ROHITKARELIA)
  @Category(UnitTests.class)
  public void testexecuteTerraformDestroyStep() throws InterruptedException, TimeoutException, IOException {
    TerraformExecuteStepRequest terraformExecuteStepRequest = getTerraformExecuteStepRequest().build();
    doReturn(Arrays.asList("w1")).when(spyTerraformBaseHelper).parseOutput("* w1\n");

    when(terraformClient.getWorkspaceList(terraformExecuteStepRequest.getTimeoutInMillis(),
             terraformExecuteStepRequest.getEnvVars(), terraformExecuteStepRequest.getScriptDirectory(),
             terraformExecuteStepRequest.getLogCallback()))
        .thenReturn(CliResponse.builder().output("workspace").build());

    terraformBaseHelper.executeTerraformDestroyStep(terraformExecuteStepRequest);

    Mockito.verify(terraformClient, times(1))
        .init(TerraformInitCommandRequest.builder()
                  .tfBackendConfigsFilePath(terraformExecuteStepRequest.getTfBackendConfigsFile())
                  .build(),
            terraformExecuteStepRequest.getTimeoutInMillis(), terraformExecuteStepRequest.getEnvVars(),
            terraformExecuteStepRequest.getScriptDirectory(), terraformExecuteStepRequest.getLogCallback());
    Mockito.verify(terraformClient, times(1))
        .getWorkspaceList(terraformExecuteStepRequest.getTimeoutInMillis(), terraformExecuteStepRequest.getEnvVars(),
            terraformExecuteStepRequest.getScriptDirectory(), terraformExecuteStepRequest.getLogCallback());
    Mockito.verify(terraformClient, times(1))
        .workspace(terraformExecuteStepRequest.getWorkspace(), true, terraformExecuteStepRequest.getTimeoutInMillis(),
            terraformExecuteStepRequest.getEnvVars(), terraformExecuteStepRequest.getScriptDirectory(),
            terraformExecuteStepRequest.getLogCallback());
    Mockito.verify(terraformClient, times(1))
        .destroy(TerraformDestroyCommandRequest.builder().targets(terraformExecuteStepRequest.getTargets()).build(),
            terraformExecuteStepRequest.getTimeoutInMillis(), terraformExecuteStepRequest.getEnvVars(),
            terraformExecuteStepRequest.getScriptDirectory(), terraformExecuteStepRequest.getLogCallback());
  }

  @Test
  @Owner(developers = ROHITKARELIA)
  @Category(UnitTests.class)
  public void testsaveTerraformPlanContentToFile() throws IOException {
    String scriptDirectory = "repository/testSaveAndGetTerraformPlanFile";
    FileIo.createDirectoryIfDoesNotExist(scriptDirectory);
    byte[] planContent = "terraformPlanContent".getBytes();

    doReturn(planContent).when(encryptDecryptHelper).getDecryptedContent(any(), any(), any());

    terraformBaseHelper.saveTerraformPlanContentToFile(
        encryptionConfig, encryptedPlanContent, scriptDirectory, "accountId", TERRAFORM_PLAN_FILE_OUTPUT_NAME);
    List<FileData> fileDataList = FileIo.getFilesUnderPath(scriptDirectory);
    assertThat(fileDataList.size()).isEqualTo(1);
    assertThat(fileDataList.get(0).getFileBytes()).isEqualTo(planContent);

    FileIo.deleteDirectoryAndItsContentIfExists(scriptDirectory);
  }

  @Test
  @Owner(developers = ROHITKARELIA)
  @Category(UnitTests.class)
  public void testbuildcommitIdToFetchedFilesMapHasConfigFileAndVarFileCommitInfo() {
    GitBaseRequest mockGitBaseRequest = mock(GitBaseRequest.class);
    EncryptedDataDetail encryptedDataDetail = mock(EncryptedDataDetail.class);
    List<EncryptedDataDetail> encryptedDataDetailList = new ArrayList<>();
    encryptedDataDetailList.add(encryptedDataDetail);

    doReturn("commitsha").when(spyTerraformBaseHelper).getLatestCommitSHA(new File("repoDir"));
    doReturn("repoDir").when(gitClientHelper).getRepoDirectory(any(GitBaseRequest.class));
    doReturn("commitsha").when(spyTerraformBaseHelper).getLatestCommitSHAFromLocalRepo(any(GitBaseRequest.class));

    Map<String, String> resultMap = terraformBaseHelper.buildcommitIdToFetchedFilesMap(
        "accountId", "configFileIdentifier", mockGitBaseRequest, getTerraformFileInfoList());
    assertThat(resultMap).isNotNull();
    assertThat(resultMap.size()).isGreaterThan(1); // should atleast have config file and it's identifier
  }
  @Test
  @Owner(developers = ROHITKARELIA)
  @Category(UnitTests.class)
  public void testCheckoutRemoteVarFileAndConvertToVarFilePaths() throws IOException {
    String scriptDirectory = "repository/testSaveAndGetTerraformPlanFile";
    FileIo.createDirectoryIfDoesNotExist(scriptDirectory);
    String tfvarDir = "repository/tfVarDir";
    FileIo.createDirectoryIfDoesNotExist(tfvarDir);

    List<String> varFilePaths = terraformBaseHelper.checkoutRemoteVarFileAndConvertToVarFilePaths(
        getTerraformFileInfoList(), scriptDirectory, logCallback, "accountId", tfvarDir);
    assertThat(varFilePaths.size()).isEqualTo(2);
    assertThat(varFilePaths.get(0))
        .isEqualTo(Paths.get(tfvarDir).toAbsolutePath() + "/"
            + "filepath1");
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testUploadTfPlanJson() throws IOException {
    final String fileId = "fileId";
    final String tfPlanJsonContent = "plan-json-content";
    final File tfPlanJsonFile = File.createTempFile("testFile", "tfPlanJson");

    try (FileWriter fileWriter = new FileWriter(tfPlanJsonFile)) {
      fileWriter.write(tfPlanJsonContent);
    }

    terraformBaseHelper.uploadTfPlanJson(
        "accountId", "delegateId", "taskId", "entityId", "planName", tfPlanJsonFile.getAbsolutePath());

    ArgumentCaptor<DelegateFile> delegateFileCaptor = ArgumentCaptor.forClass(DelegateFile.class);
    verify(delegateFileManager, times(1)).upload(delegateFileCaptor.capture(), any(InputStream.class));
    DelegateFile delegateFile = delegateFileCaptor.getValue();
    assertThat(delegateFile.getDelegateId()).isEqualTo("delegateId");
    assertThat(delegateFile.getAccountId()).isEqualTo("accountId");
    assertThat(delegateFile.getTaskId()).isEqualTo("taskId");
    assertThat(delegateFile.getBucket()).isEqualTo(FileBucket.TERRAFORM_PLAN_JSON);
    assertThat(delegateFile.getFileName()).isEqualTo(format(TERRAFORM_PLAN_JSON_FILE_NAME, "planName"));

    FileIo.deleteFileIfExists(tfPlanJsonFile.getAbsolutePath());
  }

  private List<TerraformVarFileInfo> getTerraformFileInfoList() {
    List<TerraformVarFileInfo> varFileInfos = new ArrayList<>();
    GitStoreDelegateConfig gitStoreDelegateConfig = getGitStoreDelegateConfig();
    RemoteTerraformVarFileInfo remoteTerraformVarFileInfo =
        RemoteTerraformVarFileInfo.builder()
            .gitFetchFilesConfig(GitFetchFilesConfig.builder().gitStoreDelegateConfig(gitStoreDelegateConfig).build())
            .build();

    varFileInfos.add(remoteTerraformVarFileInfo);

    return varFileInfos;
  }

  private GitStoreDelegateConfig getGitStoreDelegateConfig() {
    return GitStoreDelegateConfig.builder()
        .gitConfigDTO(GitConfigDTO.builder().url("repourl").build())
        .connectorName("connectorId")
        .paths(Arrays.asList("filepath1", "filepath2"))
        .branch("master")
        .sshKeySpecDTO(SSHKeySpecDTO.builder().build())
        .build();
  }

  private TerraformExecuteStepRequestBuilder getTerraformExecuteStepRequest() {
    return TerraformExecuteStepRequest.builder()
        .tfBackendConfigsFile("backendConfigFile.txt")
        .tfOutputsFile("outputfile.txt")
        .scriptDirectory("scriptDirectory")
        .envVars(new HashMap<>())
        .workspace("workspace")
        .isRunPlanOnly(false)
        .isSkipRefreshBeforeApplyingPlan(true)
        .isSaveTerraformJson(false)
        .logCallback(logCallback)
        .planJsonLogOutputStream(planJsonLogOutputStream);
  }
}
