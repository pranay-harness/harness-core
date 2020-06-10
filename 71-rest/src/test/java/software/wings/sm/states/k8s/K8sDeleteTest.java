package software.wings.sm.states.k8s;

import static io.harness.rule.OwnerRule.SAHIL;
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
import static software.wings.helpers.ext.k8s.request.K8sTaskParameters.K8sTaskType.DELETE;
import static software.wings.sm.StateExecutionInstance.Builder.aStateExecutionInstance;
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
import software.wings.helpers.ext.k8s.request.K8sDelegateManifestConfig;
import software.wings.helpers.ext.k8s.request.K8sDeleteTaskParameters;
import software.wings.helpers.ext.k8s.request.K8sTaskParameters;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.StateExecutionInstance;
import software.wings.utils.ApplicationManifestUtils;

import java.util.Collections;
import java.util.HashMap;

public class K8sDeleteTest extends WingsBaseTest {
  private static final String RELEASE_NAME = "releaseName";
  private static final String FILE_PATHS = "abc/xyz";

  @Mock private K8sStateHelper k8sStateHelper;
  @Mock private ApplicationManifestUtils applicationManifestUtils;
  @Mock private VariableProcessor variableProcessor;
  @Mock private ManagerExpressionEvaluator evaluator;
  @InjectMocks K8sDelete k8sDelete;

  private StateExecutionInstance stateExecutionInstance = aStateExecutionInstance().displayName(STATE_NAME).build();

  private ExecutionContextImpl context;

  @Before
  public void setup() {
    context = new ExecutionContextImpl(stateExecutionInstance);
    k8sDelete.setFilePaths(FILE_PATHS);

    when(variableProcessor.getVariables(any(), any())).thenReturn(emptyMap());
    when(evaluator.substitute(anyString(), anyMap(), any(VariableResolverTracker.class), anyString()))
        .thenAnswer(i -> i.getArguments()[0]);
  }

  @Test
  @Owner(developers = SAHIL)
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

    k8sDelete.executeK8sTask(context, ACTIVITY_ID);

    ArgumentCaptor<K8sTaskParameters> k8sDeleteTaskParamsArgumentCaptor =
        ArgumentCaptor.forClass(K8sTaskParameters.class);
    verify(k8sStateHelper, times(1)).queueK8sDelegateTask(any(), k8sDeleteTaskParamsArgumentCaptor.capture());
    K8sDeleteTaskParameters taskParams = (K8sDeleteTaskParameters) k8sDeleteTaskParamsArgumentCaptor.getValue();

    assertThat(taskParams.getReleaseName()).isEqualTo(RELEASE_NAME);
    assertThat(taskParams.getActivityId()).isEqualTo(ACTIVITY_ID);
    assertThat(taskParams.getCommandType()).isEqualTo(DELETE);
    assertThat(taskParams.getCommandName()).isEqualTo(K8sDelete.K8S_DELETE_COMMAND_NAME);
    assertThat(taskParams.getFilePaths()).isEqualTo(FILE_PATHS);
  }
}