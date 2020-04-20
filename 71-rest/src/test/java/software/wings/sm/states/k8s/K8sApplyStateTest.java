package software.wings.sm.states.k8s;

import static io.harness.rule.OwnerRule.ANSHUL;
import static io.harness.rule.OwnerRule.YOGESH;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.GcpKubernetesInfrastructureMapping.Builder.aGcpKubernetesInfrastructureMapping;
import static software.wings.helpers.ext.k8s.request.K8sTaskParameters.K8sTaskType.APPLY;
import static software.wings.sm.StateExecutionInstance.Builder.aStateExecutionInstance;
import static software.wings.sm.states.k8s.K8sApplyState.K8S_APPLY_STATE;
import static software.wings.utils.WingsTestConstants.ACTIVITY_ID;
import static software.wings.utils.WingsTestConstants.STATE_NAME;

import io.harness.category.element.UnitTests;
import io.harness.expression.VariableResolverTracker;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.appmanifest.AppManifestKind;
import software.wings.common.VariableProcessor;
import software.wings.expression.ManagerExpressionEvaluator;
import software.wings.helpers.ext.k8s.request.K8sApplyTaskParameters;
import software.wings.helpers.ext.k8s.request.K8sDelegateManifestConfig;
import software.wings.helpers.ext.k8s.request.K8sTaskParameters;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.StateExecutionInstance;
import software.wings.utils.ApplicationManifestUtils;

import java.util.Collections;
import java.util.HashMap;

public class K8sApplyStateTest extends WingsBaseTest {
  private static final String RELEASE_NAME = "releaseName";
  private static final String FILE_PATHS = "abc/xyz";

  @Mock private K8sStateHelper k8sStateHelper;
  @Mock private ApplicationManifestUtils applicationManifestUtils;
  @Mock private VariableProcessor variableProcessor;
  @Mock private ManagerExpressionEvaluator evaluator;
  @InjectMocks K8sApplyState k8sApplyState;

  private StateExecutionInstance stateExecutionInstance = aStateExecutionInstance().displayName(STATE_NAME).build();

  private ExecutionContextImpl context;

  @Before
  public void setup() {
    context = new ExecutionContextImpl(stateExecutionInstance);
    k8sApplyState.setStateTimeoutInMinutes("10");
    k8sApplyState.setSkipDryRun(true);
    k8sApplyState.setFilePaths(FILE_PATHS);

    when(variableProcessor.getVariables(any(), any())).thenReturn(emptyMap());
    when(evaluator.substitute(anyString(), anyMap(), any(VariableResolverTracker.class), anyString()))
        .thenAnswer(i -> i.getArguments()[0]);
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testExecute() {
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

    k8sApplyState.executeK8sTask(context, ACTIVITY_ID);

    ArgumentCaptor<K8sTaskParameters> k8sApplyTaskParamsArgumentCaptor =
        ArgumentCaptor.forClass(K8sTaskParameters.class);
    verify(k8sStateHelper, times(1)).queueK8sDelegateTask(any(), k8sApplyTaskParamsArgumentCaptor.capture());
    K8sApplyTaskParameters taskParams = (K8sApplyTaskParameters) k8sApplyTaskParamsArgumentCaptor.getValue();

    assertThat(taskParams.getReleaseName()).isEqualTo(RELEASE_NAME);
    assertThat(taskParams.getActivityId()).isEqualTo(ACTIVITY_ID);
    assertThat(taskParams.getCommandType()).isEqualTo(APPLY);
    assertThat(taskParams.getCommandName()).isEqualTo(K8S_APPLY_STATE);
    assertThat(taskParams.getTimeoutIntervalInMin()).isEqualTo(10);
    assertThat(taskParams.isSkipDryRun()).isTrue();
    assertThat(taskParams.getFilePaths()).isEqualTo(FILE_PATHS);
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testTimeoutValue() {
    K8sApplyState state = new K8sApplyState("k8s-apply");
    assertThat(state.getTimeoutMillis()).isNull();

    state.setStateTimeoutInMinutes("5");
    assertThat(state.getTimeoutMillis()).isEqualTo(300000);

    state.setStateTimeoutInMinutes("foo");
    assertThat(state.getTimeoutMillis()).isNull();
  }
}
