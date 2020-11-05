package software.wings.sm.states.provision;

import static io.harness.beans.SweepingOutputInstance.Scope;
import static io.harness.beans.SweepingOutputInstance.builder;
import static io.harness.context.ContextElementType.TERRAFORM_INHERIT_PLAN;
import static io.harness.rule.OwnerRule.ABOSII;
import static io.harness.rule.OwnerRule.BOJANA;
import static io.harness.rule.OwnerRule.GEORGE;
import static io.harness.rule.OwnerRule.VAIBHAV_SI;
import static io.harness.rule.OwnerRule.YOGESH;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.beans.Environment.EnvironmentType.ALL;
import static software.wings.beans.Environment.GLOBAL_ENV_ID;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.ACTIVITY_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.ENV_NAME;
import static software.wings.utils.WingsTestConstants.PROVISIONER_ID;
import static software.wings.utils.WingsTestConstants.USER_EMAIL;
import static software.wings.utils.WingsTestConstants.USER_NAME;
import static software.wings.utils.WingsTestConstants.WORKFLOW_EXECUTION_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_NAME;

import com.google.common.collect.ImmutableMap;

import io.harness.beans.DelegateTask;
import io.harness.beans.EmbeddedUser;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.FileMetadata;
import io.harness.beans.OrchestrationWorkflowType;
import io.harness.beans.SweepingOutputInstance;
import io.harness.beans.WorkflowType;
import io.harness.category.element.UnitTests;
import io.harness.context.ContextElementType;
import io.harness.delegate.service.DelegateAgentFileService.FileBucket;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.stream.BoundedInputStream;
import io.harness.tasks.Cd1SetupFields;
import io.harness.tasks.ResponseData;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;
import org.mongodb.morphia.query.Query;
import software.wings.WingsBaseTest;
import software.wings.api.ScriptStateExecutionData;
import software.wings.api.TerraformExecutionData;
import software.wings.api.TerraformOutputInfoElement;
import software.wings.api.TerraformPlanParam;
import software.wings.api.terraform.TerraformProvisionInheritPlanElement;
import software.wings.beans.Activity;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.beans.FeatureName;
import software.wings.beans.GitConfig;
import software.wings.beans.InfrastructureProvisioner;
import software.wings.beans.NameValuePair;
import software.wings.beans.PhaseStep;
import software.wings.beans.TerraformInfrastructureProvisioner;
import software.wings.beans.delegation.TerraformProvisionParameters;
import software.wings.beans.infrastructure.TerraformConfig;
import software.wings.dl.WingsPersistence;
import software.wings.security.UsageRestrictions;
import software.wings.service.impl.GitConfigHelperService;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.InfrastructureProvisionerService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.service.intfc.sweepingoutput.SweepingOutputInquiry;
import software.wings.service.intfc.sweepingoutput.SweepingOutputService;
import software.wings.sm.ContextElement;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.StateExecutionContext;
import software.wings.sm.WorkflowStandardParams;
import software.wings.sm.states.ManagerExecutionLogCallback;
import software.wings.utils.GitUtilsManager;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Collector;

public class TerraformProvisionStateTest extends WingsBaseTest {
  @Mock InfrastructureProvisionerService infrastructureProvisionerService;
  @Mock private DelegateService delegateService;
  @Mock private ActivityService activityService;
  @Mock private GitUtilsManager gitUtilsManager;
  @Mock private FileService fileService;
  @Mock private SecretManager secretManager;
  @Mock private SweepingOutputService sweepingOutputService;
  @Mock private ExecutionContextImpl executionContext;
  @Mock private WingsPersistence wingsPersistence;
  @Mock private GitConfigHelperService gitConfigHelperService;
  @Mock private FeatureFlagService featureFlagService;
  @InjectMocks private TerraformProvisionState state = new ApplyTerraformProvisionState("tf");
  @InjectMocks private TerraformProvisionState destroyProvisionState = new DestroyTerraformProvisionState("tf");

