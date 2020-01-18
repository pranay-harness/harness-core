package io.harness.k8s.kubectl;

import static io.harness.rule.OwnerRule.HANTANG;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class AuthCommandTest extends CategoryTest {
  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void testWatchNodes() {
    Kubectl kubectl = Kubectl.client(null, null);
    AuthCommand authCommand = kubectl.auth().verb("watch").resources("nodes").allNamespaces(true);
    assertThat(authCommand.command()).isEqualTo("kubectl auth can-i watch nodes --all-namespaces=true");
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void testWatchPodsInNamespace() {
    Kubectl kubectl = Kubectl.client(null, null);
    AuthCommand authCommand = kubectl.auth().verb("watch").resources("pods").namespace("default");
    assertThat(authCommand.command()).isEqualTo("kubectl auth can-i watch pods --namespace=default");
  }
}
