package io.harness.cdng.k8s;

import static io.harness.cdng.k8s.K8sDeleteStep.K8S_DELETE_COMMAND_NAME;
import static io.harness.logging.CommandExecutionStatus.FAILURE;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;
import static io.harness.rule.OwnerRule.ACASIAN;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.logstreaming.UnitProgressData;
import io.harness.delegate.task.k8s.DeleteResourcesType;
import io.harness.delegate.task.k8s.K8sDeleteRequest;
import io.harness.delegate.task.k8s.K8sDeployResponse;
import io.harness.delegate.task.k8s.K8sTaskType;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.yaml.ParameterField;
import io.harness.rule.Owner;

import java.util.Arrays;
import lombok.SneakyThrows;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;

@OwnedBy(HarnessTeam.CDP)
public class K8sDeleteStepTest extends AbstractK8sStepExecutorTestBase {
  @InjectMocks private K8sDeleteStep deleteStep;

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testObtainTaskWithResourceName() {
    DeleteResourceNameSpec spec = new DeleteResourceNameSpec();
    spec.setResourceNames(ParameterField.createValueField(Arrays.asList(
        "Deployment/test-delete-resource-name-deployment", "ConfigMap/test-delete-resource-name-config")));

    final K8sDeleteStepParameters stepParameters =
        K8sDeleteStepParameters.infoBuilder()
            .deleteResources(DeleteResourcesWrapper.builder()
                                 .spec(spec)
                                 .type(io.harness.delegate.task.k8s.DeleteResourcesType.ResourceName)
                                 .build())
            .timeout(ParameterField.createValueField("10m"))
            .build();

    doReturn("test-delete-resource-name-release").when(k8sStepHelper).getReleaseName(infrastructureOutcome);
    K8sDeleteRequest deleteRequest = executeTask(stepParameters, K8sDeleteRequest.class);
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
            .deleteResources(DeleteResourcesWrapper.builder()
                                 .spec(spec)
                                 .type(io.harness.delegate.task.k8s.DeleteResourcesType.ManifestPath)
                                 .build())
            .timeout(ParameterField.createValueField("10m"))
            .build();

    doReturn("test-delete-manifest-file-release").when(k8sStepHelper).getReleaseName(infrastructureOutcome);

    K8sDeleteRequest deleteRequest = executeTask(stepParameters, K8sDeleteRequest.class);
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

    doReturn("test-delete-release-name-release").when(k8sStepHelper).getReleaseName(infrastructureOutcome);

    K8sDeleteRequest deleteRequest = executeTask(stepParameters, K8sDeleteRequest.class);
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

  @SneakyThrows
  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testHandleTaskResultSucceeded() {
    K8sDeleteStepParameters stepParameters = K8sDeleteStepParameters.infoBuilder().build();
    K8sDeployResponse k8sDeployResponse = K8sDeployResponse.builder()
                                              .commandExecutionStatus(SUCCESS)
                                              .commandUnitsProgress(UnitProgressData.builder().build())
                                              .build();

    StepResponse response = deleteStep.finalizeExecution(ambiance, stepParameters, null, () -> k8sDeployResponse);
    assertThat(response.getStatus()).isEqualTo(Status.SUCCEEDED);
  }

  @SneakyThrows
  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testHandleTaskResultFailed() {
    K8sDeleteStepParameters stepParameters = K8sDeleteStepParameters.infoBuilder().build();
    K8sDeployResponse k8sDeployResponse = K8sDeployResponse.builder()
                                              .errorMessage("Execution failed.")
                                              .commandExecutionStatus(FAILURE)
                                              .commandUnitsProgress(UnitProgressData.builder().build())
                                              .build();

    StepResponse response = deleteStep.finalizeExecution(ambiance, stepParameters, null, () -> k8sDeployResponse);
    assertThat(response.getStatus()).isEqualTo(Status.FAILED);
    assertThat(response.getFailureInfo().getErrorMessage()).isEqualTo("Execution failed.");
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testValidateK8sDeleteStepParams() {
    K8sDeleteStepParameters deleteStepParameters = K8sDeleteStepParameters.infoBuilder().build();
    StepInputPackage stepInputPackage = StepInputPackage.builder().build();
    assertThatThrownBy(() -> deleteStep.startChainLink(ambiance, deleteStepParameters, stepInputPackage))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("DeleteResources is mandatory");

    deleteStepParameters.setDeleteResources(DeleteResourcesWrapper.builder().build());
    assertThatThrownBy(() -> deleteStep.startChainLink(ambiance, deleteStepParameters, stepInputPackage))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("DeleteResources type is mandatory");

    deleteStepParameters.setDeleteResources(
        DeleteResourcesWrapper.builder().type(io.harness.delegate.task.k8s.DeleteResourcesType.ManifestPath).build());
    assertThatThrownBy(() -> deleteStep.startChainLink(ambiance, deleteStepParameters, stepInputPackage))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("DeleteResources spec is mandatory");
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testGetK8sDeleteStepParameter() {
    assertThat(deleteStep.getStepParametersClass()).isEqualTo(K8sDeleteStepParameters.class);
  }

  @Override
  protected K8sStepExecutor getK8sStepExecutor() {
    return deleteStep;
  }
}
