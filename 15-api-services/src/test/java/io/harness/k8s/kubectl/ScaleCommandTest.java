package io.harness.k8s.kubectl;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ScaleCommandTest extends CategoryTest {
  @Test
  @Category(UnitTests.class)
  public void smokeTest() {
    Kubectl client = Kubectl.client(null, null);

    ScaleCommand scaleCommand = client.scale().resource("Deployment/nginx").replicas(2);

    assertThat(scaleCommand.command()).isEqualTo("kubectl scale Deployment/nginx --replicas=2");
  }
}
