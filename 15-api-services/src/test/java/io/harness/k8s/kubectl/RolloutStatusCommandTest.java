package io.harness.k8s.kubectl;

import static io.harness.rule.OwnerRule.PUNEET;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class RolloutStatusCommandTest extends CategoryTest {
  @Test
  @Owner(developers = PUNEET)
  @Category(UnitTests.class)
  public void smokeTest() throws Exception {
    Kubectl client = Kubectl.client(null, null);

    RolloutStatusCommand rolloutStatusCommand = client.rollout().status().resource("Deployment/nginx").watch(true);

    assertThat(rolloutStatusCommand.command()).isEqualTo("kubectl rollout status Deployment/nginx --watch=true");
  }

  @Test
  @Owner(developers = PUNEET)
  @Category(UnitTests.class)
  public void watchFalseTest() throws Exception {
    Kubectl client = Kubectl.client(null, null);

    RolloutStatusCommand rolloutStatusCommand = client.rollout().status().resource("Deployment/nginx").watch(false);

    assertThat(rolloutStatusCommand.command()).isEqualTo("kubectl rollout status Deployment/nginx --watch=false");
  }
}
