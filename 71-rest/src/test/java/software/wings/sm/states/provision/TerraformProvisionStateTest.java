package software.wings.sm.states.provision;

import static io.harness.rule.OwnerRule.ABOSII;
import static io.harness.rule.OwnerRule.GEORGE;
import static io.harness.rule.OwnerRule.VAIBHAV_SI;
import static io.harness.rule.OwnerRule.YOGESH;
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.PROVISIONER_ID;

import com.google.common.collect.ImmutableMap;

import io.harness.beans.DelegateTask;
import io.harness.beans.EmbeddedUser;
import io.harness.beans.FileMetadata;
import io.harness.category.element.UnitTests;
import io.harness.context.ContextElementType;
import io.harness.delegate.service.DelegateAgentFileService.FileBucket;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptedDataDetail;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;
import software.wings.WingsBaseTest;
import software.wings.beans.Activity;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.GitConfig;
import software.wings.beans.NameValuePair;
import software.wings.beans.TerraformInfrastructureProvisioner;
import software.wings.beans.delegation.TerraformProvisionParameters;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.InfrastructureProvisionerService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.StateExecutionContext;
import software.wings.sm.WorkflowStandardParams;
import software.wings.utils.GitUtilsManager;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collector;

public class TerraformProvisionStateTest extends WingsBaseTest {
  @Mock InfrastructureProvisionerService infrastructureProvisionerService;
  @Mock private DelegateService delegateService;
  @Mock private ActivityService activityService;
  @Mock private GitUtilsManager gitUtilsManager;
  @Mock private FileService fileService;
  @Mock private SecretManager secretManager;
  @Mock private ExecutionContextImpl executionContext;
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
    assertThat(provisioner.getWorkspaces().size() == 2 && provisioner.getWorkspaces().equals(Arrays.asList("w1", "w2")))
        .isTrue();
    state.updateProvisionerWorkspaces(provisioner, "w2");
    assertThat(provisioner.getWorkspaces().size() == 2 && provisioner.getWorkspaces().equals(Arrays.asList("w1", "w2")))
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

    final List<NameValuePair> workflowVars = Arrays.asList(wf_var_1, wf_var_2, wf_var_3);
    final List<NameValuePair> provVars = Arrays.asList(prov_var_1, prov_var_2);

    List<NameValuePair> filteredVars_1 = TerraformProvisionState.validateAndFilterVariables(workflowVars, provVars);

    final List<NameValuePair> expected_1 = Arrays.asList(wf_var_1, wf_var_2);
    assertThat(filteredVars_1).isEqualTo(expected_1);

    wf_var_1.setValueType("ENCRYPTED_TEXT");

    final List<NameValuePair> filteredVars_2 =
        TerraformProvisionState.validateAndFilterVariables(workflowVars, provVars);

    final List<NameValuePair> expected_2 = Arrays.asList(wf_var_1, wf_var_2);
    assertThat(filteredVars_2).isEqualTo(expected_2);
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
                                                         .build();
    GitConfig gitConfig = GitConfig.builder().branch("master").build();

    doReturn(provisioner).when(infrastructureProvisionerService).get(APP_ID, PROVISIONER_ID);
    doReturn("taskId").when(delegateService).queueTask(any(DelegateTask.class));
    doReturn(gitConfig).when(gitUtilsManager).getGitConfig(anyString());
    ExecutionResponse response = destroyProvisionState.execute(executionContext);

    ArgumentCaptor<DelegateTask> taskCaptor = ArgumentCaptor.forClass(DelegateTask.class);
    verify(delegateService).queueTask(taskCaptor.capture());
    DelegateTask createdTask = taskCaptor.getValue();

    assertThat(response.getDelegateTaskId()).isEqualTo("taskId");
    assertThat(response.isAsync()).isTrue();
    assertThat(createdTask.getAppId()).isEqualTo(APP_ID);
    assertThat(createdTask.getData().getParameters()).isNotEmpty();
    TerraformProvisionParameters parameters = (TerraformProvisionParameters) createdTask.getData().getParameters()[0];
    assertThat(parameters.getVariables()).isNotEmpty();
    assertThat(parameters.getBackendConfigs()).isNotEmpty();
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

