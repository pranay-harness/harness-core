package io.harness.perpetualtask.k8s.metrics.client;

import static io.harness.rule.OwnerRule.AVMOHAN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import io.fabric8.kubernetes.api.model.RootPaths;
import io.fabric8.kubernetes.client.Client;
import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.net.URL;
import java.util.Arrays;

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
  public void shouldNotBeAdaptableIfMetricPathNotInRootPaths() throws Exception {
    final K8sMetricsExtensionAdapter adapter = new K8sMetricsExtensionAdapter();
    Client client = mock(Client.class);
    given(client.getMasterUrl()).willReturn(new URL("https://example-master1/"));
    RootPaths rootPaths = new RootPaths(Arrays.asList("/api", "/api/v1", "/healthz"));
    given(client.rootPaths()).willReturn(rootPaths);
    assertThat(adapter.isAdaptable(client)).isFalse();
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldBeAdaptableIfMetricPathInRootPaths() throws Exception {
    final K8sMetricsExtensionAdapter adapter = new K8sMetricsExtensionAdapter();
    Client client = mock(Client.class);
    given(client.getMasterUrl()).willReturn(new URL("https://example-master2/"));
    RootPaths rootPaths = new RootPaths(
        Arrays.asList("/api", "/api/v1", "/healthz", "/apis/metrics.k8s.io", "/apis/metrics.k8s.io/v1beta1"));
    given(client.rootPaths()).willReturn(rootPaths);
    assertThat(adapter.isAdaptable(client)).isTrue();
  }
}