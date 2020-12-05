package software.wings.delegatetasks;

import static io.harness.delegate.beans.TaskData.DEFAULT_ASYNC_CALL_TIMEOUT;
import static io.harness.rule.OwnerRule.BOJANA;
import static io.harness.rule.OwnerRule.SATYAM;
import static io.harness.rule.OwnerRule.VAIBHAV_SI;

import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.COMMIT_REFERENCE;
import static software.wings.utils.WingsTestConstants.ENTITY_ID;
import static software.wings.utils.WingsTestConstants.SOURCE_REPO_SETTINGS_ID;
import static software.wings.utils.WingsTestConstants.WORKSPACE;

import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.atLeastOnce;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.FileData;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.DelegateFile;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.service.DelegateAgentFileService;
import io.harness.filesystem.FileIo;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.EncryptedRecordData;

import software.wings.WingsBaseTest;
import software.wings.api.TerraformExecutionData;
import software.wings.beans.GitConfig;
import software.wings.beans.GitOperationContext;
import software.wings.beans.delegation.TerraformProvisionParameters;
import software.wings.service.impl.yaml.GitClientHelper;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.yaml.GitClient;
import software.wings.utils.WingsTestConstants;

import com.google.common.collect.ImmutableMap;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.zeroturnaround.exec.stream.LogOutputStream;

@TargetModule(Module._930_DELEGATE_TASKS)
public class TerraformProvisionTaskTest extends WingsBaseTest {
  @Mock private EncryptionService mockEncryptionService;
  @Mock private GitClient gitClient;
  @Mock private DelegateLogService logService;
  @Mock private GitClientHelper gitClientHelper;
  @Mock private DelegateFileManager delegateFileManager;

  private static final String GIT_BRANCH = "test/git_branch";
  private static final String GIT_REPO_DIRECTORY = "repository/terraformTest";

  private GitConfig gitConfig;
  private Map<String, EncryptedDataDetail> encryptedBackendConfigs;
  private Map<String, EncryptedDataDetail> encryptedEnvironmentVariables;
  private EncryptedDataDetail encryptedDataDetail;
  private List<EncryptedDataDetail> sourceRepoEncryptionDetails;

  TerraformProvisionTask terraformProvisionTask =
      new TerraformProvisionTask(DelegateTaskPackage.builder()
                                     .delegateId(WingsTestConstants.DELEGATE_ID)
                                     .data(TaskData.builder().async(true).timeout(DEFAULT_ASYNC_CALL_TIMEOUT).build())
                                     .build(),
          null, delegateTaskResponse -> {}, () -> true);

  private TerraformProvisionTask terraformProvisionTaskSpy;

