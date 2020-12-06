package software.wings.sm.states.k8s;

import static io.harness.delegate.task.k8s.K8sTaskType.DELETE;
import static io.harness.rule.OwnerRule.BOJANA;
import static io.harness.rule.OwnerRule.SAHIL;

import static software.wings.beans.GcpKubernetesInfrastructureMapping.Builder.aGcpKubernetesInfrastructureMapping;
import static software.wings.sm.StateExecutionInstance.Builder.aStateExecutionInstance;
import static software.wings.sm.StateType.K8S_DELETE;
import static software.wings.sm.states.k8s.K8sDelete.K8S_DELETE_COMMAND_NAME;
import static software.wings.utils.WingsTestConstants.ACTIVITY_ID;
import static software.wings.utils.WingsTestConstants.STATE_NAME;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.beans.ExecutionStatus;
import io.harness.category.element.UnitTests;
import io.harness.context.ContextElementType;
import io.harness.expression.VariableResolverTracker;
import io.harness.k8s.K8sCommandUnitConstants;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import io.harness.tasks.ResponseData;

import software.wings.WingsBaseTest;
import software.wings.api.k8s.K8sStateExecutionData;
import software.wings.beans.Activity;
import software.wings.beans.DirectKubernetesInfrastructureMapping;
import software.wings.beans.appmanifest.AppManifestKind;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.command.K8sDummyCommandUnit;
import software.wings.common.VariableProcessor;
import software.wings.expression.ManagerExpressionEvaluator;
import software.wings.helpers.ext.k8s.request.K8sDelegateManifestConfig;
import software.wings.helpers.ext.k8s.request.K8sDeleteTaskParameters;
import software.wings.helpers.ext.k8s.request.K8sTaskParameters;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.AppService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.WorkflowStandardParams;
import software.wings.utils.ApplicationManifestUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class K8sDeleteTest extends WingsBaseTest {
  private static final String RELEASE_NAME = "releaseName";
  private static final String FILE_PATHS = "abc/xyz";
  private static final String RESOURCES = "*";

  @Mock private K8sStateHelper k8sStateHelper;
  @Mock private ApplicationManifestUtils applicationManifestUtils;
  @Mock private VariableProcessor variableProcessor;
  @Mock private ManagerExpressionEvaluator evaluator;
  @Mock private AppService appService;
  @Mock private ActivityService activityService;
  @InjectMocks K8sDelete k8sDelete;

  private StateExecutionInstance stateExecutionInstance = aStateExecutionInstance().displayName(STATE_NAME).build();

  @Mock private ExecutionContextImpl context;

  @Before
  public void setup() {
    k8sDelete.setFilePaths(FILE_PATHS);
    k8sDelete.setDeleteNamespacesForRelease(true);
    k8sDelete.setResources(RESOURCES);
    when(variableProcessor.getVariables(any(), any())).thenReturn(emptyMap());
    when(evaluator.substitute(anyString(), anyMap(), any(VariableResolverTracker.class), anyString()))
        .thenAnswer(i -> i.getArguments()[0]);
    doReturn(Activity.builder().uuid("activity-id").build())
        .when(k8sStateHelper)
        .createK8sActivity(any(), eq(K8S_DELETE_COMMAND_NAME), anyString(), any(ActivityService.class), anyList());
    doReturn(new DirectKubernetesInfrastructureMapping())
        .when(k8sStateHelper)
        .getContainerInfrastructureMapping(context);
    doReturn("release-name")
        .when(k8sStateHelper)
        .getReleaseName(eq(context), eq(new DirectKubernetesInfrastructureMapping()));
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void executeWithoutManifestDeleteNamespace() {
    doReturn("Deployment/test").when(context).renderExpression("${workflow.variables.resources}");
    k8sDelete.setResources("${workflow.variables.resources}");
    k8sDelete.setFilePaths(null);
    k8sDelete.setDeleteNamespacesForRelease(true);

    k8sDelete.execute(context);

    ArgumentCaptor<List> listArgumentCaptor = ArgumentCaptor.forClass(List.class);
    verify(k8sStateHelper, never()).executeWrapperWithManifest(any(), any(), anyLong());
    verify(k8sStateHelper, times(1))
        .createK8sActivity(eq(context), eq(K8S_DELETE_COMMAND_NAME), eq("K8S_DELETE"), any(ActivityService.class),
            listArgumentCaptor.capture());

    @SuppressWarnings("unchecked") List<K8sDummyCommandUnit> commandUnits = listArgumentCaptor.getValue();
    assertThat(commandUnits.stream().map(K8sDummyCommandUnit::getName).collect(Collectors.toList()))
        .containsExactly(K8sCommandUnitConstants.Init, K8sCommandUnitConstants.Delete);

    verify(k8sStateHelper, times(1))
        .queueK8sDelegateTask(eq(context),
            eq(K8sDeleteTaskParameters.builder()
                    .releaseName("release-name")
                    .resources("Deployment/test")
                    .filePaths(null)
                    .activityId("activity-id")
                    .commandName(K8S_DELETE_COMMAND_NAME)
                    .k8sTaskType(DELETE)
                    .deleteNamespacesForRelease(true)
                    .timeoutIntervalInMin(10)
                    .build()));
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void executeWithoutManifestNotDeleteNamespace() {
    doReturn("Deployment/test").when(context).renderExpression("${workflow.variables.resources}");
    k8sDelete.setResources("${workflow.variables.resources}");
    k8sDelete.setFilePaths(null);
    k8sDelete.setDeleteNamespacesForRelease(false);

    k8sDelete.execute(context);

    ArgumentCaptor<List> listArgumentCaptor = ArgumentCaptor.forClass(List.class);
    verify(k8sStateHelper, never()).executeWrapperWithManifest(any(), any(), anyLong());
    verify(k8sStateHelper, times(1))
        .createK8sActivity(eq(context), eq(K8S_DELETE_COMMAND_NAME), eq("K8S_DELETE"), any(ActivityService.class),
            listArgumentCaptor.capture());

    @SuppressWarnings("unchecked") List<K8sDummyCommandUnit> commandUnits = listArgumentCaptor.getValue();
    assertThat(commandUnits.stream().map(K8sDummyCommandUnit::getName).collect(Collectors.toList()))
        .containsExactly(K8sCommandUnitConstants.Init, K8sCommandUnitConstants.Delete);

    verify(k8sStateHelper, times(1))
        .queueK8sDelegateTask(eq(context),
            eq(K8sDeleteTaskParameters.builder()
                    .releaseName("release-name")
                    .resources("Deployment/test")
                    .filePaths(null)
                    .activityId("activity-id")
                    .commandName(K8S_DELETE_COMMAND_NAME)
                    .k8sTaskType(DELETE)
                    .deleteNamespacesForRelease(false)
                    .timeoutIntervalInMin(10)
                    .build()));
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void executeWithManifest() {
    k8sDelete.setFilePaths("templates/foo.yaml");
    k8sDelete.execute(context);
    verify(k8sStateHelper, times(1)).executeWrapperWithManifest(k8sDelete, context, 10 * 60 * 1000L);
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testExecute() {
    k8sDelete.setFilePaths(FILE_PATHS);
    on(context).set("variableProcessor", variableProcessor);
    on(context).set("evaluator", evaluator);

    when(applicationManifestUtils.getApplicationManifests(context, AppManifestKind.VALUES)).thenReturn(new HashMap<>());
    when(k8sStateHelper.getContainerInfrastructureMapping(context))
        .thenReturn(aGcpKubernetesInfrastructureMapping().build());
    when(k8sStateHelper.getReleaseName(any(), any())).thenReturn(RELEASE_NAME);
    when(k8sStateHelper.createDelegateManifestConfig(any(), any()))
        .thenReturn(K8sDelegateManifestConfig.builder().build());
    when(k8sStateHelper.getRenderedValuesFiles(any(), any())).thenReturn(Collections.emptyList());
    when(k8sStateHelper.queueK8sDelegateTask(any(), any())).thenReturn(ExecutionResponse.builder().build());
    doReturn("abc/xyz").when(context).renderExpression("abc/xyz");
    doReturn("*").when(context).renderExpression("*");

    k8sDelete.executeK8sTask(context, ACTIVITY_ID);

    ArgumentCaptor<K8sTaskParameters> k8sDeleteTaskParamsArgumentCaptor =
        ArgumentCaptor.forClass(K8sTaskParameters.class);
    verify(k8sStateHelper, times(1)).queueK8sDelegateTask(any(), k8sDeleteTaskParamsArgumentCaptor.capture());
    K8sDeleteTaskParameters taskParams = (K8sDeleteTaskParameters) k8sDeleteTaskParamsArgumentCaptor.getValue();

    assertThat(taskParams.getReleaseName()).isEqualTo(RELEASE_NAME);
    assertThat(taskParams.getActivityId()).isEqualTo(ACTIVITY_ID);
    assertThat(taskParams.getCommandType()).isEqualTo(DELETE);
    assertThat(taskParams.getCommandName()).isEqualTo(K8S_DELETE_COMMAND_NAME);
    assertThat(taskParams.getFilePaths()).isEqualTo(FILE_PATHS);
    assertThat(taskParams.isDeleteNamespacesForRelease()).isEqualTo(true);
    assertThat(taskParams.getResources()).isEqualTo(RESOURCES);
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void testHandleAsyncResponse() {
    k8sDelete.handleAsyncResponse(context, new HashMap<>());
    verify(k8sStateHelper, times(1))
        .handleAsyncResponseWrapper(any(K8sStateExecutor.class), any(ExecutionContext.class), anyMap());
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void testValidateParameters() {
    K8sDelete k8sDeleteSpy = spy(k8sDelete);
    k8sDeleteSpy.validateParameters(context);
    verify(k8sDeleteSpy, times(1)).validateParameters(any(ExecutionContext.class));
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void testValidateFields() {
    String RESOURCES_KEY = "resources";
    k8sDelete.setFilePaths(null);
    k8sDelete.setResources(null);
    Map<String, String> invalidFields = k8sDelete.validateFields();
    assertThat(invalidFields).isNotEmpty();
    assertThat(invalidFields.size()).isEqualTo(1);
    assertThat(invalidFields).containsKeys(RESOURCES_KEY);
    assertThat(invalidFields.get(RESOURCES_KEY)).isEqualTo("Resources must not be blank");
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void testCommandName() {
    String commandName = k8sDelete.commandName();
    assertThat(commandName).isEqualTo(K8S_DELETE_COMMAND_NAME);
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void testStateType() {
    String stateType = k8sDelete.stateType();
    assertThat(stateType).isEqualTo(K8S_DELETE.name());
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void testCommandUnitList() {
    List<CommandUnit> applyCommandUnits = k8sDelete.commandUnitList(true);
    assertThat(applyCommandUnits).isNotEmpty();
    assertThat(applyCommandUnits.get(0).getName()).isEqualTo(K8sCommandUnitConstants.FetchFiles);
    assertThat(applyCommandUnits.get(1).getName()).isEqualTo(K8sCommandUnitConstants.Init);
    assertThat(applyCommandUnits.get(applyCommandUnits.size() - 1).getName()).isEqualTo(K8sCommandUnitConstants.Delete);
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void testHandleAsyncResponseForK8sTask() {
    WorkflowStandardParams workflowStandardParams = new WorkflowStandardParams();
    when(context.getContextElement(any(ContextElementType.class))).thenReturn(workflowStandardParams);

    K8sTaskExecutionResponse k8sTaskExecutionResponse = K8sTaskExecutionResponse.builder().build();
    Map<String, ResponseData> response = new HashMap<>();
    response.put("k8sTaskExecutionResponse", k8sTaskExecutionResponse);

    when(context.getStateExecutionData()).thenReturn(new K8sStateExecutionData());

    k8sDelete.handleAsyncResponseForK8sTask(context, response);
    verify(activityService, times(1)).updateStatus(anyString(), anyString(), any(ExecutionStatus.class));
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void testHandleAbortEvent() {
    K8sDelete k8sDeleteSpy = spy(k8sDelete);
    k8sDeleteSpy.handleAbortEvent(context);
    verify(k8sDeleteSpy, times(1)).handleAbortEvent(any(ExecutionContext.class));
  }

  @Test
  @Owner(developers = BOJANA)
  @Category(UnitTests.class)
  public void testGetTimeoutMillis() {
    k8sDelete.setStateTimeoutInMinutes(1);
    Integer timeout = k8sDelete.getTimeoutMillis();
    assertThat(timeout).isEqualTo(60000);
  }
}
