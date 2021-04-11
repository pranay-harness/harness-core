package io.harness.cdng.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.rule.OwnerRule.ABOSII;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.delegate.task.k8s.K8sApplyRequest;
import io.harness.delegate.task.k8s.K8sTaskType;
import io.harness.pms.yaml.ParameterField;
import io.harness.rule.Owner;

import java.util.Arrays;
import org.junit.Test;
import org.junit.experimental.categories.Category;
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
    stepParameters.setTimeout(ParameterField.createValueField("30m"));
    stepParameters.setFilePaths(ParameterField.createValueField(Arrays.asList("file1.yaml", "file2.yaml")));

    K8sApplyRequest request = executeTask(stepParameters, K8sApplyRequest.class);
    assertThat(request.getAccountId()).isEqualTo(accountId);
    assertThat(request.getFilePaths()).containsExactlyInAnyOrder("file1.yaml", "file2.yaml");
    assertThat(request.getTaskType()).isEqualTo(K8sTaskType.APPLY);
    assertThat(request.isSkipDryRun()).isTrue();
    assertThat(request.isSkipSteadyStateCheck()).isTrue();
    assertThat(request.getTimeoutIntervalInMin()).isEqualTo(30);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testExecuteNullParameterFields() {
    K8sApplyStepParameters stepParameters = new K8sApplyStepParameters();
    stepParameters.setSkipDryRun(ParameterField.ofNull());
    stepParameters.setSkipSteadyStateCheck(ParameterField.ofNull());
    stepParameters.setTimeout(ParameterField.ofNull());
    stepParameters.setFilePaths(ParameterField.createValueField(Arrays.asList("file1.yaml", "file2.yaml")));

    K8sApplyRequest request = executeTask(stepParameters, K8sApplyRequest.class);
    assertThat(request.isSkipDryRun()).isFalse();
    assertThat(request.isSkipSteadyStateCheck()).isFalse();
    assertThat(request.getTimeoutIntervalInMin()).isEqualTo(K8sStepHelper.getTimeoutInMin(stepParameters));
  }

  @Override
  protected K8sStepExecutor getK8sStepExecutor() {
    return k8sApplyStep;
  }
}