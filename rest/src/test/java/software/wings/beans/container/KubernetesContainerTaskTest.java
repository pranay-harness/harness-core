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
}