  @Before
  public void setUp() throws Exception {
    on(terraformProvisionTask).set("encryptionService", mockEncryptionService);
    on(terraformProvisionTask).set("gitClient", gitClient);
    on(terraformProvisionTask).set("logService", logService);
    on(terraformProvisionTask).set("gitClientHelper", gitClientHelper);
    on(terraformProvisionTask).set("delegateFileManager", delegateFileManager);

    gitConfig = GitConfig.builder().branch(GIT_BRANCH).build();
    gitConfig.setReference(COMMIT_REFERENCE);

    encryptedBackendConfigs = new HashMap<>();
    encryptedDataDetail = EncryptedDataDetail.builder()
                              .encryptedData(EncryptedRecordData.builder().uuid(WingsTestConstants.UUID).build())
                              .build();
    encryptedBackendConfigs.put("var2", encryptedDataDetail);

    encryptedEnvironmentVariables = new HashMap<>();
    encryptedEnvironmentVariables.put("key", encryptedDataDetail);

    sourceRepoEncryptionDetails = new ArrayList<>();
    sourceRepoEncryptionDetails.add(EncryptedDataDetail.builder().build());

    doReturn(GIT_REPO_DIRECTORY).when(gitClientHelper).getRepoDirectory(any(GitOperationContext.class));

    terraformProvisionTaskSpy = spy(terraformProvisionTask);

    doReturn(0)
        .when(terraformProvisionTaskSpy)
        .executeShellCommand(
            anyString(), anyString(), any(TerraformProvisionParameters.class), anyMap(), any(LogOutputStream.class));
    doReturn("latestCommit")
        .when(terraformProvisionTaskSpy)
        .getLatestCommitSHAFromLocalRepo(any(GitOperationContext.class));
    doReturn(new ArrayList<String>()).when(terraformProvisionTaskSpy).getWorkspacesList(anyString(), anyLong());
    doReturn(new char[] {'v', 'a', 'l', '2'}).when(mockEncryptionService).getDecryptedValue(encryptedDataDetail, false);

    when(delegateFileManager.upload(any(DelegateFile.class), any(InputStream.class))).thenReturn(new DelegateFile());
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void getTargetsArgsTest() {
    assertThat(terraformProvisionTask.getTargetArgs(null)).isEqualTo("");
    assertThat(terraformProvisionTask.getTargetArgs(Collections.EMPTY_LIST)).isEqualTo("");

    List<String> targets = new ArrayList<>(Arrays.asList("target1", "target2"));

    assertThat(terraformProvisionTask.getTargetArgs(targets)).isEqualTo("-target=target1 -target=target2 ");
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testParseOutput() {
    String workspaceCommandOutput = "* w1\n  w2\n w3";
    assertThat(Arrays.asList("w1", "w2", "w3").equals(terraformProvisionTask.parseOutput(workspaceCommandOutput)))
        .isTrue();
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testGenerateInlineCommandVars() throws Exception {
    doReturn(new char[] {'v', '2'}).when(mockEncryptionService).getDecryptedValue(any(), eq(false));
    TerraformProvisionParameters parameters =
        TerraformProvisionParameters.builder()
            .variables(ImmutableMap.of("k1", "v1"))
            .encryptedVariables(ImmutableMap.of("k2", EncryptedDataDetail.builder().build()))
            .build();
    StringBuilder inlineCommandBuffer = new StringBuilder();
    StringBuilder inlineUILogBuffer = new StringBuilder();
    terraformProvisionTask.getCommandLineVariableParams(parameters, null, inlineCommandBuffer, inlineUILogBuffer);
    String varParams = inlineCommandBuffer.toString();
    String uiLogs = inlineUILogBuffer.toString();
    assertThat(varParams).isEqualTo(" -var='k1=v1'  -var='k2=v2' ");
    assertThat(uiLogs).isEqualTo(" -var='k1=v1'  -var='k2=HarnessSecret:[k2]' ");
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void testRunApply() throws IOException, TimeoutException, InterruptedException {
    setupForApply();

    // regular apply
    TerraformProvisionParameters terraformProvisionParameters =
        createTerraformProvisionParameters(false, false, null, TerraformProvisionParameters.TerraformCommandUnit.Apply,
            TerraformProvisionParameters.TerraformCommand.APPLY, false, false);
    TerraformExecutionData terraformExecutionData = terraformProvisionTaskSpy.run(terraformProvisionParameters);
    verify(terraformExecutionData, TerraformProvisionParameters.TerraformCommand.APPLY);
  }

  private void setupForApply() throws IOException {
    FileIo.createDirectoryIfDoesNotExist(GIT_REPO_DIRECTORY.concat("/scriptPath"));
    FileIo.writeFile(GIT_REPO_DIRECTORY.concat("/scriptPath/backend_configs-ENTITY_ID"), new byte[] {});
    FileIo.writeFile(GIT_REPO_DIRECTORY.concat("/scriptPath/terraform-ENTITY_ID.tfvars"), new byte[] {});
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void TC1_testApplyUsingApprovedPlan() throws IOException, TimeoutException, InterruptedException {
    setupForApply();

    byte[] terraformPlan = "terraformPlan".getBytes();
    TerraformProvisionParameters terraformProvisionParameters = createTerraformProvisionParameters(false, false,
        terraformPlan, TerraformProvisionParameters.TerraformCommandUnit.Apply,
        TerraformProvisionParameters.TerraformCommand.APPLY, false, false);
    TerraformExecutionData terraformExecutionData = terraformProvisionTaskSpy.run(terraformProvisionParameters);
    verifyCommandExecuted(
        "terraform init", "terraform workspace", "terraform refresh", "terraform apply", "terraform output");
    verify(terraformExecutionData, TerraformProvisionParameters.TerraformCommand.APPLY);
  }

  /**
   * should skip refresh
   */
  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void TC2_testApplyUsingApprovedPlan() throws IOException, TimeoutException, InterruptedException {
    setupForApply();

    byte[] terraformPlan = "terraformPlan".getBytes();
    TerraformProvisionParameters terraformProvisionParameters = createTerraformProvisionParameters(false, false,
        terraformPlan, TerraformProvisionParameters.TerraformCommandUnit.Apply,
        TerraformProvisionParameters.TerraformCommand.APPLY, false, true);
    TerraformExecutionData terraformExecutionData = terraformProvisionTaskSpy.run(terraformProvisionParameters);
    verifyCommandExecuted("terraform init", "terraform workspace", "terraform apply", "terraform output");
    verify(terraformExecutionData, TerraformProvisionParameters.TerraformCommand.APPLY);
  }

  /**
   *  should not skip refresh since not using approved plan
   */
  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void TC1_testPlanAndExport() throws IOException, TimeoutException, InterruptedException {
    setupForApply();
    // run plan only and execute terraform show command
    byte[] terraformPlan = "terraformPlan".getBytes();
    TerraformProvisionParameters terraformProvisionParameters =
        createTerraformProvisionParameters(true, true, null, TerraformProvisionParameters.TerraformCommandUnit.Apply,
            TerraformProvisionParameters.TerraformCommand.APPLY, true, false);
    doReturn(terraformPlan)
        .when(terraformProvisionTaskSpy)
        .getTerraformPlanFile(anyString(), any(TerraformProvisionParameters.class));
    TerraformExecutionData terraformExecutionData = terraformProvisionTaskSpy.run(terraformProvisionParameters);
    verify(terraformExecutionData, TerraformProvisionParameters.TerraformCommand.APPLY);
    verifyCommandExecuted(
        "terraform init", "terraform workspace", "terraform refresh", "terraform plan", "terraform show");
    assertThat(terraformExecutionData.getTfPlanFile()).isEqualTo(terraformPlan);
  }

  /**
   *  should not skip refresh since not using approved plan
   */
  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void TC2_testApplyPlanAndExport() throws IOException, TimeoutException, InterruptedException {
    setupForApply();
    // run plan only and execute terraform show command
    byte[] terraformPlan = "terraformPlan".getBytes();
    TerraformProvisionParameters terraformProvisionParameters =
        createTerraformProvisionParameters(true, true, null, TerraformProvisionParameters.TerraformCommandUnit.Apply,
            TerraformProvisionParameters.TerraformCommand.APPLY, true, true);
    doReturn(terraformPlan)
        .when(terraformProvisionTaskSpy)
        .getTerraformPlanFile(anyString(), any(TerraformProvisionParameters.class));
    TerraformExecutionData terraformExecutionData = terraformProvisionTaskSpy.run(terraformProvisionParameters);
    verify(terraformExecutionData, TerraformProvisionParameters.TerraformCommand.APPLY);
    verifyCommandExecuted(
        "terraform init", "terraform workspace", "terraform refresh", "terraform plan", "terraform show");
    assertThat(terraformExecutionData.getTfPlanFile()).isEqualTo(terraformPlan);
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void testSaveAndGetTerraformPlanFile() throws IOException {
    String scriptDirectory = "repository/testSaveAndGetTerraformPlanFile";
    FileIo.createDirectoryIfDoesNotExist(scriptDirectory);
    byte[] planContent = "terraformPlanContent".getBytes();
    String workspacePath = "workspace";
    TerraformProvisionParameters terraformProvisionParameters =
        TerraformProvisionParameters.builder()
            .workspace(workspacePath)
            .terraformPlan(planContent)
            .command(TerraformProvisionParameters.TerraformCommand.APPLY)
            .build();
    terraformProvisionTask.saveTerraformPlanContentToFile(terraformProvisionParameters, scriptDirectory);
    List<FileData> fileDataList = FileIo.getFilesUnderPath(scriptDirectory);
    assertThat(fileDataList.size()).isEqualTo(1);
    assertThat(fileDataList.get(0).getFileBytes()).isEqualTo(planContent);

    byte[] retrievedTerraformPlanContent =
        terraformProvisionTask.getTerraformPlanFile(scriptDirectory, terraformProvisionParameters);
    assertThat(retrievedTerraformPlanContent).isEqualTo(planContent);

    FileIo.deleteDirectoryAndItsContentIfExists(scriptDirectory);
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void TC1_testRunDestroy() throws IOException, TimeoutException, InterruptedException {
    setupForDestroyTests();

    // regular destroy with no plan exported
    TerraformProvisionParameters terraformProvisionParameters = createTerraformProvisionParameters(false, false, null,
        TerraformProvisionParameters.TerraformCommandUnit.Destroy,
        TerraformProvisionParameters.TerraformCommand.DESTROY, false, false);
    TerraformExecutionData terraformExecutionData = terraformProvisionTaskSpy.run(terraformProvisionParameters);
    verify(terraformExecutionData, TerraformProvisionParameters.TerraformCommand.DESTROY);
    verifyCommandExecuted("terraform init", "terraform workspace", "terraform refresh", "terraform destroy");

    FileIo.deleteDirectoryAndItsContentIfExists(GIT_REPO_DIRECTORY);
    FileIo.deleteDirectoryAndItsContentIfExists("./terraform-working-dir");
  }

  /**
   * Should not skip refresh even because not using approved plan
   */
  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void TC2_testRunDestroy() throws IOException, TimeoutException, InterruptedException {
    setupForDestroyTests();

    // regular destroy with no plan exported
    TerraformProvisionParameters terraformProvisionParameters = createTerraformProvisionParameters(false, false, null,
        TerraformProvisionParameters.TerraformCommandUnit.Destroy,
        TerraformProvisionParameters.TerraformCommand.DESTROY, false, true);
    TerraformExecutionData terraformExecutionData = terraformProvisionTaskSpy.run(terraformProvisionParameters);
    verify(terraformExecutionData, TerraformProvisionParameters.TerraformCommand.DESTROY);
    verifyCommandExecuted("terraform init", "terraform workspace", "terraform refresh", "terraform destroy");

    FileIo.deleteDirectoryAndItsContentIfExists(GIT_REPO_DIRECTORY);
    FileIo.deleteDirectoryAndItsContentIfExists("./terraform-working-dir");
  }

  private void setupForDestroyTests() throws IOException {
    setupForApply();

    doReturn(new ByteArrayInputStream(new byte[] {}))
        .when(delegateFileManager)
        .downloadByFileId(any(DelegateAgentFileService.FileBucket.class), anyString(), anyString());
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void TC1_destroyUsingPlan() throws InterruptedException, TimeoutException, IOException {
    setupForDestroyTests();

    byte[] terraformDestroyPlan = "terraformDestroyPlan".getBytes();
    TerraformProvisionParameters terraformProvisionParameters = createTerraformProvisionParameters(false, false,
        terraformDestroyPlan, TerraformProvisionParameters.TerraformCommandUnit.Destroy,
        TerraformProvisionParameters.TerraformCommand.DESTROY, false, false);
    TerraformExecutionData terraformExecutionData = terraformProvisionTaskSpy.run(terraformProvisionParameters);
    verify(terraformExecutionData, TerraformProvisionParameters.TerraformCommand.DESTROY);
    verifyCommandExecuted("terraform init", "terraform workspace", "terraform refresh", "terraform apply");

    FileIo.deleteDirectoryAndItsContentIfExists(GIT_REPO_DIRECTORY);
    FileIo.deleteDirectoryAndItsContentIfExists("./terraform-working-dir");
  }

  /**
   * Skip Terraform Refresh when using approved plan
   */
  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void TC2_destroyUsingPlan() throws InterruptedException, TimeoutException, IOException {
    setupForDestroyTests();

    byte[] terraformDestroyPlan = "terraformDestroyPlan".getBytes();
    TerraformProvisionParameters terraformProvisionParameters = createTerraformProvisionParameters(false, false,
        terraformDestroyPlan, TerraformProvisionParameters.TerraformCommandUnit.Destroy,
        TerraformProvisionParameters.TerraformCommand.DESTROY, false, true);
    TerraformExecutionData terraformExecutionData = terraformProvisionTaskSpy.run(terraformProvisionParameters);
    verify(terraformExecutionData, TerraformProvisionParameters.TerraformCommand.DESTROY);
    verifyCommandExecuted("terraform init", "terraform workspace", "terraform apply");
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void TC1_destroyRunPlanOnly() throws InterruptedException, TimeoutException, IOException {
    setupForDestroyTests();

    byte[] terraformDestroyPlan = "terraformDestroyPlan".getBytes();
    TerraformProvisionParameters terraformProvisionParameters =
        createTerraformProvisionParameters(true, true, null, TerraformProvisionParameters.TerraformCommandUnit.Destroy,
            TerraformProvisionParameters.TerraformCommand.DESTROY, true, false);
    doReturn(terraformDestroyPlan)
        .when(terraformProvisionTaskSpy)
        .getTerraformPlanFile(anyString(), any(TerraformProvisionParameters.class));
    TerraformExecutionData terraformExecutionData = terraformProvisionTaskSpy.run(terraformProvisionParameters);
    verify(terraformExecutionData, TerraformProvisionParameters.TerraformCommand.DESTROY);
    assertThat(terraformExecutionData.getTfPlanFile()).isEqualTo(terraformDestroyPlan);
    verifyCommandExecuted(
        "terraform init", "terraform workspace", "terraform refresh", "terraform plan", "terraform show");
  }

  /**
   * Should not skip refresh even because not using approved plan
   */
  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void TC2_destroyRunPlanOnly() throws InterruptedException, TimeoutException, IOException {
    setupForDestroyTests();

    byte[] terraformDestroyPlan = "terraformDestroyPlan".getBytes();
    TerraformProvisionParameters terraformProvisionParameters =
        createTerraformProvisionParameters(true, true, null, TerraformProvisionParameters.TerraformCommandUnit.Destroy,
            TerraformProvisionParameters.TerraformCommand.DESTROY, true, true);
    doReturn(terraformDestroyPlan)
        .when(terraformProvisionTaskSpy)
        .getTerraformPlanFile(anyString(), any(TerraformProvisionParameters.class));
    TerraformExecutionData terraformExecutionData = terraformProvisionTaskSpy.run(terraformProvisionParameters);
    verify(terraformExecutionData, TerraformProvisionParameters.TerraformCommand.DESTROY);
    assertThat(terraformExecutionData.getTfPlanFile()).isEqualTo(terraformDestroyPlan);
    verifyCommandExecuted(
        "terraform init", "terraform workspace", "terraform refresh", "terraform plan", "terraform show");
  }

  private void verifyCommandExecuted(String... commands) throws IOException, InterruptedException, TimeoutException {
    ArgumentCaptor<String> listCaptor = ArgumentCaptor.forClass(String.class);
    Mockito.verify(terraformProvisionTaskSpy, atLeastOnce())
        .executeShellCommand(
            listCaptor.capture(), anyString(), any(TerraformProvisionParameters.class), any(Map.class), any());
    assertThat(listCaptor.getAllValues()
                   .stream()
                   .map(s -> "terraform " + s.split("terraform")[1])
                   .map(s -> s.split("\\s+"))
                   .map(s -> s[0] + " " + s[1])
                   .collect(Collectors.toList()))
        .containsExactly(commands);
  }

  private void verify(
      TerraformExecutionData terraformExecutionData, TerraformProvisionParameters.TerraformCommand command) {
    Mockito.verify(mockEncryptionService, times(1)).decrypt(gitConfig, sourceRepoEncryptionDetails, false);
    Mockito.verify(gitClient, times(1)).ensureRepoLocallyClonedAndUpdated(any(GitOperationContext.class));
    Mockito.verify(gitClientHelper, times(1)).getRepoDirectory(any(GitOperationContext.class));
    int uploadTimes = TerraformProvisionParameters.TerraformCommand.DESTROY.equals(command) ? 1 : 2;
    Mockito.verify(delegateFileManager, times(uploadTimes)).upload(any(DelegateFile.class), any(InputStream.class));
    assertThat(terraformExecutionData.getWorkspace()).isEqualTo(WORKSPACE);
    assertThat(terraformExecutionData.getEntityId()).isEqualTo(ENTITY_ID);
    assertThat(terraformExecutionData.getPlanLogFileId()).isEqualTo(null);
    assertThat(terraformExecutionData.getCommandExecuted()).isEqualTo(command);
    assertThat(terraformExecutionData.getSourceRepoReference()).isEqualTo("latestCommit");
    assertThat(terraformExecutionData.getExecutionStatus()).isEqualTo(ExecutionStatus.SUCCESS);
  }

  private TerraformProvisionParameters createTerraformProvisionParameters(boolean runPlanOnly,
      boolean exportPlanToApplyStep, byte[] terraformPlan,
      TerraformProvisionParameters.TerraformCommandUnit commandUnit,
      TerraformProvisionParameters.TerraformCommand command, boolean saveTerraformJson, boolean skipRefresh) {
    Map<String, String> backendConfigs = new HashMap<>();
    backendConfigs.put("var1", "value1");

    Map<String, String> variables = new HashMap<>();
    variables.put("var3", "val3");

    Map<String, String> environmentVariables = new HashMap<>();
    environmentVariables.put("TF_LOG", "TRACE");

    List<String> tfVarFiles = Arrays.asList("tfVarFile");

    return TerraformProvisionParameters.builder()
        .sourceRepo(gitConfig)
        .sourceRepoSettingId(SOURCE_REPO_SETTINGS_ID)
        .sourceRepoEncryptionDetails(sourceRepoEncryptionDetails)
        .scriptPath("scriptPath")
        .command(command)
        .commandUnit(commandUnit)
        .accountId(ACCOUNT_ID)
        .workspace(WORKSPACE)
        .entityId(ENTITY_ID)
        .backendConfigs(backendConfigs)
        .encryptedBackendConfigs(encryptedBackendConfigs)
        .terraformPlan(terraformPlan)
        .runPlanOnly(runPlanOnly)
        .exportPlanToApplyStep(exportPlanToApplyStep)
        .variables(variables)
        .environmentVariables(environmentVariables)
        .encryptedEnvironmentVariables(encryptedEnvironmentVariables)
        .tfVarFiles(tfVarFiles)
        .saveTerraformJson(saveTerraformJson)
        .skipRefreshBeforeApplyingPlan(skipRefresh)
        .build();
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void testSaveAndGetTerraformPlanFileWorkspaceEmpty() throws IOException {
    String scriptDirectory = "repository/testSaveAndGetTerraformPlanFileWorkspaceEmpty";
    FileIo.createDirectoryIfDoesNotExist(scriptDirectory);
    byte[] planContent = "terraformPlanContent".getBytes();
    TerraformProvisionParameters terraformProvisionParameters =
        TerraformProvisionParameters.builder()
            .terraformPlan(planContent)
            .command(TerraformProvisionParameters.TerraformCommand.APPLY)
            .build();
    terraformProvisionTask.saveTerraformPlanContentToFile(terraformProvisionParameters, scriptDirectory);
    List<FileData> fileDataList = FileIo.getFilesUnderPath(scriptDirectory);
    assertThat(fileDataList.size()).isEqualTo(1);
    assertThat(fileDataList.get(0).getFileBytes()).isEqualTo(planContent);

    TerraformProvisionParameters provisionParameters =
        TerraformProvisionParameters.builder().command(TerraformProvisionParameters.TerraformCommand.APPLY).build();
    byte[] retrievedTerraformPlanContent =
        terraformProvisionTask.getTerraformPlanFile(scriptDirectory, provisionParameters);
    assertThat(retrievedTerraformPlanContent).isEqualTo(planContent);

    FileIo.deleteDirectoryAndItsContentIfExists(scriptDirectory);
  }
}
