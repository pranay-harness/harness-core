package io.harness.cdng.k8s;

import static io.harness.rule.OwnerRule.ABOSII;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.delegate.task.k8s.K8sRollingDeployRequest;
import io.harness.delegate.task.k8s.K8sTaskType;
import io.harness.pms.yaml.ParameterField;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;

public class K8sRollingStepTest extends AbstractK8sStepExecutorTestBase {
  @InjectMocks private K8sRollingStep k8sRollingStep;

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testExecuteK8sTask() {
    K8sRollingStepParameters stepParameters = new K8sRollingStepParameters();
    stepParameters.setSkipDryRun(ParameterField.createValueField(true));
    stepParameters.setTimeout(ParameterField.createValueField("30m"));

    K8sRollingDeployRequest request = executeTask(stepParameters, K8sRollingDeployRequest.class);
    assertThat(request.isSkipDryRun()).isTrue();
    assertThat(request.getTimeoutIntervalInMin()).isEqualTo(30);
    assertThat(request.getK8sInfraDelegateConfig()).isEqualTo(infraDelegateConfig);
    assertThat(request.getManifestDelegateConfig()).isEqualTo(manifestDelegateConfig);
    assertThat(request.getTaskType()).isEqualTo(K8sTaskType.DEPLOYMENT_ROLLING);
    assertThat(request.getAccountId()).isEqualTo(accountId);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testExecuteK8sTaskNullParameterFields() {
    K8sRollingStepParameters stepParameters = new K8sRollingStepParameters();
    stepParameters.setSkipDryRun(ParameterField.ofNull());
    stepParameters.setTimeout(ParameterField.ofNull());

    K8sRollingDeployRequest request = executeTask(stepParameters, K8sRollingDeployRequest.class);
    assertThat(request.isSkipDryRun()).isFalse();
    assertThat(request.getTimeoutIntervalInMin()).isEqualTo(K8sStepHelper.getTimeout(stepParameters));
  }

  @Override
  protected K8sStepExecutor getK8sStepExecutor() {
    return k8sRollingStep;
  }
}
