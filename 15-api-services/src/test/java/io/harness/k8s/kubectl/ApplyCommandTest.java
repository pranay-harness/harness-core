package io.harness.k8s.kubectl;

import static io.harness.rule.OwnerRule.PUNEET;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ApplyCommandTest extends CategoryTest {
  @Test
  @Owner(developers = PUNEET)
  @Category(UnitTests.class)
  public void smokeTest() {
    Kubectl client = Kubectl.client(null, null);

    ApplyCommand applyCommand = client.apply().filename("manifests.yaml").dryrun(true).record(true).output("yaml");

    assertThat(applyCommand.command())
        .isEqualTo("kubectl apply --filename=manifests.yaml --dry-run --record --output=yaml");
  }

  @Test
  @Owner(developers = PUNEET)
  @Category(UnitTests.class)
  public void kubectlPathTest() {
    Kubectl client = Kubectl.client("/usr/bin/kubectl", null);

    ApplyCommand applyCommand = client.apply().filename("manifests.yaml");

    assertThat(applyCommand.command()).isEqualTo("/usr/bin/kubectl apply --filename=manifests.yaml");

    client = Kubectl.client("C:\\Program Files\\Docker\\Docker\\Resources\\bin\\kubectl.exe", null);

    applyCommand = client.apply().filename("manifests.yaml");

    assertThat(applyCommand.command())
        .isEqualTo(
            "\"C:\\Program Files\\Docker\\Docker\\Resources\\bin\\kubectl.exe\" apply --filename=manifests.yaml");
  }

  @Test
  @Owner(developers = PUNEET)
  @Category(UnitTests.class)
  public void kubeconfigPathTest() {
    Kubectl client = Kubectl.client("", "config");

    ApplyCommand applyCommand = client.apply().filename("manifests.yaml");

    assertThat(applyCommand.command()).isEqualTo("kubectl --kubeconfig=config apply --filename=manifests.yaml");

    client = Kubectl.client("", "c:\\config files\\.kubeconfig");

    applyCommand = client.apply().filename("manifests.yaml");

    assertThat(applyCommand.command())
        .isEqualTo("kubectl --kubeconfig=\"c:\\config files\\.kubeconfig\" apply --filename=manifests.yaml");
  }

  @Test
  @Owner(developers = PUNEET)
  @Category(UnitTests.class)
  public void testDryRun() {
    Kubectl client = Kubectl.client(null, null);

    ApplyCommand applyCommand = client.apply().filename("manifests.yaml").dryrun(true).output("yaml");

    assertThat(applyCommand.command().contains("--dry-run")).isTrue();

    applyCommand.dryrun(false);
    assertThat(applyCommand.command().contains("--dry-run")).isFalse();
  }

  @Test
  @Owner(developers = PUNEET)
  @Category(UnitTests.class)
  public void testRecord() {
    Kubectl client = Kubectl.client(null, null);

    ApplyCommand applyCommand = client.apply().filename("manifests.yaml").record(true).output("yaml");

    assertThat(applyCommand.command().contains("--record")).isTrue();

    applyCommand.record(false);
    assertThat(applyCommand.command().contains("--record")).isFalse();
  }
}
