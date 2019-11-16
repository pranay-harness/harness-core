package io.harness.k8s.kubectl;

import static io.harness.rule.OwnerRule.PUNEET;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.OwnerRule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class VersionCommandTest extends CategoryTest {
  @Test
  @Owner(developers = PUNEET)
  @Category(UnitTests.class)
  public void smokeTest() {
    Kubectl client = Kubectl.client(null, null);
    VersionCommand versionCommand = client.version();

    assertThat(versionCommand.command()).isEqualTo("kubectl version");
  }
}
