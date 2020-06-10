package software.wings.sm.states.k8s;

import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.SUCCESS;
import static io.harness.rule.OwnerRule.ABOSII;
import static io.harness.rule.OwnerRule.ANSHUL;
import static io.harness.rule.OwnerRule.YOGESH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.GcpKubernetesInfrastructureMapping.Builder.aGcpKubernetesInfrastructureMapping;
import static software.wings.helpers.ext.k8s.request.K8sTaskParameters.K8sTaskType.BLUE_GREEN_DEPLOY;
import static software.wings.sm.StateExecutionInstance.Builder.aStateExecutionInstance;
import static software.wings.sm.states.k8s.K8sBlueGreenDeploy.K8S_BLUE_GREEN_DEPLOY_COMMAND_NAME;
import static software.wings.utils.WingsTestConstants.ACTIVITY_ID;
import static software.wings.utils.WingsTestConstants.STATE_NAME;

import com.google.common.collect.ImmutableMap;

import io.harness.category.element.UnitTests;
import io.harness.k8s.model.K8sPod;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.api.InstanceElementListParam;
import software.wings.api.k8s.K8sStateExecutionData;
import software.wings.beans.appmanifest.AppManifestKind;
import software.wings.common.VariableProcessor;
import software.wings.expression.ManagerExpressionEvaluator;
import software.wings.helpers.ext.helm.response.HelmChartInfo;
import software.wings.helpers.ext.k8s.request.K8sBlueGreenDeployTaskParameters;
import software.wings.helpers.ext.k8s.request.K8sDelegateManifestConfig;
import software.wings.helpers.ext.k8s.request.K8sTaskParameters;
import software.wings.helpers.ext.k8s.response.K8sBlueGreenDeployResponse;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;
import software.wings.service.intfc.ActivityService;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.StateExecutionInstance;
import software.wings.utils.ApplicationManifestUtils;

import java.util.Collections;
import java.util.HashMap;

public class K8sBlueGreenDeployTest extends WingsBaseTest {
  private static final String RELEASE_NAME = "releaseName";

  @Mock private K8sStateHelper k8sStateHelper;
  @Mock private ApplicationManifestUtils applicationManifestUtils;
  @Mock private VariableProcessor variableProcessor;
  @Mock private ManagerExpressionEvaluator evaluator;
  @Mock private ActivityService activityService;
  @InjectMocks private K8sBlueGreenDeploy k8sBlueGreenDeploy;

  private StateExecutionInstance stateExecutionInstance = aStateExecutionInstance().displayName(STATE_NAME).build();

  private ExecutionContextImpl context;

  @Before
  public void setup() {
    context = new ExecutionContextImpl(stateExecutionInstance);
    k8sBlueGreenDeploy.setStateTimeoutInMinutes(10);
    k8sBlueGreenDeploy.setSkipDryRun(true);
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

    k8sBlueGreenDeploy.executeK8sTask(context, ACTIVITY_ID);

    ArgumentCaptor<K8sTaskParameters> k8sApplyTaskParamsArgumentCaptor =
        ArgumentCaptor.forClass(K8sTaskParameters.class);
    verify(k8sStateHelper, times(1)).queueK8sDelegateTask(any(), k8sApplyTaskParamsArgumentCaptor.capture());
    K8sBlueGreenDeployTaskParameters taskParams =
        (K8sBlueGreenDeployTaskParameters) k8sApplyTaskParamsArgumentCaptor.getValue();

    assertThat(taskParams.getReleaseName()).isEqualTo(RELEASE_NAME);
    assertThat(taskParams.getActivityId()).isEqualTo(ACTIVITY_ID);
    assertThat(taskParams.getCommandType()).isEqualTo(BLUE_GREEN_DEPLOY);
    assertThat(taskParams.getCommandName()).isEqualTo(K8S_BLUE_GREEN_DEPLOY_COMMAND_NAME);
    assertThat(taskParams.getTimeoutIntervalInMin()).isEqualTo(10);
    assertThat(taskParams.isSkipDryRun()).isTrue();
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testTimeoutValue() {
    K8sBlueGreenDeploy state = new K8sBlueGreenDeploy("k8s-bg");
    assertThat(state.getTimeoutMillis()).isNull();

    state.setStateTimeoutInMinutes(5);
    assertThat(state.getTimeoutMillis()).isEqualTo(300000);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testHelmChartInfoValue() {
    stateExecutionInstance.setStateExecutionMap(ImmutableMap.of(STATE_NAME, new K8sStateExecutionData()));
    HelmChartInfo helmChartInfo = HelmChartInfo.builder().name("chart").version("1.0.0").build();
    K8sTaskExecutionResponse taskExecutionResponse =
        K8sTaskExecutionResponse.builder()
            .commandExecutionStatus(SUCCESS)
            .k8sTaskResponse(K8sBlueGreenDeployResponse.builder().helmChartInfo(helmChartInfo).build())
            .build();

    doReturn(InstanceElementListParam.builder().build())
        .when(k8sStateHelper)
        .getInstanceElementListParam(anyListOf(K8sPod.class));
    ExecutionResponse executionResponse =
        k8sBlueGreenDeploy.handleAsyncResponseForK8sTask(context, ImmutableMap.of("response", taskExecutionResponse));
    K8sStateExecutionData executionData = (K8sStateExecutionData) executionResponse.getStateExecutionData();

    assertThat(executionData.getHelmChartInfo()).isEqualTo(helmChartInfo);
  }
}