  @Before
  public void setup() {
    BiFunction<String, Collector, Answer> extractVariablesOfType = (type, collector) -> {
      return invocation -> {
        List<NameValuePair> input = invocation.getArgumentAt(0, List.class);
        return input.stream().filter(value -> type.equals(value.getValueType())).collect(collector);
      };
    };
    Answer doExtractTextVariables =
        extractVariablesOfType.apply("TEXT", toMap(NameValuePair::getName, NameValuePair::getValue));
    Answer doExtractEncryptedVariables = extractVariablesOfType.apply("ENCRYPTED_TEXT",
        toMap(NameValuePair::getName, entry -> EncryptedDataDetail.builder().fieldName(entry.getName()).build()));
    Answer<String> doReturnSameValue = invocation -> invocation.getArgumentAt(0, String.class);

    doReturn(Activity.builder().uuid("uuid").build()).when(activityService).save(any(Activity.class));
    doAnswer(doExtractTextVariables)
        .when(infrastructureProvisionerService)
        .extractTextVariables(anyListOf(NameValuePair.class), any(ExecutionContext.class));
    doAnswer(doExtractTextVariables)
        .when(infrastructureProvisionerService)
        .extractUnresolvedTextVariables(anyListOf(NameValuePair.class));
    doAnswer(doExtractEncryptedVariables)
        .when(infrastructureProvisionerService)
        .extractEncryptedTextVariables(anyListOf(NameValuePair.class), anyString());
    doAnswer(doReturnSameValue).when(executionContext).renderExpression(anyString());
    doAnswer(doReturnSameValue).when(executionContext).renderExpression(anyString(), any(StateExecutionContext.class));
    doReturn(APP_ID).when(executionContext).getAppId();
    doReturn(Environment.Builder.anEnvironment().build()).when(executionContext).getEnv();
    doReturn(Application.Builder.anApplication().appId(APP_ID).build()).when(executionContext).getApp();
    doReturn(WorkflowStandardParams.Builder.aWorkflowStandardParams()
                 .withCurrentUser(EmbeddedUser.builder().name("name").build())
                 .build())
        .when(executionContext)
        .getContextElement(any(ContextElementType.class));

    doReturn(null).when(executionContext).getContextElement(ContextElementType.TERRAFORM_PROVISION);
    doReturn(SweepingOutputInquiry.builder()).when(executionContext).prepareSweepingOutputInquiryBuilder();
    doReturn(builder()).when(executionContext).prepareSweepingOutputBuilder(any(Scope.class));

    when(featureFlagService.isEnabled(eq(FeatureName.EXPORT_TF_PLAN), anyString())).thenReturn(true);
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldParseOutputs() throws IOException {
    assertThat(TerraformProvisionState.parseOutputs(null).size()).isEqualTo(0);
    assertThat(TerraformProvisionState.parseOutputs("").size()).isEqualTo(0);
    assertThat(TerraformProvisionState.parseOutputs("  ").size()).isEqualTo(0);

    ClassLoader classLoader = getClass().getClassLoader();
    File file = new File(classLoader.getResource("software/wings/sm/states/provision/terraform_output.json").getFile());
    String json = FileUtils.readFileToString(file, Charset.defaultCharset());

    final Map<String, Object> stringObjectMap = TerraformProvisionState.parseOutputs(json);
    assertThat(stringObjectMap.size()).isEqualTo(4);
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void shouldUpdateProvisionerWorkspaces() {
    when(infrastructureProvisionerService.update(any())).thenReturn(null);
    TerraformInfrastructureProvisioner provisioner = TerraformInfrastructureProvisioner.builder().build();
    state.updateProvisionerWorkspaces(provisioner, "w1");
    assertThat(provisioner.getWorkspaces().size() == 1 && provisioner.getWorkspaces().contains("w1")).isTrue();
    state.updateProvisionerWorkspaces(provisioner, "w2");
    assertThat(provisioner.getWorkspaces().size() == 2 && provisioner.getWorkspaces().equals(asList("w1", "w2")))
        .isTrue();
    state.updateProvisionerWorkspaces(provisioner, "w2");
    assertThat(provisioner.getWorkspaces().size() == 2 && provisioner.getWorkspaces().equals(asList("w1", "w2")))
        .isTrue();
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldHandleDefaultWorkspace() {
    assertThat(state.handleDefaultWorkspace(null) == null).isTrue();
    assertThat(state.handleDefaultWorkspace("default") == null).isTrue();
    assertThat(state.handleDefaultWorkspace("abc").equals("abc")).isTrue();
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testValidateAndFilterVariables() {
    NameValuePair prov_var_1 = NameValuePair.builder().name("access_key").valueType("TEXT").build();
    NameValuePair prov_var_2 = NameValuePair.builder().name("secret_key").valueType("TEXT").build();

    NameValuePair wf_var_1 = NameValuePair.builder().name("access_key").valueType("TEXT").value("value-1").build();
    NameValuePair wf_var_2 = NameValuePair.builder().name("secret_key").valueType("TEXT").value("value-2").build();
    NameValuePair wf_var_3 = NameValuePair.builder().name("region").valueType("TEXT").value("value-3").build();

    final List<NameValuePair> workflowVars = asList(wf_var_1, wf_var_2, wf_var_3);
    final List<NameValuePair> provVars = asList(prov_var_1, prov_var_2);

    List<NameValuePair> filteredVars_1 = TerraformProvisionState.validateAndFilterVariables(workflowVars, provVars);

    final List<NameValuePair> expected_1 = asList(wf_var_1, wf_var_2);
    assertThat(filteredVars_1).isEqualTo(expected_1);

    wf_var_1.setValueType("ENCRYPTED_TEXT");

    final List<NameValuePair> filteredVars_2 =
        TerraformProvisionState.validateAndFilterVariables(workflowVars, provVars);

    final List<NameValuePair> expected_2 = asList(wf_var_1, wf_var_2);
    assertThat(filteredVars_2).isEqualTo(expected_2);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testValidateAndFilterVariablesEmpty() {
    final List<NameValuePair> workflowVars =
        Collections.singletonList(NameValuePair.builder().name("key").valueType("TEXT").value("value").build());
    final List<NameValuePair> provVars = Collections.emptyList();

    assertThat(TerraformProvisionState.validateAndFilterVariables(workflowVars, provVars)).isEmpty();
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testStateTimeout() {
    testTimeoutInternal(new ApplyTerraformProvisionState("tf"));
    testTimeoutInternal(new AdjustTerraformProvisionState("tf"));
    testTimeoutInternal(new DestroyTerraformProvisionState("tf"));
    testTimeoutInternal(new TerraformRollbackState("tf"));
    testTimeoutInternal(new ApplyTerraformState("tf"));
  }

  private void testTimeoutInternal(TerraformProvisionState state) {
    state.setTimeoutMillis(null);
    assertThat(state.getTimeoutMillis()).isNull();

    state.setTimeoutMillis(500);
    assertThat(state.getTimeoutMillis()).isEqualTo(500);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testExecuteTerraformDestroyStateWithConfiguration() {
    destroyProvisionState.setVariables(getTerraformVariables());
    destroyProvisionState.setBackendConfigs(getTerraformBackendConfigs());
    destroyProvisionState.setProvisionerId(PROVISIONER_ID);
    TerraformInfrastructureProvisioner provisioner = TerraformInfrastructureProvisioner.builder()
                                                         .appId(APP_ID)
                                                         .path("current/working/directory")
                                                         .variables(getTerraformVariables())
                                                         .skipRefreshBeforeApplyingPlan(true)
                                                         .build();
    GitConfig gitConfig = GitConfig.builder().branch("master").build();

    doReturn(provisioner).when(infrastructureProvisionerService).get(APP_ID, PROVISIONER_ID);
    doReturn("taskId").when(delegateService).queueTask(any(DelegateTask.class));
    doReturn(gitConfig).when(gitUtilsManager).getGitConfig(anyString());
    ExecutionResponse response = destroyProvisionState.execute(executionContext);

    ArgumentCaptor<DelegateTask> taskCaptor = ArgumentCaptor.forClass(DelegateTask.class);
    verify(delegateService).queueTask(taskCaptor.capture());
    DelegateTask createdTask = taskCaptor.getValue();
    verify(gitConfigHelperService).convertToRepoGitConfig(any(GitConfig.class), anyString());

    assertThat(response.isAsync()).isTrue();
    assertThat(createdTask.getSetupAbstractions().get(Cd1SetupFields.APP_ID_FIELD)).isEqualTo(APP_ID);
    assertThat(createdTask.getData().getParameters()).isNotEmpty();
    TerraformProvisionParameters parameters = (TerraformProvisionParameters) createdTask.getData().getParameters()[0];
    assertThat(parameters.getVariables()).isNotEmpty();
    assertThat(parameters.getBackendConfigs()).isNotEmpty();
    assertThat(parameters.isSkipRefreshBeforeApplyingPlan()).isTrue();
    assertParametersVariables(parameters);
    assertParametersBackendConfigs(parameters);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testExecuteTerraformDestroyWithConfigurationAndStateFile() {
    destroyProvisionState.setVariables(getTerraformVariables());
    destroyProvisionState.setBackendConfigs(getTerraformBackendConfigs());
    destroyProvisionState.setProvisionerId(PROVISIONER_ID);
    TerraformInfrastructureProvisioner provisioner = TerraformInfrastructureProvisioner.builder()
                                                         .appId(APP_ID)
                                                         .path("current/working/directory")
                                                         .variables(getTerraformVariables())
                                                         .build();
    GitConfig gitConfig = GitConfig.builder().branch("master").build();
    FileMetadata fileMetadata =
        FileMetadata.builder()
            .metadata(ImmutableMap.of("variables", ImmutableMap.of("region", "us-west"), "backend_configs",
                ImmutableMap.of("bucket", "tf-remote-state", "key", "old_terraform.tfstate")))
            .build();

    doReturn("fileId").when(fileService).getLatestFileId(anyString(), eq(FileBucket.TERRAFORM_STATE));
    doReturn(fileMetadata).when(fileService).getFileMetadata("fileId", FileBucket.TERRAFORM_STATE);
    doReturn(provisioner).when(infrastructureProvisionerService).get(APP_ID, PROVISIONER_ID);
    doReturn("taskId").when(delegateService).queueTask(any(DelegateTask.class));
    doReturn(gitConfig).when(gitUtilsManager).getGitConfig(anyString());
    ExecutionResponse response = destroyProvisionState.execute(executionContext);

    ArgumentCaptor<DelegateTask> taskCaptor = ArgumentCaptor.forClass(DelegateTask.class);
    verify(delegateService).queueTask(taskCaptor.capture());
    DelegateTask createdTask = taskCaptor.getValue();

    verify(gitConfigHelperService).convertToRepoGitConfig(any(GitConfig.class), anyString());
    assertThat(response.isAsync()).isTrue();
    assertThat(createdTask.getSetupAbstractions().get(Cd1SetupFields.APP_ID_FIELD)).isEqualTo(APP_ID);
    assertThat(createdTask.getData().getParameters()).isNotEmpty();
    TerraformProvisionParameters parameters = (TerraformProvisionParameters) createdTask.getData().getParameters()[0];
    assertThat(parameters.getVariables()).isNotEmpty();
    assertThat(parameters.getBackendConfigs()).isNotEmpty();
    assertThat(parameters.isSkipRefreshBeforeApplyingPlan()).isFalse();
    assertParametersVariables(parameters);
    assertParametersBackendConfigs(parameters);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testExecuteTerraformDestroyUsingFileMetaData() {
    destroyProvisionState.setProvisionerId(PROVISIONER_ID);
    TerraformInfrastructureProvisioner provisioner = TerraformInfrastructureProvisioner.builder()
                                                         .appId(APP_ID)
                                                         .path("current/working/directory")
                                                         .variables(getTerraformVariables())
                                                         .skipRefreshBeforeApplyingPlan(true)
                                                         .build();
    GitConfig gitConfig = GitConfig.builder().branch("master").build();
    FileMetadata fileMetadata =
        FileMetadata.builder()
            .metadata(
                ImmutableMap.<String, Object>builder()
                    .put("variables", ImmutableMap.of("region", "us-east", "vpc_id", "vpc-id"))
                    .put("encrypted_variables", ImmutableMap.of("access_key", "access_key", "secret_key", "secret_key"))
                    .put("backend_configs", ImmutableMap.of("bucket", "tf-remote-state", "key", "terraform.tfstate"))
                    .put("encrypted_backend_configs", ImmutableMap.of("access_token", "access_token"))
                    .put("environment_variables", ImmutableMap.of("TF_LOG", "TRACE"))
                    .put("encrypted_environment_variables", ImmutableMap.of("access_token", "access_token"))
                    .put("targets", asList("target1", "target2"))
                    .put("tf_var_files", asList("file1", "file2"))
                    .build())
            .build();

    doReturn("fileId")
        .when(fileService)
        .getLatestFileIdByQualifier(anyString(), eq(FileBucket.TERRAFORM_STATE), eq("apply"));
    doReturn(fileMetadata).when(fileService).getFileMetadata("fileId", FileBucket.TERRAFORM_STATE);
    doReturn(provisioner).when(infrastructureProvisionerService).get(APP_ID, PROVISIONER_ID);
    doReturn("taskId").when(delegateService).queueTask(any(DelegateTask.class));
    doReturn(gitConfig).when(gitUtilsManager).getGitConfig(anyString());
    doAnswer(invocation -> invocation.getArgumentAt(0, String.class) + "-rendered")
        .when(executionContext)
        .renderExpression(anyString());
    ExecutionResponse response = destroyProvisionState.execute(executionContext);

    ArgumentCaptor<DelegateTask> taskCaptor = ArgumentCaptor.forClass(DelegateTask.class);
    verify(delegateService).queueTask(taskCaptor.capture());
    DelegateTask createdTask = taskCaptor.getValue();
    verify(gitConfigHelperService).convertToRepoGitConfig(any(GitConfig.class), anyString());

    assertThat(response.isAsync()).isTrue();
    assertThat(createdTask.getSetupAbstractions().get(Cd1SetupFields.APP_ID_FIELD)).isEqualTo(APP_ID);
    assertThat(createdTask.getData().getParameters()).isNotEmpty();
    TerraformProvisionParameters parameters = (TerraformProvisionParameters) createdTask.getData().getParameters()[0];
    assertThat(parameters.getVariables()).isNotEmpty();
    assertThat(parameters.getBackendConfigs()).isNotEmpty();
    assertThat(parameters.getEnvironmentVariables()).isNotEmpty();
    assertThat(parameters.isSkipRefreshBeforeApplyingPlan()).isTrue();
    assertParametersVariables(parameters);
    assertParametersBackendConfigs(parameters);
    assertParametersEnvironmentVariables(parameters);
    assertThat(parameters.getTfVarFiles()).containsExactlyInAnyOrder("file1-rendered", "file2-rendered");
    assertThat(parameters.getTargets()).containsExactlyInAnyOrder("target1-rendered", "target2-rendered");
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testExecuteTerraformDestroyWithOnlyBackendConfigs() {
    destroyProvisionState.setProvisionerId(PROVISIONER_ID);
    destroyProvisionState.setBackendConfigs(getTerraformBackendConfigs());
    TerraformInfrastructureProvisioner provisioner = TerraformInfrastructureProvisioner.builder()
                                                         .appId(APP_ID)
                                                         .path("current/working/directory")
                                                         .variables(getTerraformVariables())
                                                         .skipRefreshBeforeApplyingPlan(false)
                                                         .build();
    GitConfig gitConfig = GitConfig.builder().branch("master").build();

    doReturn(provisioner).when(infrastructureProvisionerService).get(APP_ID, PROVISIONER_ID);
    doReturn("taskId").when(delegateService).queueTask(any(DelegateTask.class));
    doReturn(gitConfig).when(gitUtilsManager).getGitConfig(anyString());
    ExecutionResponse response = destroyProvisionState.execute(executionContext);

    ArgumentCaptor<DelegateTask> taskCaptor = ArgumentCaptor.forClass(DelegateTask.class);
    verify(delegateService).queueTask(taskCaptor.capture());
    DelegateTask createdTask = taskCaptor.getValue();
    verify(gitConfigHelperService).convertToRepoGitConfig(any(GitConfig.class), anyString());

    assertThat(response.isAsync()).isTrue();
    assertThat(createdTask.getSetupAbstractions().get(Cd1SetupFields.APP_ID_FIELD)).isEqualTo(APP_ID);
    assertThat(createdTask.getData().getParameters()).isNotEmpty();
    TerraformProvisionParameters parameters = (TerraformProvisionParameters) createdTask.getData().getParameters()[0];
    assertThat(parameters.getBackendConfigs()).isNotEmpty();
    assertThat(parameters.getVariables()).isEmpty();
    assertThat(parameters.isSkipRefreshBeforeApplyingPlan()).isFalse();
    assertParametersBackendConfigs(parameters);
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void testExecuteInheritApprovedPlan() {
    state.setInheritApprovedPlan(true);
    List<ContextElement> terraformProvisionInheritPlanElements = new ArrayList<>();
    TerraformProvisionInheritPlanElement terraformProvisionInheritPlanElement =
        TerraformProvisionInheritPlanElement.builder()
            .provisionerId(PROVISIONER_ID)
            .workspace("workspace")
            .sourceRepoReference("sourceRepoReference")
            .backendConfigs(getTerraformBackendConfigs())
            .environmentVariables(getTerraformEnvironmentVariables())
            .targets(Arrays.asList("target1"))
            .variables(getTerraformVariables())
            .build();
    terraformProvisionInheritPlanElements.add(terraformProvisionInheritPlanElement);
    when(executionContext.getContextElementList(TERRAFORM_INHERIT_PLAN))
        .thenReturn(terraformProvisionInheritPlanElements);

    when(executionContext.getAppId()).thenReturn(APP_ID);
    doReturn(Environment.Builder.anEnvironment().build()).when(executionContext).getEnv();
    state.setProvisionerId(PROVISIONER_ID);
    TerraformInfrastructureProvisioner provisioner = TerraformInfrastructureProvisioner.builder()
                                                         .appId(APP_ID)
                                                         .path("current/working/directory")
                                                         .sourceRepoBranch("sourceRepoBranch")
                                                         .build();
    GitConfig gitConfig = GitConfig.builder().branch("master").build();
    doReturn(provisioner).when(infrastructureProvisionerService).get(APP_ID, PROVISIONER_ID);
    doReturn("fileId").when(fileService).getLatestFileId(anyString(), eq(FileBucket.TERRAFORM_STATE));
    doReturn(gitConfig).when(gitUtilsManager).getGitConfig(anyString());
    when(executionContext.getWorkflowExecutionId()).thenReturn(WORKFLOW_EXECUTION_ID);
    when(executionContext.prepareSweepingOutputInquiryBuilder()).thenReturn(SweepingOutputInquiry.builder());
    List<EncryptedDataDetail> encryptedDataDetails = new ArrayList<>();
    when(secretManager.getEncryptionDetails(gitConfig, GLOBAL_APP_ID, WORKFLOW_EXECUTION_ID))
        .thenReturn(encryptedDataDetails);
    ExecutionResponse executionResponse = state.execute(executionContext);

    verify(gitConfigHelperService).convertToRepoGitConfig(any(GitConfig.class), anyString());
    verify(infrastructureProvisionerService, times(1)).get(APP_ID, PROVISIONER_ID);
    verify(fileService, times(1)).getLatestFileId(anyString(), any(FileBucket.class));
    verify(gitUtilsManager, times(1)).getGitConfig(anyString());
    verify(infrastructureProvisionerService, times(1)).extractTextVariables(anyList(), any(ExecutionContext.class));
    // once for environment variables, once for variables, once for backend configs
    verify(infrastructureProvisionerService, times(3)).extractEncryptedTextVariables(anyList(), eq(APP_ID));
    // once for environment variables, once for variables
    verify(infrastructureProvisionerService, times(2)).extractUnresolvedTextVariables(anyList());
    verify(secretManager, times(1)).getEncryptionDetails(any(GitConfig.class), anyString(), anyString());
    assertThat(executionResponse.getCorrelationIds().get(0)).isEqualTo("uuid");
    assertThat(((ScriptStateExecutionData) executionResponse.getStateExecutionData()).getActivityId())
        .isEqualTo("uuid");
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testExecuteInternalInheritedInvalid() {
    state.setInheritApprovedPlan(true);
    state.setProvisionerId(PROVISIONER_ID);
    TerraformInfrastructureProvisioner provisioner = TerraformInfrastructureProvisioner.builder()
                                                         .appId(APP_ID)
                                                         .path("current/working/directory")
                                                         .variables(getTerraformVariables())
                                                         .build();
    doReturn(provisioner).when(infrastructureProvisionerService).get(APP_ID, PROVISIONER_ID);

    // No previous terraform plan executed
    doReturn(Collections.emptyList()).when(executionContext).getContextElementList(TERRAFORM_INHERIT_PLAN);
    assertThatThrownBy(() -> state.execute(executionContext))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("No previous Terraform plan execution found");

    // No previous terraform plan executed for PROVISIONER_ID
    doReturn(
        Collections.singletonList(TerraformProvisionInheritPlanElement.builder().provisionerId("NOT_THIS_ID").build()))
        .when(executionContext)
        .getContextElementList(TERRAFORM_INHERIT_PLAN);
    assertThatThrownBy(() -> state.execute(executionContext))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("No Terraform provision command found with current provisioner");

    // Invalid provisioner path
    doReturn(null).when(executionContext).renderExpression("current/working/directory");
    doReturn(
        Collections.singletonList(TerraformProvisionInheritPlanElement.builder().provisionerId(PROVISIONER_ID).build()))
        .when(executionContext)
        .getContextElementList(TERRAFORM_INHERIT_PLAN);
    assertThatThrownBy(() -> state.execute(executionContext))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("Invalid Terraform script path");

    // Empty source repo reference
    doReturn("current/working/directory").when(executionContext).renderExpression("current/working/directory");
    doReturn(Collections.singletonList(TerraformProvisionInheritPlanElement.builder()
                                           .sourceRepoReference(null)
                                           .provisionerId(PROVISIONER_ID)
                                           .build()))
        .when(executionContext)
        .getContextElementList(TERRAFORM_INHERIT_PLAN);
    assertThatThrownBy(() -> state.execute(executionContext))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("No commit id found in context inherit tf plan element");
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void testHandleAsyncResponseInternalRunPlanOnly() {
    state.setRunPlanOnly(true);
    state.setProvisionerId(PROVISIONER_ID);
    when(executionContext.getAppId()).thenReturn(APP_ID);
    Map<String, ResponseData> response = new HashMap<>();
    TerraformExecutionData terraformExecutionData =
        TerraformExecutionData.builder().executionStatus(ExecutionStatus.SUCCESS).tfPlanJson("").build();
    response.put("activityId", terraformExecutionData);
    TerraformInfrastructureProvisioner provisioner = TerraformInfrastructureProvisioner.builder().appId(APP_ID).build();
    doReturn(provisioner).when(infrastructureProvisionerService).get(APP_ID, PROVISIONER_ID);
    doReturn(SweepingOutputInstance.builder())
        .when(executionContext)
        .prepareSweepingOutputBuilder(any(SweepingOutputInstance.Scope.class));

    ExecutionResponse executionResponse = state.handleAsyncResponse(executionContext, response);
    assertThat(executionResponse.getStateExecutionData()).isEqualTo(terraformExecutionData);
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(ExecutionStatus.SUCCESS);
    assertThat(
        ((TerraformProvisionInheritPlanElement) executionResponse.getContextElements().get(0)).getProvisionerId())
        .isEqualTo(PROVISIONER_ID);
    verify(infrastructureProvisionerService, times(1)).get(APP_ID, PROVISIONER_ID);
    // once for saving the tf plan json variable
    verify(sweepingOutputService, times(1)).save(any(SweepingOutputInstance.class));
    verify(executionContext, times(1)).prepareSweepingOutputBuilder(eq(Scope.PIPELINE));
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void testHandleAsyncResponseSavePlan() {
    state.setRunPlanOnly(true);
    state.setExportPlanToApplyStep(true);
    Map<String, ResponseData> response = new HashMap<>();
    response.put("activityId",
        TerraformExecutionData.builder().tfPlanFile("TFPlanFileContent".getBytes()).tfPlanJson("{}").build());
    doReturn("workflowExecutionId").when(executionContext).getWorkflowExecutionId();
    state.setProvisionerId(PROVISIONER_ID);
    doReturn(SweepingOutputInquiry.builder()).when(executionContext).prepareSweepingOutputInquiryBuilder();
    doReturn(TerraformInfrastructureProvisioner.builder().build())
        .when(infrastructureProvisionerService)
        .get(APP_ID, PROVISIONER_ID);
    doReturn(SweepingOutputInstance.builder())
        .when(executionContext)
        .prepareSweepingOutputBuilder(any(SweepingOutputInstance.Scope.class));
    ExecutionResponse executionResponse = state.handleAsyncResponse(executionContext, response);
    assertThat(
        ((TerraformProvisionInheritPlanElement) executionResponse.getContextElements().get(0)).getProvisionerId())
        .isEqualTo(PROVISIONER_ID);
    verify(secretManager, times(1))
        .saveFile(anyString(), anyString(), anyString(), any(UsageRestrictions.class), any(BoundedInputStream.class),
            anyBoolean(), anyBoolean());
    // once for saving the tfplan content, once for saving the tfplan json variable
    verify(sweepingOutputService, times(2)).save(any(SweepingOutputInstance.class));
    verify(executionContext, times(1)).prepareSweepingOutputBuilder(eq(Scope.WORKFLOW));
    verify(executionContext, times(1)).prepareSweepingOutputBuilder(eq(Scope.PIPELINE));
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void testHandleAsyncResponseInternalRegularFail() {
    state.setProvisionerId(PROVISIONER_ID);
    when(executionContext.getAppId()).thenReturn(APP_ID);
    Map<String, ResponseData> response = new HashMap<>();
    TerraformExecutionData terraformExecutionData =
        TerraformExecutionData.builder().executionStatus(ExecutionStatus.FAILED).build();
    response.put("activityId", terraformExecutionData);
    TerraformInfrastructureProvisioner provisioner = TerraformInfrastructureProvisioner.builder().appId(APP_ID).build();
    doReturn(provisioner).when(infrastructureProvisionerService).get(APP_ID, PROVISIONER_ID);

    when(executionContext.prepareSweepingOutputInquiryBuilder()).thenReturn(SweepingOutputInquiry.builder());
    doReturn(SweepingOutputInstance.builder())
        .when(executionContext)
        .prepareSweepingOutputBuilder(SweepingOutputInstance.Scope.WORKFLOW);

    ExecutionResponse executionResponse = state.handleAsyncResponse(executionContext, response);

    verify(infrastructureProvisionerService, times(1)).get(APP_ID, PROVISIONER_ID);
    assertThat(executionResponse.getStateExecutionData()).isEqualTo(terraformExecutionData);
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(ExecutionStatus.FAILED);
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void testGetTerraformPlanFromSecretManager() {
    String terraformPlanSecretManagerId = "terraformPlanSecretManagerId";
    when(executionContext.getWorkflowExecutionId()).thenReturn(WORKFLOW_EXECUTION_ID);
    when(executionContext.getAccountId()).thenReturn(ACCOUNT_ID);
    SweepingOutputInstance sweepingOutputInstance =
        SweepingOutputInstance.builder()
            .value(TerraformPlanParam.builder().terraformPlanSecretManagerId(terraformPlanSecretManagerId).build())
            .build();
    when(executionContext.prepareSweepingOutputInquiryBuilder()).thenReturn(SweepingOutputInquiry.builder());
    when(sweepingOutputService.find(any(SweepingOutputInquiry.class))).thenReturn(sweepingOutputInstance);
    byte[] terraformPlanContent = "terraformPlanContent".getBytes();
    when(secretManager.getFileContents(anyString(), anyString())).thenReturn(terraformPlanContent);
    byte[] retrievedFileContent = state.getTerraformPlanFromSecretManager(executionContext);
    assertThat(retrievedFileContent).isEqualTo(terraformPlanContent);
    verify(secretManager, times(1)).getFileContents(ACCOUNT_ID, terraformPlanSecretManagerId);
    verify(sweepingOutputService, times(1)).find(any(SweepingOutputInquiry.class));
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void testExecuteRegularWithEnvironmentVariables() {
    state.setProvisionerId(PROVISIONER_ID);
    List<NameValuePair> nameValuePairList =
        asList(NameValuePair.builder().name("TF_LOG").value("TRACE").valueType("TEXT").build(),
            NameValuePair.builder().name("access_token").value("access_token").valueType("ENCRYPTED_TEXT").build(),
            NameValuePair.builder().name("nil").valueType("TEXT").build(),
            NameValuePair.builder().name("noValueType").value("value").valueType(null).build());
    state.setEnvironmentVariables(nameValuePairList);
    TerraformInfrastructureProvisioner provisioner = TerraformInfrastructureProvisioner.builder()
                                                         .appId(APP_ID)
                                                         .path("current/working/directory")
                                                         .environmentVariables(getTerraformEnvironmentVariables())
                                                         .skipRefreshBeforeApplyingPlan(true)
                                                         .build();
    GitConfig gitConfig = GitConfig.builder().branch("master").build();

    doReturn(provisioner).when(infrastructureProvisionerService).get(APP_ID, PROVISIONER_ID);
    doReturn("taskId").when(delegateService).queueTask(any(DelegateTask.class));
    doReturn(gitConfig).when(gitUtilsManager).getGitConfig(anyString());
    ExecutionResponse response = state.execute(executionContext);

    ArgumentCaptor<DelegateTask> taskCaptor = ArgumentCaptor.forClass(DelegateTask.class);
    verify(delegateService).queueTask(taskCaptor.capture());
    DelegateTask createdTask = taskCaptor.getValue();
    verify(gitConfigHelperService).convertToRepoGitConfig(any(GitConfig.class), anyString());

    assertThat(response.isAsync()).isTrue();
    assertThat(createdTask.getData().getParameters()).isNotEmpty();
    TerraformProvisionParameters parameters = (TerraformProvisionParameters) createdTask.getData().getParameters()[0];
    assertThat(parameters.getEnvironmentVariables()).isNotEmpty();
    assertThat(parameters.isSkipRefreshBeforeApplyingPlan()).isTrue();
    assertParametersEnvironmentVariables(parameters);
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void testHandleAsyncResponseInternalRegularDestroy() {
    TerraformProvisionState destroyProvisionStateSpy = spy(destroyProvisionState);
    destroyProvisionStateSpy.setProvisionerId(PROVISIONER_ID);
    destroyProvisionStateSpy.setTargets(Arrays.asList("target1"));
    when(executionContext.getAppId()).thenReturn(APP_ID);
    String outputs = "{\n"
        + "\"key\": {\n"
        + "\"value\": \"value1\"\n"
        + "}\n"
        + "}";
    Map<String, ResponseData> response = new HashMap<>();
    TerraformExecutionData terraformExecutionData = TerraformExecutionData.builder()
                                                        .workspace("workspace")
                                                        .executionStatus(ExecutionStatus.SUCCESS)
                                                        .activityId(ACTIVITY_ID)
                                                        .outputs(outputs)
                                                        .build();
    response.put("activityId", terraformExecutionData);
    TerraformInfrastructureProvisioner provisioner = TerraformInfrastructureProvisioner.builder().appId(APP_ID).build();
    doReturn(provisioner).when(infrastructureProvisionerService).get(APP_ID, PROVISIONER_ID);

    when(executionContext.getContextElement(ContextElementType.TERRAFORM_PROVISION))
        .thenReturn(TerraformOutputInfoElement.builder().build());
    when(infrastructureProvisionerService.getManagerExecutionCallback(anyString(), anyString(), anyString()))
        .thenReturn(mock(ManagerExecutionLogCallback.class));
    when(executionContext.prepareSweepingOutputInquiryBuilder()).thenReturn(SweepingOutputInquiry.builder());

    ExecutionResponse executionResponse = destroyProvisionStateSpy.handleAsyncResponse(executionContext, response);

    verify(infrastructureProvisionerService, times(1))
        .regenerateInfrastructureMappings(
            anyString(), any(ExecutionContext.class), anyMap(), any(Optional.class), any(Optional.class));
    verify(infrastructureProvisionerService, times(1)).get(APP_ID, PROVISIONER_ID);
    assertThat(executionResponse.getStateExecutionData()).isEqualTo(terraformExecutionData);
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(ExecutionStatus.SUCCESS);

    TerraformOutputInfoElement terraformOutputInfoElement =
        (TerraformOutputInfoElement) executionResponse.getContextElements().get(0);
    assertThat(terraformOutputInfoElement.paramMap(executionContext)).containsKeys("terraform");
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void testDeleteTerraformPlanFromSecretManager() {
    String terraformPlanSecretManagerId = "terraformPlanSecretManagerId";
    String sweepingOutputInstanceUUID = "sweepingOutputInstanceUUID";
    when(executionContext.getWorkflowExecutionId()).thenReturn(WORKFLOW_EXECUTION_ID);
    when(executionContext.getAccountId()).thenReturn(ACCOUNT_ID);
    when(executionContext.getAppId()).thenReturn(APP_ID);
    SweepingOutputInstance sweepingOutputInstance =
        SweepingOutputInstance.builder()
            .uuid(sweepingOutputInstanceUUID)
            .value(TerraformPlanParam.builder().terraformPlanSecretManagerId(terraformPlanSecretManagerId).build())
            .build();
    when(executionContext.prepareSweepingOutputInquiryBuilder()).thenReturn(SweepingOutputInquiry.builder());
    when(sweepingOutputService.find(any(SweepingOutputInquiry.class))).thenReturn(sweepingOutputInstance);
    state.deleteTerraformPlanFromSecretManager(executionContext);
    verify(sweepingOutputService, times(1)).find(any(SweepingOutputInquiry.class));
    verify(secretManager, times(1)).deleteFile(ACCOUNT_ID, terraformPlanSecretManagerId);
    verify(sweepingOutputService, times(1)).deleteById(APP_ID, sweepingOutputInstanceUUID);
  }

  private void assertParametersVariables(TerraformProvisionParameters parameters) {
    assertThat(parameters.getVariables().keySet()).containsExactlyInAnyOrder("region", "vpc_id");
    assertThat(parameters.getVariables().values()).containsExactlyInAnyOrder("us-east", "vpc-id");
    assertThat(parameters.getEncryptedVariables().keySet()).containsExactlyInAnyOrder("access_key", "secret_key");
  }

  private void assertParametersBackendConfigs(TerraformProvisionParameters parameters) {
    assertThat(parameters.getBackendConfigs().keySet()).containsExactlyInAnyOrder("key", "bucket");
    assertThat(parameters.getBackendConfigs().values())
        .containsExactlyInAnyOrder("terraform.tfstate", "tf-remote-state");
    assertThat(parameters.getEncryptedBackendConfigs().keySet()).containsExactlyInAnyOrder("access_token");
  }

  private void assertParametersEnvironmentVariables(TerraformProvisionParameters parameters) {
    assertThat(parameters.getEnvironmentVariables().keySet()).containsOnly("TF_LOG");
    assertThat(parameters.getEnvironmentVariables().values()).containsOnly("TRACE");
    assertThat(parameters.getEncryptedEnvironmentVariables().keySet()).containsExactlyInAnyOrder("access_token");
  }

  private List<NameValuePair> getTerraformVariables() {
    return asList(NameValuePair.builder().name("region").value("us-east").valueType("TEXT").build(),
        NameValuePair.builder().name("vpc_id").value("vpc-id").valueType("TEXT").build(),
        NameValuePair.builder().name("access_key").value("access_key").valueType("ENCRYPTED_TEXT").build(),
        NameValuePair.builder().name("secret_key").value("secret_key").valueType("ENCRYPTED_TEXT").build());
  }

  private List<NameValuePair> getTerraformBackendConfigs() {
    return asList(NameValuePair.builder().name("key").value("terraform.tfstate").valueType("TEXT").build(),
        NameValuePair.builder().name("bucket").value("tf-remote-state").valueType("TEXT").build(),
        NameValuePair.builder().name("access_token").value("access_token").valueType("ENCRYPTED_TEXT").build());
  }

  private List<NameValuePair> getTerraformEnvironmentVariables() {
    return asList(NameValuePair.builder().name("TF_LOG").value("TRACE").valueType("TEXT").build(),
        NameValuePair.builder().name("access_token").value("access_token").valueType("ENCRYPTED_TEXT").build());
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testHandleAsyncResponseSaveUserInputs() {
    List<NameValuePair> nameValuePairList =
        asList(NameValuePair.builder().name("key").value("value").valueType("TEXT").build(),
            NameValuePair.builder().name("password").value("password").valueType("ENCRYPTED_TEXT").build(),
            NameValuePair.builder().name("nil").valueType("TEXT").build(),
            NameValuePair.builder().name("noValueType").value("value").valueType(null).build());

    TerraformExecutionData responseData = TerraformExecutionData.builder()
                                              .executionStatus(ExecutionStatus.SUCCESS)
                                              .workspace("workspace")
                                              .targets(Collections.emptyList())
                                              .backendConfigs(nameValuePairList)
                                              .variables(nameValuePairList)
                                              .tfVarFiles(asList("file-1", "file-2"))
                                              .targets(asList("target1", "target2"))
                                              .build();

    TerraformInfrastructureProvisioner provisioner = TerraformInfrastructureProvisioner.builder()
                                                         .appId(APP_ID)
                                                         .path("current/working/directory")
                                                         .variables(getTerraformVariables())
                                                         .build();
    Map<String, ResponseData> responseMap = ImmutableMap.of(ACTIVITY_ID, responseData);
    state.setRunPlanOnly(false);
    state.setProvisionerId(PROVISIONER_ID);
    doReturn(provisioner).when(infrastructureProvisionerService).get(APP_ID, PROVISIONER_ID);

    state.handleAsyncResponse(executionContext, responseMap);
    ArgumentCaptor<Map> othersArgumentCaptor = ArgumentCaptor.forClass(Map.class);
    verify(wingsPersistence, times(1)).save(any(TerraformConfig.class));
    verify(fileService, times(1))
        .updateParentEntityIdAndVersion(eq(PhaseStep.class), isNull(String.class), isNull(Integer.class),
            isNull(String.class), othersArgumentCaptor.capture(), eq(FileBucket.TERRAFORM_STATE));
    Map<String, Object> storeOthersMap = (Map<String, Object>) othersArgumentCaptor.getValue();

    Map<String, String> expectedEncryptedNameValuePair = ImmutableMap.of("password", "password");
    assertThat(storeOthersMap.get("variables")).isEqualTo(ImmutableMap.of("key", "value", "noValueType", "value"));
    assertThat(storeOthersMap.get("encrypted_variables")).isEqualTo(expectedEncryptedNameValuePair);
    assertThat(storeOthersMap.get("backend_configs")).isEqualTo(ImmutableMap.of("key", "value"));
    assertThat(storeOthersMap.get("encrypted_backend_configs")).isEqualTo(expectedEncryptedNameValuePair);
    assertThat(storeOthersMap.get("targets")).isEqualTo(responseData.getTargets());
    assertThat(storeOthersMap.get("tf_var_files")).isEqualTo(responseData.getTfVarFiles());
    assertThat(storeOthersMap.get("tf_workspace")).isEqualTo(responseData.getWorkspace());

    // Don't save terraform config if status is not SUCCESS
    reset(wingsPersistence);
    responseData.setExecutionStatus(ExecutionStatus.FAILED);
    state.handleAsyncResponse(executionContext, responseMap);
    verify(wingsPersistence, never()).save(any(TerraformConfig.class));
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void testHandleAsyncResponseSaveEnvironmentVariables() {
    List<NameValuePair> nameValuePairList =
        asList(NameValuePair.builder().name("key").value("value").valueType("TEXT").build(),
            NameValuePair.builder().name("password").value("password").valueType("ENCRYPTED_TEXT").build(),
            NameValuePair.builder().name("nil").valueType("TEXT").build(),
            NameValuePair.builder().name("noValueType").value("value").valueType(null).build());

    TerraformExecutionData responseData = TerraformExecutionData.builder()
                                              .executionStatus(ExecutionStatus.SUCCESS)
                                              .environmentVariables(nameValuePairList)
                                              .build();

    TerraformInfrastructureProvisioner provisioner = TerraformInfrastructureProvisioner.builder().appId(APP_ID).build();
    Map<String, ResponseData> responseMap = ImmutableMap.of(ACTIVITY_ID, responseData);
    state.setRunPlanOnly(false);
    state.setProvisionerId(PROVISIONER_ID);
    doReturn(provisioner).when(infrastructureProvisionerService).get(APP_ID, PROVISIONER_ID);

    state.handleAsyncResponse(executionContext, responseMap);
    ArgumentCaptor<Map> othersArgumentCaptor = ArgumentCaptor.forClass(Map.class);
    verify(wingsPersistence, times(1)).save(any(TerraformConfig.class));
    verify(fileService, times(1))
        .updateParentEntityIdAndVersion(eq(PhaseStep.class), isNull(String.class), isNull(Integer.class),
            isNull(String.class), othersArgumentCaptor.capture(), eq(FileBucket.TERRAFORM_STATE));
    Map<String, Object> storeOthersMap = (Map<String, Object>) othersArgumentCaptor.getValue();

    Map<String, String> expectedEncryptedNameValuePair = ImmutableMap.of("password", "password");
    assertThat(storeOthersMap.get("environment_variables")).isEqualTo(ImmutableMap.of("key", "value"));
    assertThat(storeOthersMap.get("encrypted_environment_variables")).isEqualTo(expectedEncryptedNameValuePair);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testHandleAsyncResponseSaveDestroyStepUserInputs() {
    TerraformExecutionData responseData = TerraformExecutionData.builder()
                                              .executionStatus(ExecutionStatus.SUCCESS)
                                              .workspace("workspace")
                                              .targets(asList("target1", "target2"))
                                              .build();

    TerraformInfrastructureProvisioner provisioner = TerraformInfrastructureProvisioner.builder()
                                                         .appId(APP_ID)
                                                         .path("current/working/directory")
                                                         .variables(getTerraformVariables())
                                                         .build();
    Map<String, ResponseData> responseMap = ImmutableMap.of(ACTIVITY_ID, responseData);
    destroyProvisionState.setRunPlanOnly(false);
    destroyProvisionState.setProvisionerId(PROVISIONER_ID);
    doReturn(provisioner).when(infrastructureProvisionerService).get(APP_ID, PROVISIONER_ID);

    // Save terraform config if there any targets
    destroyProvisionState.setTargets(asList("target1", "target2"));
    destroyProvisionState.handleAsyncResponse(executionContext, responseMap);
    verify(wingsPersistence, times(1)).save(any(TerraformConfig.class));
    verify(wingsPersistence, never()).delete(any(Query.class));

    // Delete terraform config if no any targets
    reset(wingsPersistence);
    doReturn(mock(Query.class)).when(wingsPersistence).createQuery(any());
    destroyProvisionState.setTargets(Collections.emptyList());
    destroyProvisionState.handleAsyncResponse(executionContext, responseMap);
    verify(wingsPersistence, never()).save(any(TerraformConfig.class));
    verify(wingsPersistence, times(1)).delete(any(Query.class));

    // Don't do anything if the status is not SUCCESS
    reset(wingsPersistence);
    responseData.setExecutionStatus(ExecutionStatus.FAILED);
    destroyProvisionState.handleAsyncResponse(executionContext, responseMap);
    verify(wingsPersistence, never()).save(any(TerraformConfig.class));
    verify(wingsPersistence, never()).delete(any(Query.class));
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetTerraformInfrastructureProvisioner() {
    // TerraformInfrastructureProvisioner doesn't exists
    state.setProvisionerId(PROVISIONER_ID);
    doReturn(null).when(infrastructureProvisionerService).get(anyString(), eq(PROVISIONER_ID));
    assertThatThrownBy(() -> state.getTerraformInfrastructureProvisioner(executionContext))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("Infrastructure Provisioner does not exist");

    // Invalid type of  InfrastructureProvisioner
    doReturn(mock(InfrastructureProvisioner.class))
        .when(infrastructureProvisionerService)
        .get(anyString(), eq(PROVISIONER_ID));
    assertThatThrownBy(() -> state.getTerraformInfrastructureProvisioner(executionContext))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("should be of Terraform type.");

    // Valid InfrastructureProvisioner
    TerraformInfrastructureProvisioner provisioner = TerraformInfrastructureProvisioner.builder().build();
    doReturn(provisioner).when(infrastructureProvisionerService).get(anyString(), eq(PROVISIONER_ID));
    assertThat(state.getTerraformInfrastructureProvisioner(executionContext)).isEqualTo(provisioner);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testCreateActivityOnExecute() {
    TerraformProvisionState spyState = spy(state);
    doReturn(Activity.builder().uuid(ACTIVITY_ID).build()).when(activityService).save(any(Activity.class));
    doReturn(null).when(spyState).executeInternal(executionContext, ACTIVITY_ID); // ignore execution

    // Missing STANDARD context element
    doReturn(null).when(executionContext).getContextElement(ContextElementType.STANDARD);
    assertThatThrownBy(() -> spyState.execute(executionContext)).hasMessageContaining("workflowStandardParams");

    // Missing current user in context
    WorkflowStandardParams standardParams = WorkflowStandardParams.Builder.aWorkflowStandardParams().build();
    doReturn(standardParams).when(executionContext).getContextElement(ContextElementType.STANDARD);
    assertThatThrownBy(() -> spyState.execute(executionContext)).hasMessageContaining("currentUser");

    standardParams.setCurrentUser(EmbeddedUser.builder().name(USER_NAME).email(USER_EMAIL).build());
    doReturn(WorkflowType.ORCHESTRATION).when(executionContext).getWorkflowType();
    doReturn(WORKFLOW_EXECUTION_ID).when(executionContext).getWorkflowExecutionId();
    doReturn(WORKFLOW_NAME).when(executionContext).getWorkflowExecutionName();
    ArgumentCaptor<Activity> activityCaptor = ArgumentCaptor.forClass(Activity.class);

    // Valid with BUILD orchestration type
    doReturn(OrchestrationWorkflowType.BUILD).when(executionContext).getOrchestrationWorkflowType();
    spyState.execute(executionContext);
    verify(activityService, times(1)).save(activityCaptor.capture());
    assertCreatedActivity(activityCaptor.getValue(), GLOBAL_ENV_ID, GLOBAL_ENV_ID, ALL);

    // Valid with not BUILD orchestration type
    Environment env = Environment.Builder.anEnvironment()
                          .environmentType(EnvironmentType.NON_PROD)
                          .uuid(ENV_ID)
                          .name(ENV_NAME)
                          .build();
    doReturn(OrchestrationWorkflowType.BASIC).when(executionContext).getOrchestrationWorkflowType();
    doReturn(env).when(executionContext).getEnv();
    spyState.execute(executionContext);
    // 1 time from previous invocation
    verify(activityService, times(2)).save(activityCaptor.capture());
    assertCreatedActivity(activityCaptor.getValue(), ENV_NAME, ENV_ID, EnvironmentType.NON_PROD);
  }

  private void assertCreatedActivity(Activity activity, String envName, String envId, EnvironmentType envType) {
    assertThat(activity.getWorkflowType()).isEqualTo(WorkflowType.ORCHESTRATION);
    assertThat(activity.getWorkflowExecutionId()).isEqualTo(WORKFLOW_EXECUTION_ID);
    assertThat(activity.getWorkflowExecutionName()).isEqualTo(WORKFLOW_NAME);
    assertThat(activity.getStatus()).isEqualTo(ExecutionStatus.RUNNING);
    assertThat(activity.getTriggeredBy().getName()).isEqualTo(USER_NAME);
    assertThat(activity.getTriggeredBy().getEmail()).isEqualTo(USER_EMAIL);
    assertThat(activity.getEnvironmentName()).isEqualTo(envName);
    assertThat(activity.getEnvironmentId()).isEqualTo(envId);
    assertThat(activity.getEnvironmentType()).isEqualTo(envType);
  }
}