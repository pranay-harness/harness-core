package io.harness.perpetualtask.k8s.metrics.client;

import static io.harness.rule.OwnerRule.AVMOHAN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.fabric8.kubernetes.client.Client;
import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class K8sMetricsExtensionAdapterTest extends CategoryTest {
  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void testGetExtensionType() throws Exception {
    final K8sMetricsExtensionAdapter adapter = new K8sMetricsExtensionAdapter();
    assertThat(adapter.getExtensionType()).isEqualTo(K8sMetricsClient.class);
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldBeAdaptable() throws Exception {
    final K8sMetricsExtensionAdapter adapter = new K8sMetricsExtensionAdapter();
    Client client = mock(Client.class);
    assertThat(adapter.isAdaptable(client)).isTrue();
  }
}
