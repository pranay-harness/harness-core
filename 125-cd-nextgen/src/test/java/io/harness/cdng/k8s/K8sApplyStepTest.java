package io.harness.cdng.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.rule.OwnerRule.ABOSII;
import static io.harness.rule.OwnerRule.ACASIAN;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.delegate.task.k8s.K8sApplyRequest;
import io.harness.delegate.task.k8s.K8sTaskType;
import io.harness.exception.InvalidRequestException;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.yaml.ParameterField;
import io.harness.rule.Owner;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;

@OwnedBy(CDP)
public class K8sApplyStepTest extends AbstractK8sStepExecutorTestBase {
  @InjectMocks private K8sApplyStep k8sApplyStep;

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testExecuteTask() {
    K8sApplyStepParameters stepParameters = new K8sApplyStepParameters();
    stepParameters.setSkipDryRun(ParameterField.createValueField(true));
    stepParameters.setSkipSteadyStateCheck(ParameterField.createValueField(true));
    stepParameters.setFilePaths(ParameterField.createValueField(Arrays.asList("file1.yaml", "file2.yaml")));
    final StepElementParameters stepElementParameters =
        StepElementParameters.builder().spec(stepParameters).timeout(ParameterField.createValueField("30m")).build();

    K8sApplyRequest request = executeTask(stepElementParameters, K8sApplyRequest.class);
    assertThat(request.getAccountId()).isEqualTo(accountId);
    assertThat(request.getFilePaths()).containsExactlyInAnyOrder("file1.yaml", "file2.yaml");
    assertThat(request.getTaskType()).isEqualTo(K8sTaskType.APPLY);
    assertThat(request.isSkipDryRun()).isTrue();
    assertThat(request.isSkipSteadyStateCheck()).isTrue();
    assertThat(request.getTimeoutIntervalInMin()).isEqualTo(30);

    ArgumentCaptor<String> releaseNameCaptor = ArgumentCaptor.forClass(String.class);
    verify(k8sStepHelper, times(1)).publishReleaseNameStepDetails(eq(ambiance), releaseNameCaptor.capture());
    assertThat(releaseNameCaptor.getValue()).isEqualTo(releaseName);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testExecuteNullParameterFields() {
    K8sApplyStepParameters stepParameters = new K8sApplyStepParameters();
    stepParameters.setSkipDryRun(ParameterField.ofNull());
    stepParameters.setSkipSteadyStateCheck(ParameterField.ofNull());
    stepParameters.setFilePaths(ParameterField.createValueField(Arrays.asList("file1.yaml", "file2.yaml")));

    final StepElementParameters stepElementParameters =
        StepElementParameters.builder().spec(stepParameters).timeout(ParameterField.ofNull()).build();

    K8sApplyRequest request = executeTask(stepElementParameters, K8sApplyRequest.class);
    assertThat(request.isSkipDryRun()).isFalse();
    assertThat(request.isSkipSteadyStateCheck()).isFalse();
    assertThat(request.getTimeoutIntervalInMin()).isEqualTo(K8sStepHelper.getTimeoutInMin(stepElementParameters));
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testShouldValidateFilePathsSuccess() {
    K8sApplyStepParameters stepParameters = new K8sApplyStepParameters();
    stepParameters.setFilePaths(ParameterField.createValueField(Arrays.asList("file1.yaml", "file2.yaml")));

    final StepElementParameters stepElementParameters =
        StepElementParameters.builder().spec(stepParameters).timeout(ParameterField.ofNull()).build();
    k8sApplyStep.startChainLink(ambiance, stepElementParameters, StepInputPackage.builder().build());

    verify(k8sStepHelper, times(1)).startChainLink(eq(k8sApplyStep), eq(ambiance), eq(stepElementParameters));
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testShouldValidateFilePathsFailure() {
    final StepElementParameters stepElementParameters =
        StepElementParameters.builder()
            .spec(K8sApplyStepParameters.infoBuilder()
                      .filePaths(ParameterField.createValueField(Arrays.asList("file1.yaml", "")))
                      .build())
            .timeout(ParameterField.ofNull())
            .build();

    assertFilePathsValidation(stepElementParameters);

    final StepElementParameters stepElementParametersWithEmptyFilePaths =
        StepElementParameters.builder()
            .spec(K8sApplyStepParameters.infoBuilder()
                      .filePaths(ParameterField.createValueField(Collections.emptyList()))
                      .build())
            .timeout(ParameterField.ofNull())
            .build();

    assertFilePathsValidation(stepElementParametersWithEmptyFilePaths);

    final StepElementParameters stepElementParametersWithNullFilePaths =
        StepElementParameters.builder()
            .spec(K8sApplyStepParameters.infoBuilder().filePaths(ParameterField.createValueField(null)).build())
            .timeout(ParameterField.ofNull())
            .build();

    assertFilePathsValidation(stepElementParametersWithNullFilePaths);
  }

  private void assertFilePathsValidation(StepElementParameters stepElementParameters) {
    try {
      k8sApplyStep.startChainLink(ambiance, stepElementParameters, StepInputPackage.builder().build());
      fail("Should throw invalid request exception");
    } catch (Exception ex) {
      assertThat(ex).isInstanceOf(InvalidRequestException.class);
      assertThat(ex.getMessage()).isEqualTo("File/Folder path must be present");
    }
  }

  @Override
  protected K8sStepExecutor getK8sStepExecutor() {
    return k8sApplyStep;
  }
}