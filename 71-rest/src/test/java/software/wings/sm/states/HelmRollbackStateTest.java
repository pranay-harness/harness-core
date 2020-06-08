package software.wings.sm.states;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.GitConfig;
import software.wings.beans.container.HelmChartSpecification;
import software.wings.beans.container.ImageDetails;
import software.wings.helpers.ext.helm.HelmConstants;
import software.wings.helpers.ext.helm.request.HelmRollbackCommandRequest;
import software.wings.helpers.ext.k8s.request.K8sDelegateManifestConfig;
import software.wings.service.impl.ContainerServiceParams;
import software.wings.sm.ExecutionContext;
import software.wings.utils.WingsTestConstants;

import java.util.Collections;

public class HelmRollbackStateTest extends WingsBaseTest {
  @Mock private ExecutionContext executionContext;

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void getHelmCommandRequestTimeoutValue() {
    HelmRollbackState helmRollbackState = new HelmRollbackState("Helm-Rollback");
    HelmRollbackCommandRequest commandRequest;

    helmRollbackState.setSteadyStateTimeout(0);
    commandRequest = getHelmRollbackCommandRequest(helmRollbackState);
    assertThat(commandRequest.getTimeoutInMillis()).isEqualTo(600000);

    helmRollbackState.setSteadyStateTimeout(5);
    commandRequest = getHelmRollbackCommandRequest(helmRollbackState);
    assertThat(commandRequest.getTimeoutInMillis()).isEqualTo(300000);

    helmRollbackState.setSteadyStateTimeout(Integer.MAX_VALUE);
    commandRequest = getHelmRollbackCommandRequest(helmRollbackState);
    assertThat(commandRequest.getTimeoutInMillis()).isEqualTo(600000);
  }

  private HelmRollbackCommandRequest getHelmRollbackCommandRequest(HelmRollbackState helmRollbackState) {
    return (HelmRollbackCommandRequest) helmRollbackState.getHelmCommandRequest(executionContext,
        HelmChartSpecification.builder().build(), ContainerServiceParams.builder().build(), "release-name",
        WingsTestConstants.ACCOUNT_ID, WingsTestConstants.APP_ID, WingsTestConstants.ACTIVITY_ID,
        ImageDetails.builder().build(), "repo", GitConfig.builder().build(), Collections.emptyList(), null,
        K8sDelegateManifestConfig.builder().build(), Collections.emptyMap(), HelmConstants.HelmVersion.V3);
  }
}