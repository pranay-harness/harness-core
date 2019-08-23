package io.harness.k8s.kubectl;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class DescribeCommandTest extends CategoryTest {
  @Test
  @Category(UnitTests.class)
  public void smokeTest() {
    Kubectl client = Kubectl.client(null, null);
    DescribeCommand describeCommand = client.describe().filename("manifests.yaml");

    assertThat(describeCommand.command()).isEqualTo("kubectl describe --filename=manifests.yaml");
  }
}
