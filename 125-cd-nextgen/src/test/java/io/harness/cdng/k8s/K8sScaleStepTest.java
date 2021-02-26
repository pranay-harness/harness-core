package io.harness.cdng.k8s;

import static io.harness.cdng.k8s.K8sScaleStep.K8S_SCALE_COMMAND_NAME;
import static io.harness.logging.CommandExecutionStatus.FAILURE;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;
import static io.harness.rule.OwnerRule.ACASIAN;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.cdng.infra.beans.InfrastructureOutcome;
import io.harness.cdng.manifest.yaml.K8sManifestOutcome;
import io.harness.cdng.manifest.yaml.ManifestOutcome;
import io.harness.cdng.manifest.yaml.StoreConfig;
import io.harness.cdng.service.beans.ServiceOutcome;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.delegate.beans.logstreaming.UnitProgressData;
import io.harness.delegate.task.k8s.K8sDeployRequest;
import io.harness.delegate.task.k8s.K8sDeployResponse;
import io.harness.delegate.task.k8s.K8sInfraDelegateConfig;
import io.harness.delegate.task.k8s.K8sScaleRequest;
import io.harness.delegate.task.k8s.K8sTaskType;
import io.harness.delegate.task.k8s.ManifestDelegateConfig;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
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
import java.util.LinkedList;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class K8sScaleStepTest extends CategoryTest {
  @Mock private OutcomeService outcomeService;
  @Mock private K8sStepHelper k8sStepHelper;
  @Mock private InfrastructureOutcome infrastructureOutcome;
  @Mock private K8sInfraDelegateConfig infraDelegateConfig;
  @Mock private ManifestDelegateConfig manifestDelegateConfig;
  @Mock StoreConfig storeConfig;
  @Mock ServiceOutcome serviceOutcome;
  private final ManifestOutcome manifestOutcome = K8sManifestOutcome.builder().store(storeConfig).build();
  private final Ambiance ambiance = Ambiance.newBuilder().build();
  private final StepInputPackage stepInputPackage = StepInputPackage.builder().build();

  @InjectMocks private K8sScaleStep scaleStep;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    doReturn(infraDelegateConfig).when(k8sStepHelper).getK8sInfraDelegateConfig(infrastructureOutcome, ambiance);
    doReturn(manifestDelegateConfig).when(k8sStepHelper).getManifestDelegateConfig(storeConfig, ambiance);
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testObtainTaskWithCountInstanceSelection() {
    CountInstanceSelection spec = new CountInstanceSelection();
    spec.setCount(ParameterField.createValueField(2));

    final K8sScaleStepParameter stepParameters =
        K8sScaleStepParameter.infoBuilder()
            .instanceSelection(InstanceSelectionWrapper.builder().spec(spec).type(K8sInstanceUnitType.Count).build())
            .skipSteadyStateCheck(ParameterField.createValueField(false))
            .workload(ParameterField.createValueField("Deployment/test-scale-count-deployment"))
            .timeout(ParameterField.createValueField("10m"))
            .build();

    final TaskRequest taskRequest = TaskRequest.newBuilder().build();
    doReturn(TaskChainResponse.builder().taskRequest(taskRequest).build())
        .when(k8sStepHelper)
        .queueK8sTask(eq(stepParameters), any(K8sDeployRequest.class), eq(ambiance), eq(infrastructureOutcome));

    doReturn(serviceOutcome)
        .when(outcomeService)
        .resolve(ambiance, RefObjectUtils.getOutcomeRefObject(OutcomeExpressionConstants.SERVICE));
    doReturn(infrastructureOutcome)
        .when(outcomeService)
        .resolve(ambiance, RefObjectUtils.getOutcomeRefObject(OutcomeExpressionConstants.INFRASTRUCTURE));
    doReturn("test-scale-count-release").when(k8sStepHelper).getReleaseName(infrastructureOutcome);
    doReturn(manifestOutcome).when(k8sStepHelper).getK8sManifestOutcome(any(LinkedList.class));

    scaleStep.obtainTask(ambiance, stepParameters, stepInputPackage);
    ArgumentCaptor<K8sScaleRequest> scaleRequestArgumentCaptor = ArgumentCaptor.forClass(K8sScaleRequest.class);

    verify(k8sStepHelper, times(1))
        .queueK8sTask(
            eq(stepParameters), scaleRequestArgumentCaptor.capture(), eq(ambiance), eq(infrastructureOutcome));
    K8sScaleRequest scaleRequest = scaleRequestArgumentCaptor.getValue();
    assertThat(scaleRequest).isNotNull();
    assertThat(scaleRequest.getCommandName()).isEqualTo(K8S_SCALE_COMMAND_NAME);
    assertThat(scaleRequest.getTaskType()).isEqualTo(K8sTaskType.SCALE);
    assertThat(scaleRequest.getReleaseName()).isEqualTo("test-scale-count-release");
    assertThat(scaleRequest.getInstances()).isEqualTo(2);
    assertThat(scaleRequest.getWorkload()).isEqualTo("Deployment/test-scale-count-deployment");
    assertThat(scaleRequest.getTimeoutIntervalInMin()).isEqualTo(10);
    assertThat(scaleRequest.getK8sInfraDelegateConfig()).isEqualTo(infraDelegateConfig);
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testObtainTaskWithPercentageInstanceSelection() {
    PercentageInstanceSelection spec = new PercentageInstanceSelection();
    spec.setPercentage(ParameterField.createValueField(80));

    final K8sScaleStepParameter stepParameters =
        K8sScaleStepParameter.infoBuilder()
            .instanceSelection(
                InstanceSelectionWrapper.builder().spec(spec).type(K8sInstanceUnitType.Percentage).build())
            .skipSteadyStateCheck(ParameterField.createValueField(false))
            .workload(ParameterField.createValueField("Deployment/test-scale-percentage-deployment"))
            .timeout(ParameterField.createValueField("10m"))
            .build();

    final TaskRequest taskRequest = TaskRequest.newBuilder().build();
    doReturn(TaskChainResponse.builder().taskRequest(taskRequest).build())
        .when(k8sStepHelper)
        .queueK8sTask(eq(stepParameters), any(K8sDeployRequest.class), eq(ambiance), eq(infrastructureOutcome));

    doReturn(serviceOutcome)
        .when(outcomeService)
        .resolve(ambiance, RefObjectUtils.getOutcomeRefObject(OutcomeExpressionConstants.SERVICE));
    doReturn(infrastructureOutcome)
        .when(outcomeService)
        .resolve(ambiance, RefObjectUtils.getOutcomeRefObject(OutcomeExpressionConstants.INFRASTRUCTURE));
    doReturn("test-scale-percentage-release").when(k8sStepHelper).getReleaseName(infrastructureOutcome);
    doReturn(manifestOutcome).when(k8sStepHelper).getK8sManifestOutcome(any(LinkedList.class));

    scaleStep.obtainTask(ambiance, stepParameters, stepInputPackage);
    ArgumentCaptor<K8sScaleRequest> scaleRequestArgumentCaptor = ArgumentCaptor.forClass(K8sScaleRequest.class);

    verify(k8sStepHelper, times(1))
        .queueK8sTask(
            eq(stepParameters), scaleRequestArgumentCaptor.capture(), eq(ambiance), eq(infrastructureOutcome));
    K8sScaleRequest scaleRequest = scaleRequestArgumentCaptor.getValue();
    assertThat(scaleRequest).isNotNull();
    assertThat(scaleRequest.getCommandName()).isEqualTo(K8S_SCALE_COMMAND_NAME);
    assertThat(scaleRequest.getTaskType()).isEqualTo(K8sTaskType.SCALE);
    assertThat(scaleRequest.getReleaseName()).isEqualTo("test-scale-percentage-release");
    assertThat(scaleRequest.getInstances()).isEqualTo(80);
    assertThat(scaleRequest.getWorkload()).isEqualTo("Deployment/test-scale-percentage-deployment");
    assertThat(scaleRequest.getTimeoutIntervalInMin()).isEqualTo(10);
    assertThat(scaleRequest.getK8sInfraDelegateConfig()).isEqualTo(infraDelegateConfig);
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testHandleTaskResultSucceeded() {
    K8sScaleStepParameter stepParameters = K8sScaleStepParameter.infoBuilder().build();
    Map<String, ResponseData> responseDataMap = ImmutableMap.of("activity",
        K8sDeployResponse.builder()
            .commandExecutionStatus(SUCCESS)
            .commandUnitsProgress(UnitProgressData.builder().build())
            .build());

    StepResponse response = scaleStep.handleTaskResult(ambiance, stepParameters, responseDataMap);
    assertThat(response.getStatus()).isEqualTo(Status.SUCCEEDED);
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testHandleTaskResultFailed() {
    K8sScaleStepParameter stepParameters = K8sScaleStepParameter.infoBuilder().build();
    Map<String, ResponseData> responseDataMap = ImmutableMap.of("activity",
        K8sDeployResponse.builder()
            .errorMessage("Execution failed.")
            .commandExecutionStatus(FAILURE)
            .commandUnitsProgress(UnitProgressData.builder().build())
            .build());

    StepResponse response = scaleStep.handleTaskResult(ambiance, stepParameters, responseDataMap);
    assertThat(response.getStatus()).isEqualTo(Status.FAILED);
    assertThat(response.getFailureInfo().getErrorMessage()).isEqualTo("Execution failed.");
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testGetK8sScaleStepParameter() {
    assertThat(scaleStep.getStepParametersClass()).isEqualTo(K8sScaleStepParameter.class);
  }
}
