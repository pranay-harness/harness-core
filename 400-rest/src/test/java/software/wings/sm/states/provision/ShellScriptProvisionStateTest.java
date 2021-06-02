package software.wings.sm.states.provision;

import static io.harness.beans.EnvironmentType.ALL;
import static io.harness.rule.OwnerRule.ABHINAV;
import static io.harness.rule.OwnerRule.ABOSII;
import static io.harness.rule.OwnerRule.PARDHA;
import static io.harness.rule.OwnerRule.VAIBHAV_SI;

import static software.wings.beans.Environment.GLOBAL_ENV_ID;
import static software.wings.utils.WingsTestConstants.ACTIVITY_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.ENV_NAME;
import static software.wings.utils.WingsTestConstants.PROVISIONER_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_EXECUTION_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_NAME;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.beans.DelegateTask;
import io.harness.beans.EnvironmentType;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.OrchestrationWorkflowType;
import io.harness.beans.SweepingOutputInstance;
import io.harness.beans.SweepingOutputInstance.Scope;
import io.harness.beans.WorkflowType;
import io.harness.category.element.UnitTests;
import io.harness.context.ContextElementType;
import io.harness.rule.Owner;
import io.harness.serializer.KryoSerializer;
import io.harness.tasks.ResponseData;

import software.wings.WingsBaseTest;
import software.wings.api.ShellScriptProvisionerOutputElement;
import software.wings.api.shellscript.provision.ShellScriptProvisionExecutionData;
import software.wings.beans.Activity;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.InfrastructureProvisionerType;
import software.wings.beans.shellscript.provisioner.ShellScriptInfrastructureProvisioner;
import software.wings.beans.shellscript.provisioner.ShellScriptProvisionParameters;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.InfrastructureProvisionerService;
import software.wings.service.intfc.StateExecutionService;
import software.wings.service.intfc.sweepingoutput.SweepingOutputService;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.states.ManagerExecutionLogCallback;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.joor.Reflect;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class ShellScriptProvisionStateTest extends WingsBaseTest {
  @Mock private DelegateService delegateService;
  @Mock private ActivityService activityService;
  @Mock private InfrastructureProvisionerService infrastructureProvisionerService;
  @Mock private SweepingOutputService sweepingOutputService;
  @Mock private ExecutionContextImpl executionContext;
  @Mock private StateExecutionService stateExecutionService;
  @Inject private KryoSerializer kryoSerializer;

  @InjectMocks
  private ShellScriptProvisionState state =
      new ShellScriptProvisionState(InfrastructureProvisionerType.SHELL_SCRIPT.name());

  @Before
  public void setUp() throws Exception {
    doNothing().when(stateExecutionService).appendDelegateTaskDetails(anyString(), any());
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testParseOutput() {
    assertThat(state.parseOutput(null)).isEqualTo(Collections.emptyMap());
    assertThat(state.parseOutput("")).isEqualTo(Collections.emptyMap());

    String json = "{\n"
        + "\t\"key1\":\"val1\",\n"
        + "\t\"key2\":\"val2\"\n"
        + "}";
    Map<String, Object> expectedMap = new LinkedHashMap<>();
    expectedMap.put("key1", "val1");
    expectedMap.put("key2", "val2");
    assertThat(state.parseOutput(json)).isEqualTo(expectedMap);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testValidation() {
    assertThat(state.validateFields().size()).isNotEqualTo(0);
    state.setProvisionerId("test provisioner");
    assertThat(state.validateFields().size()).isEqualTo(0);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void shouldPopulateWorkflowExecutionIdParamFromExecutionContext() {
    ExecutionContextImpl executionContext = mock(ExecutionContextImpl.class);
    ArgumentCaptor<DelegateTask> delegateTaskArgumentCaptor = ArgumentCaptor.forClass(DelegateTask.class);

    when(activityService.save(any())).thenReturn(mock(Activity.class));
    when(executionContext.getApp()).thenReturn(mock(Application.class));
    when(executionContext.getEnv()).thenReturn(mock(Environment.class));
    when(infrastructureProvisionerService.getShellScriptProvisioner(anyString(), anyString()))
        .thenReturn(mock(ShellScriptInfrastructureProvisioner.class));
    when(executionContext.getWorkflowExecutionId()).thenReturn("workflow-execution-id");
    state.execute(executionContext);

    verify(delegateService).queueTask(delegateTaskArgumentCaptor.capture());
    ShellScriptProvisionParameters populatedParameters =
        (ShellScriptProvisionParameters) delegateTaskArgumentCaptor.getValue().getData().getParameters()[0];
    assertThat(populatedParameters.getWorkflowExecutionId()).isEqualTo("workflow-execution-id");
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testHandleAsyncResponseFailedResponse() {
    ShellScriptProvisionExecutionData executionData = ShellScriptProvisionExecutionData.builder()
                                                          .executionStatus(ExecutionStatus.ERROR)
                                                          .errorMsg("Error during execution")
                                                          .build();
    Map<String, ResponseData> responseData = ImmutableMap.of(ACTIVITY_ID, executionData);

    doReturn(APP_ID).when(executionContext).getAppId();
    doReturn(null).when(executionContext).getContextElement(ContextElementType.SHELL_SCRIPT_PROVISION);
    ExecutionResponse response = state.handleAsyncResponse(executionContext, responseData);
    verify(activityService, times(1)).updateStatus(ACTIVITY_ID, APP_ID, ExecutionStatus.ERROR);
    assertThat(response.getExecutionStatus()).isEqualTo(ExecutionStatus.ERROR);
    assertThat(response.getErrorMessage()).isEqualTo("Error during execution");
    assertThat(response.getContextElements().get(0)).isNotNull(); // Should create new OutputElement
    assertThat(response.getNotifyElements().get(0)).isNotNull(); // Should create new OutputElement

    ShellScriptProvisionerOutputElement outputElement = ShellScriptProvisionerOutputElement.builder().build();
    doReturn(outputElement).when(executionContext).getContextElement(ContextElementType.SHELL_SCRIPT_PROVISION);
    response = state.handleAsyncResponse(executionContext, responseData);
    assertThat(response.getContextElements().get(0)).isSameAs(outputElement); // Reuse existing OutputElement
    assertThat(response.getNotifyElements().get(0)).isSameAs(outputElement); // Reuse existing OutputElement
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testHandleAsyncResponse() {
    Reflect.on(state).set("kryoSerializer", kryoSerializer);
    state.setProvisionerId(PROVISIONER_ID);
    state.setSweepingOutputName("${expression}");
    state.setSweepingOutputScope(Scope.PHASE);
    ShellScriptProvisionExecutionData executionData = ShellScriptProvisionExecutionData.builder()
                                                          .executionStatus(ExecutionStatus.SUCCESS)
                                                          .output("{\"key\": \"value\"}")
                                                          .build();
    Map<String, ResponseData> responseData = ImmutableMap.of(ACTIVITY_ID, executionData);
    ManagerExecutionLogCallback logCallback = mock(ManagerExecutionLogCallback.class);
    Map<String, Object> expectedOutputMap = ImmutableMap.of("key", "value");
    ShellScriptProvisionerOutputElement outputElement = ShellScriptProvisionerOutputElement.builder().build();

    doReturn(outputElement).when(executionContext).getContextElement(ContextElementType.SHELL_SCRIPT_PROVISION);
    doReturn(APP_ID).when(executionContext).getAppId();
    doReturn("rendered-expression").when(executionContext).renderExpression("${expression}");
    doReturn(SweepingOutputInstance.builder()).when(executionContext).prepareSweepingOutputBuilder(Scope.PHASE);
    doReturn(logCallback)
        .when(infrastructureProvisionerService)
        .getManagerExecutionCallback(eq(APP_ID), eq(ACTIVITY_ID), anyString());
    ExecutionResponse response = state.handleAsyncResponse(executionContext, responseData);

    ArgumentCaptor<SweepingOutputInstance> instanceCaptor = ArgumentCaptor.forClass(SweepingOutputInstance.class);
    verify(infrastructureProvisionerService, times(1))
        .regenerateInfrastructureMappings(
            PROVISIONER_ID, executionContext, expectedOutputMap, Optional.of(logCallback), Optional.empty());
    verify(sweepingOutputService, times(1)).save(instanceCaptor.capture());
    verify(activityService, times(1)).updateStatus(ACTIVITY_ID, APP_ID, ExecutionStatus.SUCCESS);

    assertThat(instanceCaptor.getValue().getName()).isEqualTo("rendered-expression");
    assertThat(response.getExecutionStatus()).isEqualTo(ExecutionStatus.SUCCESS);
    assertThat(response.getContextElements().get(0)).isEqualTo(outputElement);
    assertThat(response.getNotifyElements().get(0)).isEqualTo(outputElement);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testCreateActivity() {
    state.setProvisionerId(PROVISIONER_ID);
    ShellScriptInfrastructureProvisioner provisioner = ShellScriptInfrastructureProvisioner.builder().build();
    Environment env = Environment.Builder.anEnvironment()
                          .uuid(ENV_ID)
                          .name(ENV_NAME)
                          .environmentType(EnvironmentType.NON_PROD)
                          .build();

    doReturn(env).when(executionContext).getEnv();
    doReturn(APP_ID).when(executionContext).getAppId();
    doReturn(WorkflowType.ORCHESTRATION).when(executionContext).getWorkflowType();
    doReturn(WORKFLOW_EXECUTION_ID).when(executionContext).getWorkflowExecutionId();
    doReturn(WORKFLOW_NAME).when(executionContext).getWorkflowExecutionName();
    doReturn(Application.Builder.anApplication().uuid(APP_ID).build()).when(executionContext).getApp();
    doAnswer(invocation -> invocation.getArgumentAt(0, Activity.class)).when(activityService).save(any(Activity.class));
    doReturn(provisioner).when(infrastructureProvisionerService).getShellScriptProvisioner(APP_ID, PROVISIONER_ID);
    ArgumentCaptor<Activity> activityCaptor = ArgumentCaptor.forClass(Activity.class);
    ArgumentCaptor<DelegateTask> delegateTaskArgumentCaptor = ArgumentCaptor.forClass(DelegateTask.class);

    // When OrchestrationWorkflowType is BUILD
    doReturn(OrchestrationWorkflowType.BUILD).when(executionContext).getOrchestrationWorkflowType();
    state.execute(executionContext);
    verify(activityService, times(1)).save(activityCaptor.capture());
    assertCreatedActivity(activityCaptor.getValue(), GLOBAL_ENV_ID, GLOBAL_ENV_ID, ALL);

    verify(delegateService).queueTask(delegateTaskArgumentCaptor.capture());
    assertThat(delegateTaskArgumentCaptor.getValue().getData().getExpressionFunctorToken()).isNotNull();

    // When OrchestrationWorkflowType is other than BUILD
    doReturn(OrchestrationWorkflowType.BASIC).when(executionContext).getOrchestrationWorkflowType();
    state.execute(executionContext);
    // 1 time from previous invocation
    verify(activityService, times(2)).save(activityCaptor.capture());
    assertCreatedActivity(activityCaptor.getValue(), ENV_NAME, ENV_ID, EnvironmentType.NON_PROD);
  }

  private void assertCreatedActivity(Activity activity, String envName, String envId, EnvironmentType envType) {
    assertThat(activity.getWorkflowType()).isEqualTo(WorkflowType.ORCHESTRATION);
    assertThat(activity.getWorkflowExecutionId()).isEqualTo(WORKFLOW_EXECUTION_ID);
    assertThat(activity.getWorkflowExecutionName()).isEqualTo(WORKFLOW_NAME);
    assertThat(activity.getStatus()).isEqualTo(ExecutionStatus.RUNNING);
    assertThat(activity.getEnvironmentName()).isEqualTo(envName);
    assertThat(activity.getEnvironmentId()).isEqualTo(envId);
    assertThat(activity.getEnvironmentType()).isEqualTo(envType);
  }

  @Test
  @Owner(developers = PARDHA)
  @Category(UnitTests.class)
  public void shouldPopulateRenderedDelegateSelectorsFromExecutionContext() {
    final String runTimeValueAbc = "runTimeValueAbc";
    state.setDelegateSelectors(Collections.singletonList("${workflow.variables.abc}"));
    ExecutionContextImpl executionContext = mock(ExecutionContextImpl.class);
    ArgumentCaptor<DelegateTask> delegateTaskArgumentCaptor = ArgumentCaptor.forClass(DelegateTask.class);

    when(activityService.save(any())).thenReturn(mock(Activity.class));
    when(executionContext.getApp()).thenReturn(mock(Application.class));
    when(executionContext.getEnv()).thenReturn(mock(Environment.class));
    when(infrastructureProvisionerService.getShellScriptProvisioner(anyString(), anyString()))
        .thenReturn(mock(ShellScriptInfrastructureProvisioner.class));
    when(executionContext.renderExpression(anyString())).thenReturn(runTimeValueAbc);
    state.execute(executionContext);

    verify(delegateService).queueTask(delegateTaskArgumentCaptor.capture());
    ShellScriptProvisionParameters populatedParameters =
        (ShellScriptProvisionParameters) delegateTaskArgumentCaptor.getValue().getData().getParameters()[0];
    assertThat(populatedParameters.getDelegateSelectors()).isEqualTo(Collections.singletonList(runTimeValueAbc));
  }
}