    assertThat(response.getDelegateTaskId()).isEqualTo("taskId");
    assertThat(response.isAsync()).isTrue();
    assertThat(createdTask.getAppId()).isEqualTo(APP_ID);
    assertThat(createdTask.getData().getParameters()).isNotEmpty();
    TerraformProvisionParameters parameters = (TerraformProvisionParameters) createdTask.getData().getParameters()[0];
    assertThat(parameters.getVariables()).isNotEmpty();
    assertThat(parameters.getBackendConfigs()).isNotEmpty();
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
                                                         .build();
    GitConfig gitConfig = GitConfig.builder().branch("master").build();
    FileMetadata fileMetadata =
        FileMetadata.builder()
            .metadata(ImmutableMap.of("variables", ImmutableMap.of("region", "us-east", "vpc_id", "vpc-id"),
                "encrypted_variables", ImmutableMap.of("access_key", "access_key", "secret_key", "secret_key"),
                "backend_configs", ImmutableMap.of("bucket", "tf-remote-state", "key", "terraform.tfstate"),
                "encrypted_backend_configs", ImmutableMap.of("access_token", "access_token")))
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

    assertThat(response.getDelegateTaskId()).isEqualTo("taskId");
    assertThat(response.isAsync()).isTrue();
    assertThat(createdTask.getAppId()).isEqualTo(APP_ID);
    assertThat(createdTask.getData().getParameters()).isNotEmpty();
    TerraformProvisionParameters parameters = (TerraformProvisionParameters) createdTask.getData().getParameters()[0];
    assertThat(parameters.getVariables()).isNotEmpty();
    assertThat(parameters.getBackendConfigs()).isNotEmpty();
    assertParametersVariables(parameters);
    assertParametersBackendConfigs(parameters);
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
                                                         .build();
    GitConfig gitConfig = GitConfig.builder().branch("master").build();

    doReturn(provisioner).when(infrastructureProvisionerService).get(APP_ID, PROVISIONER_ID);
    doReturn("taskId").when(delegateService).queueTask(any(DelegateTask.class));
    doReturn(gitConfig).when(gitUtilsManager).getGitConfig(anyString());
    ExecutionResponse response = destroyProvisionState.execute(executionContext);

    ArgumentCaptor<DelegateTask> taskCaptor = ArgumentCaptor.forClass(DelegateTask.class);
    verify(delegateService).queueTask(taskCaptor.capture());
    DelegateTask createdTask = taskCaptor.getValue();

    assertThat(response.getDelegateTaskId()).isEqualTo("taskId");
    assertThat(response.isAsync()).isTrue();
    assertThat(createdTask.getAppId()).isEqualTo(APP_ID);
    assertThat(createdTask.getData().getParameters()).isNotEmpty();
    TerraformProvisionParameters parameters = (TerraformProvisionParameters) createdTask.getData().getParameters()[0];
    assertThat(parameters.getBackendConfigs()).isNotEmpty();
    assertThat(parameters.getVariables()).isEmpty();
    assertParametersBackendConfigs(parameters);
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

  private List<NameValuePair> getTerraformVariables() {
    return Arrays.asList(NameValuePair.builder().name("region").value("us-east").valueType("TEXT").build(),
        NameValuePair.builder().name("vpc_id").value("vpc-id").valueType("TEXT").build(),
        NameValuePair.builder().name("access_key").value("access_key").valueType("ENCRYPTED_TEXT").build(),
        NameValuePair.builder().name("secret_key").value("secret_key").valueType("ENCRYPTED_TEXT").build());
  }

  private List<NameValuePair> getTerraformBackendConfigs() {
    return Arrays.asList(NameValuePair.builder().name("key").value("terraform.tfstate").valueType("TEXT").build(),
        NameValuePair.builder().name("bucket").value("tf-remote-state").valueType("TEXT").build(),
        NameValuePair.builder().name("access_token").value("access_token").valueType("ENCRYPTED_TEXT").build());
  }
}