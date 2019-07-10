package io.harness.k8s.kubectl;

import static org.junit.Assert.assertEquals;

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

    assertEquals("kubectl describe --filename=manifests.yaml", describeCommand.command());
  }
}
