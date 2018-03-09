package software.wings.beans.container;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class KubernetesContainerTaskTest {
  @Test
  public void shouldCheckDaemonSet() {
    KubernetesContainerTask kubernetesContainerTask = new KubernetesContainerTask();
    kubernetesContainerTask.setAdvancedConfig("a: b\n kind: DaemonSet\nfoo: bar");
    assertThat(kubernetesContainerTask.checkDaemonSet()).isTrue();
    kubernetesContainerTask.setAdvancedConfig("a: b\n kind: \"DaemonSet\"\nfoo: bar");
    assertThat(kubernetesContainerTask.checkDaemonSet()).isTrue();
    kubernetesContainerTask.setAdvancedConfig("a: b\n kind:DaemonSet\nfoo: bar");
    assertThat(kubernetesContainerTask.checkDaemonSet()).isTrue();
    kubernetesContainerTask.setAdvancedConfig("a: b\n kind:  DaemonSet\nfoo: bar");
    assertThat(kubernetesContainerTask.checkDaemonSet()).isTrue();
    kubernetesContainerTask.setAdvancedConfig("a: b\n kind:\t\"DaemonSet\"\nfoo: bar");
    assertThat(kubernetesContainerTask.checkDaemonSet()).isTrue();
    kubernetesContainerTask.setAdvancedConfig("a: b\n kind: Deployment\nfoo: bar");
    assertThat(kubernetesContainerTask.checkDaemonSet()).isFalse();
  }

  @Test
  public void shouldSetAdvancedConfig() {
    KubernetesContainerTask kubernetesContainerTask = new KubernetesContainerTask();

    kubernetesContainerTask.setAdvancedConfig(null);
    assertThat(kubernetesContainerTask.getAdvancedConfig()).isNull();

    kubernetesContainerTask.setAdvancedConfig("one line");
    assertThat(kubernetesContainerTask.getAdvancedConfig()).isEqualTo("one line");

    kubernetesContainerTask.setAdvancedConfig("a\nb");
    assertThat(kubernetesContainerTask.getAdvancedConfig()).isEqualTo("a\nb");

    kubernetesContainerTask.setAdvancedConfig("a \nb");
    assertThat(kubernetesContainerTask.getAdvancedConfig()).isEqualTo("a\nb");

    kubernetesContainerTask.setAdvancedConfig("a    \n b   \n  c");
    assertThat(kubernetesContainerTask.getAdvancedConfig()).isEqualTo("a\n b\n  c");
  }
}
