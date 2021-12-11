package io.harness.istio.api.networking;

import static io.harness.istio.api.networking.IstioApiNetworkingUtils.getCustomResourceDefinition;
import static io.harness.rule.OwnerRule.TATHAGAT;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinitionList;
import io.fabric8.kubernetes.api.model.apiextensions.DoneableCustomResourceDefinition;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import me.snowdrop.istio.api.networking.v1alpha3.VirtualServiceBuilder;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class IstioApiNetworkingUtilsTest extends CategoryTest {
  @Mock
  private NonNamespaceOperation<CustomResourceDefinition, CustomResourceDefinitionList,
      DoneableCustomResourceDefinition, Resource<CustomResourceDefinition, DoneableCustomResourceDefinition>>
      customResourceDefinitionOperation;
  @Mock private Resource<CustomResourceDefinition, DoneableCustomResourceDefinition> customResourceDefinition;
  @Mock private CustomResourceDefinition virtualService;

  @Before
  public void setup() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = TATHAGAT)
  @Category(UnitTests.class)
  public void testGetCustomResourceDefinitionFail() {
    KubernetesClient kubernetesClient = mock(KubernetesClient.class);
    when(kubernetesClient.customResourceDefinitions()).thenReturn(customResourceDefinitionOperation);
    when(customResourceDefinitionOperation.withName("virtualservices.networking.istio.io"))
        .thenReturn(customResourceDefinition);
    when(customResourceDefinition.get()).thenReturn(null);
    assertThatThrownBy(() -> getCustomResourceDefinition(kubernetesClient, new VirtualServiceBuilder().build()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Custom Resource Definition virtualservices.networking.istio.io is not found in cluster");

    verify(customResourceDefinitionOperation, times(1)).withName("virtualservices.networking.istio.io");
  }

  @Test
  @Owner(developers = TATHAGAT)
  @Category(UnitTests.class)
  public void testGetCustomResourceDefinition() {
    KubernetesClient kubernetesClient = mock(KubernetesClient.class);
    when(kubernetesClient.customResourceDefinitions()).thenReturn(customResourceDefinitionOperation);
    when(customResourceDefinitionOperation.withName("virtualservices.networking.istio.io"))
        .thenReturn(customResourceDefinition);
    when(customResourceDefinition.get()).thenReturn(virtualService);
    assertThat(getCustomResourceDefinition(kubernetesClient, new VirtualServiceBuilder().build()))
        .isSameAs(virtualService);
    verify(customResourceDefinitionOperation, times(1)).withName("virtualservices.networking.istio.io");
  }
}