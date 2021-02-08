package io.harness.cdng.k8s;

import static io.harness.rule.OwnerRule.ABOSII;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.delegate.task.k8s.K8sBGDeployRequest;
import io.harness.delegate.task.k8s.K8sTaskType;
import io.harness.pms.yaml.ParameterField;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;

public class K8sBlueGreenStepTest extends AbstractK8sStepExecutorTest {
  @InjectMocks private K8sBlueGreenStep k8sBlueGreenStep;

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testExecuteTask() {
    K8sBlueGreenStepParameters stepParameters = new K8sBlueGreenStepParameters();
    stepParameters.setSkipDryRun(ParameterField.createValueField(true));
    stepParameters.setTimeout(ParameterField.createValueField("30m"));

    K8sBGDeployRequest request = executeTask(stepParameters, K8sBGDeployRequest.class);
    assertThat(request.getAccountId()).isEqualTo(accountId);
    assertThat(request.getTaskType()).isEqualTo(K8sTaskType.BLUE_GREEN_DEPLOY);
    assertThat(request.getK8sInfraDelegateConfig()).isEqualTo(infraDelegateConfig);
    assertThat(request.getManifestDelegateConfig()).isEqualTo(manifestDelegateConfig);
    assertThat(request.isSkipDryRun()).isTrue();
    assertThat(request.getTimeoutIntervalInMin()).isEqualTo(30);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testExecuteTaskNullParameterFields() {
    K8sBlueGreenStepParameters stepParameters = new K8sBlueGreenStepParameters();
    stepParameters.setSkipDryRun(ParameterField.ofNull());
    stepParameters.setTimeout(ParameterField.ofNull());

    K8sBGDeployRequest request = executeTask(stepParameters, K8sBGDeployRequest.class);
    assertThat(request.isSkipDryRun()).isFalse();
    assertThat(request.getTimeoutIntervalInMin()).isEqualTo(K8sStepHelper.getTimeout(stepParameters));
  }

  @Override
  protected K8sStepExecutor getK8sStepExecutor() {
    return k8sBlueGreenStep;
  }
}