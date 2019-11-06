package software.wings.sm.states;

import static io.harness.beans.SweepingOutputInstance.Scope.PIPELINE;
import static io.harness.beans.SweepingOutputInstance.Scope.WORKFLOW;
import static io.harness.delegate.task.shell.ScriptType.BASH;
import static io.harness.rule.OwnerRule.AADITI;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.Environment.EnvironmentType.NON_PROD;
import static software.wings.beans.HostConnectionAttributes.AccessType.KEY;
import static software.wings.beans.HostConnectionAttributes.Builder.aHostConnectionAttributes;
import static software.wings.beans.ServiceTemplate.Builder.aServiceTemplate;
import static software.wings.beans.command.Command.Builder.aCommand;
import static software.wings.service.intfc.ServiceTemplateService.EncryptedFieldComputeMode.OBTAIN_VALUE;
import static software.wings.sm.states.ShellScriptState.ConnectionType.SSH;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.ACTIVITY_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.APP_NAME;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.ENV_NAME;
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID;
import static software.wings.utils.WingsTestConstants.SETTING_ID;
import static software.wings.utils.WingsTestConstants.TEMPLATE_ID;

import com.google.common.collect.ImmutableMap;

import io.harness.CategoryTest;
import io.harness.beans.DelegateTask;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.SweepingOutputInstance;
import io.harness.category.element.UnitTests;
import io.harness.context.ContextElementType;
import io.harness.data.structure.UUIDGenerator;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.command.CommandExecutionResult;
import io.harness.rule.OwnerRule.Owner;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import software.wings.api.ScriptStateExecutionData;
import software.wings.beans.Activity;
import software.wings.beans.Activity.Type;
import software.wings.beans.HostConnectionAttributes;
import software.wings.beans.SettingAttribute;
import software.wings.beans.command.CommandType;
import software.wings.beans.command.ShellExecutionData;
import software.wings.beans.delegation.ShellScriptParameters;
import software.wings.beans.template.TemplateUtils;
import software.wings.expression.ShellScriptFunctor;
import software.wings.service.impl.ActivityHelperService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.SweepingOutputService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.StateExecutionContext;
import software.wings.sm.WorkflowStandardParams;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShellScriptStateTest extends CategoryTest {
  private static final Activity ACTIVITY_WITH_ID = Activity.builder().build();

  static {
    ACTIVITY_WITH_ID.setUuid(ACTIVITY_ID);
  }

  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Mock private ActivityHelperService activityHelperService;
  @Mock private InfrastructureMappingService infrastructureMappingService;
  @Mock private ExecutionContextImpl executionContext;
  @Mock private SweepingOutputService sweepingOutputService;
  @InjectMocks private ShellScriptFunctor shellScriptFunctor;
  @Mock private TemplateUtils templateUtils;
  @Mock private DelegateService delegateService;
  @Mock private SettingsService settingsService;
  @Mock private SecretManager secretManager;
  @Mock private ServiceTemplateService serviceTemplateService;
  @Mock private WorkflowStandardParams workflowStandardParams;
  @InjectMocks private ShellScriptState shellScriptState = new ShellScriptState("ShellScript");

  private ExecutionResponse asyncExecutionResponse;

  @Before
  public void setUp() throws Exception {
    shellScriptState.setSweepingOutputName("test");
    shellScriptState.setSweepingOutputScope(PIPELINE);
    when(executionContext.getApp()).thenReturn(anApplication().accountId(ACCOUNT_ID).uuid(APP_ID).build());
    when(executionContext.getEnv()).thenReturn(anEnvironment().uuid(ENV_ID).appId(APP_ID).build());
    HostConnectionAttributes hostConnectionAttributes =
        aHostConnectionAttributes()
            .withAccessType(KEY)
            .withAccountId(UUIDGenerator.generateUuid())
            .withConnectionType(HostConnectionAttributes.ConnectionType.SSH)
            .withKey("Test Private Key".toCharArray())
            .withKeyless(false)
            .withUserName("TestUser")
            .build();
    when(executionContext.getGlobalSettingValue(ACCOUNT_ID, SETTING_ID)).thenReturn(hostConnectionAttributes);
    when(executionContext.renderExpression(anyString()))
        .thenAnswer(invocation -> invocation.getArgumentAt(0, String.class));
    when(activityHelperService.createAndSaveActivity(executionContext, Type.Verification, shellScriptState.getName(),
             shellScriptState.getStateType(),
             asList(aCommand().withName(ShellScriptParameters.CommandUnit).withCommandType(CommandType.OTHER).build())))
        .thenReturn(ACTIVITY_WITH_ID);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldHandleAsyncResponseOnShellScriptSuccessAndSaveSweepingOutput() {
    when(executionContext.getStateExecutionData())
        .thenReturn(ScriptStateExecutionData.builder().activityId(ACTIVITY_ID).build());
    when(executionContext.prepareSweepingOutputBuilder(any(SweepingOutputInstance.Scope.class)))
        .thenReturn(SweepingOutputInstance.builder());

    Map<String, String> map = new HashMap<>();
    map.put("A", "aaa");
    ExecutionResponse executionResponse = shellScriptState.handleAsyncResponse(executionContext,
        ImmutableMap.of(ACTIVITY_ID,
            CommandExecutionResult.builder()
                .status(CommandExecutionResult.CommandExecutionStatus.SUCCESS)
                .commandExecutionData(ShellExecutionData.builder().sweepingOutputEnvVariables(map).build())
                .build()));
    verify(activityHelperService).updateStatus(ACTIVITY_ID, APP_ID, ExecutionStatus.SUCCESS);
    verify(sweepingOutputService, times(1)).save(any(SweepingOutputInstance.class));
    assertThat(executionResponse).isNotNull();
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(ExecutionStatus.SUCCESS);
    assertThat(
        ((ScriptStateExecutionData) (executionResponse.getStateExecutionData())).getSweepingOutputEnvVariables().size())
        .isEqualTo(1);
    assertThat(((ScriptStateExecutionData) (executionResponse.getStateExecutionData())).getSweepingOutputEnvVariables())
        .containsKey("A");
  }

  @Test
  @Category(UnitTests.class)
  public void shouldHandleAsyncResponseOnShellScriptFailureAndNotSaveSweepingOutput() {
    when(executionContext.getStateExecutionData())
        .thenReturn(ScriptStateExecutionData.builder().activityId(ACTIVITY_ID).build());
    Map<String, String> map = new HashMap<>();
    map.put("A", "aaa");
    ExecutionResponse executionResponse = shellScriptState.handleAsyncResponse(executionContext,
        ImmutableMap.of(ACTIVITY_ID,
            CommandExecutionResult.builder()
                .status(CommandExecutionResult.CommandExecutionStatus.FAILURE)
                .commandExecutionData(ShellExecutionData.builder().sweepingOutputEnvVariables(map).build())
                .build()));
    verify(activityHelperService).updateStatus(ACTIVITY_ID, APP_ID, ExecutionStatus.FAILED);
    verify(sweepingOutputService, times(0)).save(any(SweepingOutputInstance.class));
    assertThat(executionResponse).isNotNull();
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(ExecutionStatus.FAILED);
    assertThat(((ScriptStateExecutionData) (executionResponse.getStateExecutionData())).getSweepingOutputEnvVariables())
        .isNull();
  }

  @Test
  @Category(UnitTests.class)
  public void shouldFailShellScriptStateOnErrorResponse() {
    ExecutionResponse executionResponse = shellScriptState.handleAsyncResponse(executionContext,
        ImmutableMap.of(ACTIVITY_ID, ErrorNotifyResponseData.builder().errorMessage("Failed").build()));
    assertThat(executionResponse).isNotNull();
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(ExecutionStatus.FAILED);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldGetPatternsForRequiredContextElementType() {
    shellScriptState.setScriptString("echo \"Hello world\"");
    shellScriptState.setHost("somehost");
    List<String> strings = shellScriptState.getPatternsForRequiredContextElementType();
    assertThat(strings).isNotEmpty();
    assertThat(strings).contains("echo \"Hello world\"", "somehost");
  }

  @Test
  @Owner(emails = AADITI)
  @Category(UnitTests.class)
  public void shouldExecuteOnDelegate() throws IllegalAccessException {
    Map<String, Object> variableMap = new HashMap<>();
    variableMap.put("var1", "John Doe");
    setFieldsInShellScriptState();
    FieldUtils.writeField(shellScriptState, "executeOnDelegate", true, true);

    when(serviceTemplateService.get(APP_ID, TEMPLATE_ID)).thenReturn(aServiceTemplate().withUuid(TEMPLATE_ID).build());
    when(serviceTemplateService.computeServiceVariables(APP_ID, ENV_ID, TEMPLATE_ID, null, OBTAIN_VALUE))
        .thenReturn(emptyList());

    when(templateUtils.processTemplateVariables(any(), any())).thenReturn(variableMap);
    when(executionContext.getContextElement(ContextElementType.STANDARD)).thenReturn(workflowStandardParams);
    when(executionContext.fetchInfraMappingId()).thenReturn(INFRA_MAPPING_ID);
    when(executionContext.renderExpression(eq("echo ${var1}"), any(StateExecutionContext.class)))
        .thenReturn("echo \"John Doe\"");
    when(workflowStandardParams.getAppId()).thenReturn(APP_ID);
    when(workflowStandardParams.getEnvId()).thenReturn(ENV_ID);
    when(workflowStandardParams.getApp()).thenReturn(anApplication().uuid(APP_ID).name(APP_NAME).build());
    when(workflowStandardParams.getEnv())
        .thenReturn(anEnvironment().uuid(ENV_ID).name(ENV_NAME).environmentType(NON_PROD).build());

    doReturn("TASKID").when(delegateService).queueTask(any());
    ExecutionResponse response = shellScriptState.execute(executionContext);
    assertThat(response).isNotNull().extracting(ExecutionResponse::isAsync).isEqualTo(true);
    assertThat(response.getStateExecutionData())
        .isNotNull()
        .isInstanceOf(ScriptStateExecutionData.class)
        .isEqualToComparingOnlyGivenFields(
            ScriptStateExecutionData.builder().activityId(ACTIVITY_ID).build(), "activityId");
    assertThat(response.getExecutionStatus()).isEqualTo(ExecutionStatus.SUCCESS);
    assertThat(response.getDelegateTaskId()).isEqualTo("TASKID");
    ArgumentCaptor<DelegateTask> captor = ArgumentCaptor.forClass(DelegateTask.class);
    verify(delegateService).queueTask(captor.capture());
    DelegateTask delegateTask = captor.getValue();
    ShellScriptParameters shellScriptParameters = (ShellScriptParameters) delegateTask.getData().getParameters()[0];
    assertThat(shellScriptParameters.getScript()).isEqualTo("echo \"John Doe\"");
    assertThat(shellScriptParameters.getOutputVars()).isEqualTo("var1");
    assertThat(shellScriptParameters.getConnectionType()).isEqualTo(SSH);
    assertThat(shellScriptParameters.getScriptType()).isEqualTo(BASH);
    assertThat(shellScriptParameters.isExecuteOnDelegate()).isTrue();
    assertThat(delegateTask.getTags()).contains("T1", "T2");

    verify(activityHelperService).createAndSaveActivity(any(), any(), any(), any(), any());
  }

  @Test
  @Owner(emails = AADITI)
  @Category(UnitTests.class)
  public void shouldExecuteOnTargetHostSSh() throws IllegalAccessException {
    on(shellScriptState).set("settingsService", settingsService);
    on(shellScriptState).set("secretManager", secretManager);
    Map<String, Object> variableMap = new HashMap<>();
    variableMap.put("var1", "John Doe");
    setFieldsInShellScriptState();
    FieldUtils.writeField(shellScriptState, "executeOnDelegate", false, true);
    FieldUtils.writeField(shellScriptState, "host", "localhost", true);
    FieldUtils.writeField(shellScriptState, "sshKeyRef", "SSH_KEY_REF", true);

    when(serviceTemplateService.get(APP_ID, TEMPLATE_ID)).thenReturn(aServiceTemplate().withUuid(TEMPLATE_ID).build());
    when(serviceTemplateService.computeServiceVariables(APP_ID, ENV_ID, TEMPLATE_ID, null, OBTAIN_VALUE))
        .thenReturn(emptyList());

    when(templateUtils.processTemplateVariables(any(), any())).thenReturn(variableMap);
    when(executionContext.getContextElement(ContextElementType.STANDARD)).thenReturn(workflowStandardParams);
    when(executionContext.fetchInfraMappingId()).thenReturn(INFRA_MAPPING_ID);
    when(executionContext.renderExpression(eq("echo ${var1}"), any(StateExecutionContext.class)))
        .thenReturn("echo \"John Doe\"");
    when(executionContext.renderExpression(eq("localhost"), any(StateExecutionContext.class))).thenReturn("localhost");
    when(workflowStandardParams.getAppId()).thenReturn(APP_ID);
    when(workflowStandardParams.getEnvId()).thenReturn(ENV_ID);
    when(workflowStandardParams.getApp()).thenReturn(anApplication().uuid(APP_ID).name(APP_NAME).build());
    when(workflowStandardParams.getEnv())
        .thenReturn(anEnvironment().uuid(ENV_ID).name(ENV_NAME).environmentType(NON_PROD).build());

    doReturn("TASKID").when(delegateService).queueTask(any());
    HostConnectionAttributes hostConnectionAttributes = HostConnectionAttributes.Builder.aHostConnectionAttributes()
                                                            .withUserName("TestUser")
                                                            .withKeyPath("KEY_PATH")
                                                            .withAccessType(KEY)
                                                            .withAccountId(ACCOUNT_ID)
                                                            .build();
    when(settingsService.get(any()))
        .thenReturn(SettingAttribute.Builder.aSettingAttribute()
                        .withName("SETTING")
                        .withValue(hostConnectionAttributes)
                        .withUuid("UUID")
                        .build());
    when(secretManager.getEncryptionDetails(any(), any(), any())).thenReturn(asList());
    ExecutionResponse response = shellScriptState.execute(executionContext);
    assertThat(response).isNotNull().extracting(ExecutionResponse::isAsync).isEqualTo(true);
    assertThat(response.getStateExecutionData())
        .isNotNull()
        .isInstanceOf(ScriptStateExecutionData.class)
        .isEqualToComparingOnlyGivenFields(
            ScriptStateExecutionData.builder().activityId(ACTIVITY_ID).build(), "activityId");
    assertThat(response.getExecutionStatus()).isEqualTo(ExecutionStatus.SUCCESS);
    assertThat(response.getDelegateTaskId()).isEqualTo("TASKID");

    ArgumentCaptor<DelegateTask> captor = ArgumentCaptor.forClass(DelegateTask.class);
    verify(delegateService).queueTask(captor.capture());
    DelegateTask delegateTask = captor.getValue();
    ShellScriptParameters shellScriptParameters = (ShellScriptParameters) delegateTask.getData().getParameters()[0];
    assertThat(shellScriptParameters.getScript()).isEqualTo("echo \"John Doe\"");
    assertThat(shellScriptParameters.getOutputVars()).isEqualTo("var1");
    assertThat(shellScriptParameters.getHost()).isEqualTo("localhost");
    assertThat(shellScriptParameters.getConnectionType()).isEqualTo(SSH);
    assertThat(shellScriptParameters.getScriptType()).isEqualTo(BASH);
    assertThat(shellScriptParameters.isExecuteOnDelegate()).isFalse();
    assertThat(shellScriptParameters.getHostConnectionAttributes().getUserName()).isEqualTo("TestUser");
    assertThat(shellScriptParameters.getHostConnectionAttributes().getKeyPath()).isEqualTo("KEY_PATH");
    assertThat(shellScriptParameters.getHostConnectionAttributes().getAccessType()).isEqualTo(KEY);
    assertThat(shellScriptParameters.getHostConnectionAttributes().getSshPort()).isEqualTo(22);
    assertThat(delegateTask.getTags()).contains("T1", "T2");
    verify(activityHelperService).createAndSaveActivity(any(), any(), any(), any(), any());
  }

  private void setFieldsInShellScriptState() throws IllegalAccessException {
    on(shellScriptState).set("templateUtils", templateUtils);
    on(shellScriptState).set("activityHelperService", activityHelperService);
    on(shellScriptState).set("infrastructureMappingService", infrastructureMappingService);
    on(shellScriptState).set("serviceTemplateService", serviceTemplateService);
    on(shellScriptState).set("delegateService", delegateService);
    FieldUtils.writeField(shellScriptState, "scriptString", "echo ${var1}", true);
    FieldUtils.writeField(shellScriptState, "sweepingOutputName", "out1", true);
    FieldUtils.writeField(shellScriptState, "outputVars", "var1", true);
    FieldUtils.writeField(shellScriptState, "sweepingOutputScope", WORKFLOW, true);
    FieldUtils.writeField(shellScriptState, "tags", asList("T1", "T2"), true);
  }
}
