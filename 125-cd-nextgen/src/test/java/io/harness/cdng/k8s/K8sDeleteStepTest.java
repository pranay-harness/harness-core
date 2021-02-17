package io.harness.cdng.k8s;

import static io.harness.cdng.k8s.K8sDeleteStep.K8S_DELETE_COMMAND_NAME;
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
import io.harness.delegate.task.k8s.K8sDeleteRequest;
import io.harness.delegate.task.k8s.K8sDeployRequest;
import io.harness.delegate.task.k8s.K8sDeployResponse;
import io.harness.delegate.task.k8s.K8sInfraDelegateConfig;
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
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class K8sDeleteStepTest extends CategoryTest {
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

  @InjectMocks private K8sDeleteStep deleteStep;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    doReturn(infraDelegateConfig).when(k8sStepHelper).getK8sInfraDelegateConfig(infrastructureOutcome, ambiance);
    doReturn(manifestDelegateConfig).when(k8sStepHelper).getManifestDelegateConfig(storeConfig, ambiance);
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testObtainTaskWithResourceName() {
    DeleteResourceNameSpec spec = new DeleteResourceNameSpec();
    spec.setResourceNames(ParameterField.createValueField(Arrays.asList(
        "Deployment/test-delete-resource-name-deployment", "ConfigMap/test-delete-resource-name-config")));

    final K8sDeleteStepParameters stepParameters =
        K8sDeleteStepParameters.infoBuilder()
            .deleteResources(DeleteResourcesWrapper.builder().spec(spec).type(DeleteResourcesType.ResourceName).build())
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
    doReturn("test-delete-resource-name-release").when(k8sStepHelper).getReleaseName(infrastructureOutcome);
    doReturn(manifestOutcome).when(k8sStepHelper).getK8sManifestOutcome(any(LinkedList.class));

    deleteStep.obtainTask(ambiance, stepParameters, stepInputPackage);
    ArgumentCaptor<K8sDeleteRequest> deleteRequestCaptor = ArgumentCaptor.forClass(K8sDeleteRequest.class);

    verify(k8sStepHelper, times(1))
        .queueK8sTask(eq(stepParameters), deleteRequestCaptor.capture(), eq(ambiance), eq(infrastructureOutcome));
    K8sDeleteRequest deleteRequest = deleteRequestCaptor.getValue();
    assertThat(deleteRequest).isNotNull();
    assertThat(deleteRequest.getCommandName()).isEqualTo(K8S_DELETE_COMMAND_NAME);
    assertThat(deleteRequest.getTaskType()).isEqualTo(K8sTaskType.DELETE);
    assertThat(deleteRequest.getReleaseName()).isEqualTo("test-delete-resource-name-release");
    assertThat(deleteRequest.getFilePaths()).isEmpty();
    assertThat(deleteRequest.getResources())
        .isEqualTo("Deployment/test-delete-resource-name-deployment,ConfigMap/test-delete-resource-name-config");
    assertThat(deleteRequest.isDeleteNamespacesForRelease()).isEqualTo(false);
    assertThat(deleteRequest.getTimeoutIntervalInMin()).isEqualTo(10);
    assertThat(deleteRequest.getK8sInfraDelegateConfig()).isEqualTo(infraDelegateConfig);
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testObtainTaskWithManifestPath() {
    DeleteManifestPathSpec spec = new DeleteManifestPathSpec();
    spec.setManifestPaths(ParameterField.createValueField(Arrays.asList("deployment.yaml", "config.yaml")));
    spec.setAllManifestPaths(ParameterField.createValueField(false));

    final K8sDeleteStepParameters stepParameters =
        K8sDeleteStepParameters.infoBuilder()
            .deleteResources(DeleteResourcesWrapper.builder().spec(spec).type(DeleteResourcesType.ManifestPath).build())
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
    doReturn("test-delete-manifest-file-release").when(k8sStepHelper).getReleaseName(infrastructureOutcome);
    doReturn(manifestOutcome).when(k8sStepHelper).getK8sManifestOutcome(any(LinkedList.class));

    deleteStep.obtainTask(ambiance, stepParameters, stepInputPackage);
    ArgumentCaptor<K8sDeleteRequest> deleteRequestCaptor = ArgumentCaptor.forClass(K8sDeleteRequest.class);

    verify(k8sStepHelper, times(1))
        .queueK8sTask(eq(stepParameters), deleteRequestCaptor.capture(), eq(ambiance), eq(infrastructureOutcome));
    K8sDeleteRequest deleteRequest = deleteRequestCaptor.getValue();
    assertThat(deleteRequest).isNotNull();
    assertThat(deleteRequest.getCommandName()).isEqualTo(K8S_DELETE_COMMAND_NAME);
    assertThat(deleteRequest.getTaskType()).isEqualTo(K8sTaskType.DELETE);
    assertThat(deleteRequest.getReleaseName()).isEqualTo("test-delete-manifest-file-release");
    assertThat(deleteRequest.getFilePaths()).isEqualTo("deployment.yaml,config.yaml");
    assertThat(deleteRequest.getResources()).isEmpty();
    assertThat(deleteRequest.isDeleteNamespacesForRelease()).isEqualTo(false);
    assertThat(deleteRequest.getTimeoutIntervalInMin()).isEqualTo(10);
    assertThat(deleteRequest.getK8sInfraDelegateConfig()).isEqualTo(infraDelegateConfig);
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testObtainTaskWithReleaseName() {
    DeleteReleaseNameSpec spec = new DeleteReleaseNameSpec();
    spec.setDeleteNamespace(ParameterField.createValueField(true));

    final K8sDeleteStepParameters stepParameters =
        K8sDeleteStepParameters.infoBuilder()
            .deleteResources(DeleteResourcesWrapper.builder().spec(spec).type(DeleteResourcesType.ReleaseName).build())
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
    doReturn("test-delete-release-name-release").when(k8sStepHelper).getReleaseName(infrastructureOutcome);
    doReturn(manifestOutcome).when(k8sStepHelper).getK8sManifestOutcome(any(LinkedList.class));

    deleteStep.obtainTask(ambiance, stepParameters, stepInputPackage);
    ArgumentCaptor<K8sDeleteRequest> deleteRequestCaptor = ArgumentCaptor.forClass(K8sDeleteRequest.class);

    verify(k8sStepHelper, times(1))
        .queueK8sTask(eq(stepParameters), deleteRequestCaptor.capture(), eq(ambiance), eq(infrastructureOutcome));
    K8sDeleteRequest deleteRequest = deleteRequestCaptor.getValue();
    assertThat(deleteRequest).isNotNull();
    assertThat(deleteRequest.getCommandName()).isEqualTo(K8S_DELETE_COMMAND_NAME);
    assertThat(deleteRequest.getTaskType()).isEqualTo(K8sTaskType.DELETE);
    assertThat(deleteRequest.getReleaseName()).isEqualTo("test-delete-release-name-release");
    assertThat(deleteRequest.getFilePaths()).isEmpty();
    assertThat(deleteRequest.getResources()).isEmpty();
    assertThat(deleteRequest.isDeleteNamespacesForRelease()).isEqualTo(true);
    assertThat(deleteRequest.getTimeoutIntervalInMin()).isEqualTo(10);
    assertThat(deleteRequest.getK8sInfraDelegateConfig()).isEqualTo(infraDelegateConfig);
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testHandleTaskResultSucceeded() {
    K8sDeleteStepParameters stepParameters = K8sDeleteStepParameters.infoBuilder().build();
    Map<String, ResponseData> responseDataMap =
        ImmutableMap.of("activity", K8sDeployResponse.builder().commandExecutionStatus(SUCCESS).build());

    StepResponse response = deleteStep.handleTaskResult(ambiance, stepParameters, responseDataMap);
    assertThat(response.getStatus()).isEqualTo(Status.SUCCEEDED);
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testHandleTaskResultFailed() {
    K8sDeleteStepParameters stepParameters = K8sDeleteStepParameters.infoBuilder().build();
    Map<String, ResponseData> responseDataMap = ImmutableMap.of("activity",
        K8sDeployResponse.builder().errorMessage("Execution failed.").commandExecutionStatus(FAILURE).build());

    StepResponse response = deleteStep.handleTaskResult(ambiance, stepParameters, responseDataMap);
    assertThat(response.getStatus()).isEqualTo(Status.FAILED);
    assertThat(response.getFailureInfo().getErrorMessage()).isEqualTo("Execution failed.");
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testGetK8sDeleteStepParameter() {
    assertThat(deleteStep.getStepParametersClass()).isEqualTo(K8sDeleteStepParameters.class);
  }
}
