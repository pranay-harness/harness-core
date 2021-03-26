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
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.cdng.infra.beans.InfrastructureOutcome;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.delegate.beans.logstreaming.UnitProgressData;
import io.harness.delegate.task.k8s.K8sDeleteRequest;
import io.harness.delegate.task.k8s.K8sDeployResponse;
import io.harness.delegate.task.k8s.K8sTaskType;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.sdk.core.resolver.RefObjectUtils;
import io.harness.pms.sdk.core.resolver.outcome.OutcomeService;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.sdk.core.steps.executables.TaskChainResponse;
import io.harness.pms.sdk.core.steps.io.RollbackInfo;
import io.harness.pms.sdk.core.steps.io.RollbackOutcome;
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

public class K8sCanaryDeleteStepTest extends CategoryTest {
  @Mock private OutcomeService outcomeService;
  @Mock private K8sStepHelper k8sStepHelper;

  @InjectMocks private K8sCanaryDeleteStep canaryDeleteStep;

  @Mock private InfrastructureOutcome infrastructureOutcome;
  @Mock private ExecutionSweepingOutputService executionSweepingOutputService;

  private final Ambiance ambiance = Ambiance.newBuilder().build();
  private final StepInputPackage stepInputPackage = StepInputPackage.builder().build();

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testObtainTask() {
    final String canaryWorkload = "default/Deployment/canary-deployment";
    final K8sCanaryDeleteStepParameters stepParameters =
        K8sCanaryDeleteStepParameters.infoBuilder().timeout(ParameterField.createValueField("10m")).build();
    final K8sCanaryOutcome k8sCanaryOutcome = K8sCanaryOutcome.builder()
                                                  .canaryWorkload(canaryWorkload)
                                                  .targetInstances(4)
                                                  .releaseNumber(2)
                                                  .releaseName("canary-deployment")
                                                  .build();
    final TaskChainResponse response =
        TaskChainResponse.builder().taskRequest(TaskRequest.newBuilder().build()).build();

    doReturn(response)
        .when(k8sStepHelper)
        .queueK8sTask(eq(stepParameters), any(K8sDeleteRequest.class), eq(ambiance), eq(infrastructureOutcome));
    doReturn(k8sCanaryOutcome)
        .when(executionSweepingOutputService)
        .resolve(ambiance, RefObjectUtils.getSweepingOutputRefObject(OutcomeExpressionConstants.K8S_CANARY_OUTCOME));
    doReturn(infrastructureOutcome)
        .when(outcomeService)
        .resolve(ambiance, RefObjectUtils.getOutcomeRefObject(OutcomeExpressionConstants.INFRASTRUCTURE));

    canaryDeleteStep.obtainTask(ambiance, stepParameters, stepInputPackage);
    ArgumentCaptor<K8sDeleteRequest> requestCaptor = ArgumentCaptor.forClass(K8sDeleteRequest.class);
    verify(k8sStepHelper)
        .queueK8sTask(eq(stepParameters), requestCaptor.capture(), eq(ambiance), eq(infrastructureOutcome));

    K8sDeleteRequest k8sDeleteRequest = requestCaptor.getValue();
    assertThat(k8sDeleteRequest.getResources()).isEqualTo(canaryWorkload);
    assertThat(k8sDeleteRequest.getTaskType()).isEqualTo(K8sTaskType.DELETE);
    assertThat(k8sDeleteRequest.getTimeoutIntervalInMin()).isEqualTo(10);
    assertThat(k8sDeleteRequest.isDeleteNamespacesForRelease()).isFalse();
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testHandleTaskResult() {
    K8sCanaryDeleteStepParameters stepParameters =
        K8sCanaryDeleteStepParameters.infoBuilder().timeout(ParameterField.createValueField("10m")).build();
    Map<String, ResponseData> responseDataMap = ImmutableMap.of("activity",
        K8sDeployResponse.builder()
            .commandExecutionStatus(SUCCESS)
            .commandUnitsProgress(UnitProgressData.builder().build())
            .build());

    StepResponse stepResponse = canaryDeleteStep.handleTaskResult(ambiance, stepParameters, responseDataMap);
    assertThat(stepResponse.getStatus()).isEqualTo(SUCCEEDED);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testHandleTaskResultFailed() {
    RollbackInfo rollbackInfo = RollbackInfo.builder().identifier("test").build();
    K8sCanaryDeleteStepParameters stepParameters = K8sCanaryDeleteStepParameters.infoBuilder()
                                                       .timeout(ParameterField.createValueField("10m"))
                                                       .rollbackInfo(rollbackInfo)
                                                       .build();
    Map<String, ResponseData> responseDataMap = ImmutableMap.of("activity",
        K8sDeployResponse.builder()
            .commandExecutionStatus(FAILURE)
            .errorMessage("task failed")
            .commandUnitsProgress(UnitProgressData.builder().build())
            .build());

    StepResponse stepResponse = canaryDeleteStep.handleTaskResult(ambiance, stepParameters, responseDataMap);
    assertThat(stepResponse.getStatus()).isEqualTo(FAILED);
    assertThat(stepResponse.getFailureInfo().getErrorMessage()).isEqualTo("task failed");
    assertThat(stepResponse.getStepOutcomes()).hasSize(1);
    StepResponse.StepOutcome outcome = stepResponse.getStepOutcomes().iterator().next();
    assertThat(outcome.getOutcome()).isInstanceOf(RollbackOutcome.class);
    assertThat(((RollbackOutcome) outcome.getOutcome()).getRollbackInfo()).isEqualTo(rollbackInfo);
  }
}