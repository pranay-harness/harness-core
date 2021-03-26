package io.harness.cdng.k8s;

import static io.harness.cdng.k8s.K8sBGSwapServicesStep.BG_STEP_MISSING_ERROR;
import static io.harness.cdng.k8s.K8sBGSwapServicesStep.SKIP_BG_SWAP_SERVICES_STEP_EXECUTION;
import static io.harness.cdng.k8s.K8sStepHelper.MISSING_INFRASTRUCTURE_ERROR;
import static io.harness.logging.CommandExecutionStatus.FAILURE;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;
import static io.harness.pms.contracts.execution.Status.FAILED;
import static io.harness.pms.contracts.execution.Status.SUCCEEDED;
import static io.harness.rule.OwnerRule.ABOSII;
import static io.harness.rule.OwnerRule.ANSHUL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.cdng.infra.beans.InfrastructureOutcome;
import io.harness.cdng.pipeline.steps.RollbackOptionalChildChainStep;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.delegate.beans.logstreaming.UnitProgressData;
import io.harness.delegate.task.k8s.K8sDeployRequest;
import io.harness.delegate.task.k8s.K8sDeployResponse;
import io.harness.delegate.task.k8s.K8sInfraDelegateConfig;
import io.harness.delegate.task.k8s.K8sSwapServiceSelectorsRequest;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.ambiance.Level;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.data.OptionalOutcome;
import io.harness.pms.sdk.core.data.OptionalSweepingOutput;
import io.harness.pms.sdk.core.resolver.RefObjectUtils;
import io.harness.pms.sdk.core.resolver.outcome.OutcomeService;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.sdk.core.steps.executables.TaskChainResponse;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepOutcome;
import io.harness.pms.yaml.ParameterField;
import io.harness.rule.Owner;
import io.harness.tasks.ResponseData;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.stream.Collectors;
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
  @Mock private InfrastructureOutcome infrastructureOutcome;
  @Mock private K8sInfraDelegateConfig infraDelegateConfig;
  @Mock private ExecutionSweepingOutputService executionSweepingOutputService;

  @InjectMocks private K8sBGSwapServicesStep k8sBGSwapServicesStep;

  private final Ambiance ambiance = Ambiance.newBuilder().build();
  private final StepInputPackage stepInputPackage = StepInputPackage.builder().build();
  final String primaryService = "k8s-svc";
  final String stageService = "k8s-svc-stage";
  final K8sBlueGreenOutcome blueGreenOutcome =
      K8sBlueGreenOutcome.builder().primaryServiceName(primaryService).stageServiceName(stageService).build();
  final TaskRequest createdTaskRequest = TaskRequest.newBuilder().build();
  final K8sBGSwapServicesStepParameters stepParameters =
      K8sBGSwapServicesStepParameters.infoBuilder().timeout(ParameterField.createValueField("10m")).build();

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    doReturn(infraDelegateConfig).when(k8sStepHelper).getK8sInfraDelegateConfig(infrastructureOutcome, ambiance);
  }

  private void setupPreConditions(Ambiance ambiance) {
    doReturn(TaskChainResponse.builder().taskRequest(createdTaskRequest).build())
        .when(k8sStepHelper)
        .queueK8sTask(eq(stepParameters), any(K8sDeployRequest.class), eq(ambiance), eq(infrastructureOutcome));
    doReturn(OptionalSweepingOutput.builder().found(true).output(blueGreenOutcome).build())
        .when(executionSweepingOutputService)
        .resolveOptional(
            ambiance, RefObjectUtils.getOutcomeRefObject(OutcomeExpressionConstants.K8S_BLUE_GREEN_OUTCOME));
    doReturn(infrastructureOutcome).when(k8sStepHelper).getInfrastructureOutcome(ambiance);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testObtainTask() {
    setupPreConditions(ambiance);

    final OptionalOutcome optionalOutcome = OptionalOutcome.builder().found(false).build();
    doReturn(optionalOutcome)
        .when(outcomeService)
        .resolveOptional(
            ambiance, RefObjectUtils.getOutcomeRefObject(OutcomeExpressionConstants.K8S_BG_SWAP_SERVICES_OUTCOME));

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

    // We need this null as K8sBGSwapServicesStep does not depend upon Manifests
    assertThat(request.getManifestDelegateConfig()).isNull();
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testObtainTaskInRollbackWhenSwapServicesExecutedInForwardPhase() {
    Ambiance ambiance =
        Ambiance.newBuilder()
            .addLevels(
                Level.newBuilder()
                    .setStepType(
                        StepType.newBuilder().setType(RollbackOptionalChildChainStep.STEP_TYPE.getType()).build())
                    .build())
            .build();
    final OptionalOutcome optionalOutcome = OptionalOutcome.builder().found(true).build();
    doReturn(optionalOutcome)
        .when(outcomeService)
        .resolveOptional(
            ambiance, RefObjectUtils.getOutcomeRefObject(OutcomeExpressionConstants.K8S_BG_SWAP_SERVICES_OUTCOME));

    setupPreConditions(ambiance);

    k8sBGSwapServicesStep.obtainTask(ambiance, stepParameters, stepInputPackage);
    ArgumentCaptor<K8sSwapServiceSelectorsRequest> requestArgumentCaptor =
        ArgumentCaptor.forClass(K8sSwapServiceSelectorsRequest.class);

    verify(k8sStepHelper, times(1))
        .queueK8sTask(eq(stepParameters), requestArgumentCaptor.capture(), eq(ambiance), eq(infrastructureOutcome));
    K8sSwapServiceSelectorsRequest request = requestArgumentCaptor.getValue();
    assertThat(request).isNotNull();
    assertThat(request.getService1()).isEqualTo(primaryService);
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testObtainTaskInRollbackWhenSwapServicesDidNotExecuteInForwardPhase() {
    Ambiance ambiance =
        Ambiance.newBuilder()
            .addLevels(
                Level.newBuilder()
                    .setStepType(
                        StepType.newBuilder().setType(RollbackOptionalChildChainStep.STEP_TYPE.getType()).build())
                    .build())
            .build();
    OptionalOutcome optionalOutcome = OptionalOutcome.builder().found(false).build();
    doReturn(optionalOutcome)
        .when(outcomeService)
        .resolveOptional(
            ambiance, RefObjectUtils.getOutcomeRefObject(OutcomeExpressionConstants.K8S_BG_SWAP_SERVICES_OUTCOME));

    TaskRequest taskRequest = k8sBGSwapServicesStep.obtainTask(ambiance, stepParameters, stepInputPackage);
    assertThat(taskRequest).isNotNull();
    assertThat(taskRequest.getSkipTaskRequest()).isNotNull();
    assertThat(taskRequest.getSkipTaskRequest().getMessage()).isEqualTo(SKIP_BG_SWAP_SERVICES_STEP_EXECUTION);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testHandleTaskResult() {
    Map<String, ResponseData> responseDataMap = ImmutableMap.of("activity",
        K8sDeployResponse.builder()
            .commandUnitsProgress(UnitProgressData.builder().build())
            .commandExecutionStatus(SUCCESS)
            .build());

    StepResponse response = k8sBGSwapServicesStep.handleTaskResult(ambiance, stepParameters, responseDataMap);
    assertThat(response.getStatus()).isEqualTo(SUCCEEDED);
    StepOutcome outcome = response.getStepOutcomes().stream().collect(Collectors.toList()).get(0);
    assertThat(outcome.getOutcome()).isInstanceOf(K8sBGSwapServicesOutcome.class);
    assertThat(outcome.getName()).isEqualTo(OutcomeExpressionConstants.K8S_BG_SWAP_SERVICES_OUTCOME);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testHandleTaskResultFailed() {
    Map<String, ResponseData> responseDataMap = ImmutableMap.of("activity",
        K8sDeployResponse.builder()
            .commandExecutionStatus(FAILURE)
            .commandUnitsProgress(UnitProgressData.builder().build())
            .build());

    StepResponse response = k8sBGSwapServicesStep.handleTaskResult(ambiance, stepParameters, responseDataMap);
    assertThat(response.getStatus()).isEqualTo(FAILED);
    assertThat(response.getStepOutcomes()).isEmpty();
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testObtainTaskWithInfraStrucutreOutcomeNotPresent() {
    setupPreConditions(ambiance);
    final OptionalOutcome optionalOutcome = OptionalOutcome.builder().found(false).build();
    doReturn(optionalOutcome)
        .when(outcomeService)
        .resolveOptional(
            ambiance, RefObjectUtils.getOutcomeRefObject(OutcomeExpressionConstants.K8S_BG_SWAP_SERVICES_OUTCOME));

    doThrow(new InvalidRequestException(MISSING_INFRASTRUCTURE_ERROR))
        .when(k8sStepHelper)
        .getInfrastructureOutcome(ambiance);
    assertThatThrownBy(() -> k8sBGSwapServicesStep.obtainTask(ambiance, stepParameters, stepInputPackage))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining(MISSING_INFRASTRUCTURE_ERROR);

    doReturn(OptionalSweepingOutput.builder().found(false).build())
        .when(executionSweepingOutputService)
        .resolveOptional(
            ambiance, RefObjectUtils.getOutcomeRefObject(OutcomeExpressionConstants.K8S_BLUE_GREEN_OUTCOME));
    assertThatThrownBy(() -> k8sBGSwapServicesStep.obtainTask(ambiance, stepParameters, stepInputPackage))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining(BG_STEP_MISSING_ERROR);
  }
}