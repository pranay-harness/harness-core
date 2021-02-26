package io.harness.cdng.k8s;

import static io.harness.logging.CommandExecutionStatus.FAILURE;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;
import static io.harness.pms.contracts.execution.Status.FAILED;
import static io.harness.pms.contracts.execution.Status.SUCCEEDED;
import static io.harness.rule.OwnerRule.ABOSII;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.cdng.infra.beans.InfrastructureOutcome;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.delegate.beans.logstreaming.UnitProgressData;
import io.harness.delegate.task.k8s.K8sDeployRequest;
import io.harness.delegate.task.k8s.K8sDeployResponse;
import io.harness.delegate.task.k8s.K8sInfraDelegateConfig;
import io.harness.delegate.task.k8s.K8sSwapServiceSelectorsRequest;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.sdk.core.resolver.RefObjectUtils;
import io.harness.pms.sdk.core.resolver.outcome.OutcomeService;
import io.harness.pms.sdk.core.steps.executables.TaskChainResponse;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.yaml.ParameterField;
import io.harness.rule.Owner;
import io.harness.tasks.ResponseData;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class K8sBGSwapServicesStepTest extends CategoryTest {
  @Mock private K8sStepHelper k8sStepHelper;
  @Mock private OutcomeService outcomeService;

  @InjectMocks private K8sBGSwapServicesStep k8sBGSwapServicesStep;

  @Mock private InfrastructureOutcome infrastructureOutcome;
  @Mock private K8sInfraDelegateConfig infraDelegateConfig;

  private final Ambiance ambiance = Ambiance.newBuilder().build();
  private final StepInputPackage stepInputPackage = StepInputPackage.builder().build();

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    doReturn(infraDelegateConfig).when(k8sStepHelper).getK8sInfraDelegateConfig(infrastructureOutcome, ambiance);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testObtainTask() {
    final String primaryService = "k8s-svc";
    final String stageService = "k8s-svc-stage";
    final K8sBlueGreenOutcome blueGreenOutcome =
        K8sBlueGreenOutcome.builder().primaryServiceName(primaryService).stageServiceName(stageService).build();
    final TaskRequest createdTaskRequest = TaskRequest.newBuilder().build();
    final K8sBGSwapServicesStepParameters stepParameters =
        K8sBGSwapServicesStepParameters.infoBuilder().timeout(ParameterField.createValueField("10m")).build();

    doReturn(TaskChainResponse.builder().taskRequest(createdTaskRequest).build())
        .when(k8sStepHelper)
        .queueK8sTask(eq(stepParameters), any(K8sDeployRequest.class), eq(ambiance), eq(infrastructureOutcome));
    doReturn(blueGreenOutcome)
        .when(outcomeService)
        .resolve(ambiance, RefObjectUtils.getOutcomeRefObject(OutcomeExpressionConstants.K8S_BLUE_GREEN_OUTCOME));
    doReturn(infrastructureOutcome)
        .when(outcomeService)
        .resolve(ambiance, RefObjectUtils.getOutcomeRefObject(OutcomeExpressionConstants.INFRASTRUCTURE));

    k8sBGSwapServicesStep.obtainTask(ambiance, stepParameters, stepInputPackage);
    ArgumentCaptor<K8sSwapServiceSelectorsRequest> requestArgumentCaptor =
        ArgumentCaptor.forClass(K8sSwapServiceSelectorsRequest.class);

    verify(k8sStepHelper, times(1))
        .queueK8sTask(eq(stepParameters), requestArgumentCaptor.capture(), eq(ambiance), eq(infrastructureOutcome));
    K8sSwapServiceSelectorsRequest request = requestArgumentCaptor.getValue();
    assertThat(request).isNotNull();
    assertThat(request.getService1()).isEqualTo(primaryService);
    assertThat(request.getService2()).isEqualTo(stageService);
    assertThat(request.getK8sInfraDelegateConfig()).isEqualTo(infraDelegateConfig);
    assertThat(request.getCommandName()).isEqualTo(K8sBGSwapServicesStep.K8S_BG_SWAP_SERVICES_COMMAND_NAME);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testHandleTaskResult() {
    K8sBGSwapServicesStepParameters stepParameters =
        K8sBGSwapServicesStepParameters.infoBuilder().timeout(ParameterField.createValueField("10m")).build();
    Map<String, ResponseData> responseDataMap = ImmutableMap.of("activity",
        K8sDeployResponse.builder()
            .commandUnitsProgress(UnitProgressData.builder().build())
            .commandExecutionStatus(SUCCESS)
            .build());

    StepResponse response = k8sBGSwapServicesStep.handleTaskResult(ambiance, stepParameters, responseDataMap);
    assertThat(response.getStatus()).isEqualTo(SUCCEEDED);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testHandleTaskResultFailed() {
    K8sBGSwapServicesStepParameters stepParameters =
        K8sBGSwapServicesStepParameters.infoBuilder().timeout(ParameterField.createValueField("10m")).build();
    Map<String, ResponseData> responseDataMap = ImmutableMap.of("activity",
        K8sDeployResponse.builder()
            .commandExecutionStatus(FAILURE)
            .commandUnitsProgress(UnitProgressData.builder().build())
            .build());

    StepResponse response = k8sBGSwapServicesStep.handleTaskResult(ambiance, stepParameters, responseDataMap);
    assertThat(response.getStatus()).isEqualTo(FAILED);
  }
}