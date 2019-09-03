package io.harness.perpetualtask.k8s.watch;

import static io.harness.rule.OwnerRule.HANTANG;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watcher;
import io.harness.category.element.IntegrationTests;
import io.harness.event.client.EventPublisher;
import io.harness.rule.OwnerRule.Owner;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;

import java.util.concurrent.TimeUnit;

public class PodWatcherTest {
  @Mock EventPublisher eventPublisher;
  Config config = new ConfigBuilder().build();

  @Test
  @Owner(emails = HANTANG)
  @Category(IntegrationTests.class)
  @Ignore("TODO: Not running it now, will run it in next iteration.")
  public void testPodWatcher() throws Exception {
    final KubernetesClient client = new DefaultKubernetesClient(config);

    Watcher watcher = new PodWatcher(client, eventPublisher);
    TimeUnit.MINUTES.sleep(1);
  }
}
