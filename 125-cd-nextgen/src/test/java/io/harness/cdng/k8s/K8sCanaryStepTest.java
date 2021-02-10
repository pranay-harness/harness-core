package io.harness.cdng.k8s;

import static io.harness.rule.OwnerRule.ABOSII;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.beans.NGInstanceUnitType;
import io.harness.category.element.UnitTests;
import io.harness.delegate.task.k8s.K8sCanaryDeployRequest;
import io.harness.delegate.task.k8s.K8sTaskType;
import io.harness.pms.yaml.ParameterField;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;

public class K8sCanaryStepTest extends AbstractK8sStepExecutorTest {
  @InjectMocks private K8sCanaryStep k8sCanaryStep;

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testExecuteTask() {
    CountInstanceSelection instanceSelection = new CountInstanceSelection();
    instanceSelection.setCount(ParameterField.createValueField(10));
    K8sCanaryStepParameters stepParameters = new K8sCanaryStepParameters();
    stepParameters.setSkipDryRun(ParameterField.createValueField(true));
    stepParameters.setTimeout(ParameterField.createValueField("30m"));
    stepParameters.setInstanceSelection(
        InstanceSelectionWrapper.builder().type(K8sInstanceUnitType.Count).spec(instanceSelection).build());

    K8sCanaryDeployRequest request = executeTask(stepParameters, K8sCanaryDeployRequest.class);
    assertThat(request.getAccountId()).isEqualTo(accountId);
    assertThat(request.getInstances()).isEqualTo(10);
    assertThat(request.getInstanceUnitType()).isEqualTo(NGInstanceUnitType.COUNT);
    assertThat(request.getK8sInfraDelegateConfig()).isEqualTo(infraDelegateConfig);
    assertThat(request.getManifestDelegateConfig()).isEqualTo(manifestDelegateConfig);
    assertThat(request.getTaskType()).isEqualTo(K8sTaskType.CANARY_DEPLOY);
    assertThat(request.isSkipDryRun()).isTrue();
    assertThat(request.getTimeoutIntervalInMin()).isEqualTo(30);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testExecuteTaskNullParameterFields() {
    PercentageInstanceSelection instanceSelection = new PercentageInstanceSelection();
    instanceSelection.setPercentage(ParameterField.createValueField(90));
    K8sCanaryStepParameters stepParameters = new K8sCanaryStepParameters();
    stepParameters.setSkipDryRun(ParameterField.ofNull());
    stepParameters.setTimeout(ParameterField.ofNull());
    stepParameters.setInstanceSelection(
        InstanceSelectionWrapper.builder().type(K8sInstanceUnitType.Percentage).spec(instanceSelection).build());

    K8sCanaryDeployRequest request = executeTask(stepParameters, K8sCanaryDeployRequest.class);
    assertThat(request.isSkipDryRun()).isFalse();
    assertThat(request.getTimeoutIntervalInMin()).isEqualTo(K8sStepHelper.getTimeout(stepParameters));
  }

  @Override
  protected K8sStepExecutor getK8sStepExecutor() {
    return k8sCanaryStep;
  }
}