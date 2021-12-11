package io.harness.istio.api.networking;

import static io.harness.istio.api.networking.IstioApiNetworkingUtils.HARNESS_KUBERNETES_MANAGED_LABEL_KEY;
import static io.harness.rule.OwnerRule.TATHAGAT;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.k8s.model.KubernetesConfig;
import io.harness.k8s.model.KubernetesResourceId;
import io.harness.rule.Owner;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.kubernetes.client.openapi.ApiException;
import java.util.Map;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(HarnessTeam.CDP)
public class IstioApiNetworkingServiceTest extends CategoryTest {
  @Mock private IstioApiNetworkingV1Alpha3Service mockIstioApiNetworkingV1Alpha3Service;
  @Mock private IstioApiNetworkingV1Beta1Service mockIstioApiNetworkingV1Beta1Service;

  @InjectMocks private IstioApiNetworkingService istioApiNetworkingService = new IstioApiNetworkingService();

  private static final String UUID = "uuid";

  @Before
  public void setup() throws ApiException {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = TATHAGAT)
  @Category(UnitTests.class)
  public void testGetVirtualServiceResourcesAnnotations() {
    when(mockIstioApiNetworkingV1Alpha3Service.getIstioAlpha3VirtualService(any(), any()))
        .thenReturn(getAlpha3VirtualService(ImmutableMap.of(HARNESS_KUBERNETES_MANAGED_LABEL_KEY, "true")));
    // with alpha virtual service present
    assertThat(istioApiNetworkingService.getVirtualServiceResourcesAnnotations(
                   KubernetesResourceId.builder().build(), KubernetesConfig.builder().build()))
        .containsExactlyEntriesOf(ImmutableMap.of(HARNESS_KUBERNETES_MANAGED_LABEL_KEY, "true"));

    when(mockIstioApiNetworkingV1Alpha3Service.getIstioAlpha3VirtualService(any(), any())).thenReturn(null);
    when(mockIstioApiNetworkingV1Beta1Service.getIstioBeta1VirtualService(any(), any()))
        .thenReturn(getBeta1VirtualService(ImmutableMap.of(HARNESS_KUBERNETES_MANAGED_LABEL_KEY, "false")));
    // with beta virtual service present
    assertThat(istioApiNetworkingService.getVirtualServiceResourcesAnnotations(
                   KubernetesResourceId.builder().build(), KubernetesConfig.builder().build()))
        .containsExactlyEntriesOf(ImmutableMap.of(HARNESS_KUBERNETES_MANAGED_LABEL_KEY, "false"));
  }

  @Test
  @Owner(developers = TATHAGAT)
  @Category(UnitTests.class)
  public void testGetVirtualServiceResourcesAnnotationsNoService() {
    when(mockIstioApiNetworkingV1Alpha3Service.getIstioAlpha3VirtualService(any(), any())).thenReturn(null);
    when(mockIstioApiNetworkingV1Beta1Service.getIstioBeta1VirtualService(any(), any())).thenReturn(null);

    assertThat(istioApiNetworkingService.getVirtualServiceResourcesAnnotations(
                   KubernetesResourceId.builder().build(), KubernetesConfig.builder().build()))
        .isEmpty();
  }

  @Test
  @Owner(developers = TATHAGAT)
  @Category(UnitTests.class)
  public void testGetVirtualServiceTrafficWeights() {
    when(mockIstioApiNetworkingV1Alpha3Service.getIstioAlpha3VirtualService(any(), any()))
        .thenReturn(getAlpha3VirtualService(ImmutableMap.of(HARNESS_KUBERNETES_MANAGED_LABEL_KEY, "true")));
    when(mockIstioApiNetworkingV1Alpha3Service.getTrafficWeights(any(), any()))
        .thenReturn(ImmutableMap.of("primary", 30));
    assertThat(istioApiNetworkingService.getVirtualServiceTrafficWeights(
                   KubernetesConfig.builder().build(), "my-service", "controller-prefix"))
        .containsExactlyEntriesOf(ImmutableMap.of("primary", 30));

    when(mockIstioApiNetworkingV1Alpha3Service.getIstioAlpha3VirtualService(any(), any())).thenReturn(null);
    when(mockIstioApiNetworkingV1Beta1Service.getIstioBeta1VirtualService(any(), any()))
        .thenReturn(getBeta1VirtualService(ImmutableMap.of(HARNESS_KUBERNETES_MANAGED_LABEL_KEY, "false")));
    when(mockIstioApiNetworkingV1Beta1Service.getTrafficWeights(any(), any()))
        .thenReturn(ImmutableMap.of("canary", 40));
    assertThat(istioApiNetworkingService.getVirtualServiceTrafficWeights(
                   KubernetesConfig.builder().build(), "my-service", "controller-prefix"))
        .containsExactlyEntriesOf(ImmutableMap.of("canary", 40));
  }

  @Test
  @Owner(developers = TATHAGAT)
  @Category(UnitTests.class)
  public void testGetVirtualServiceTrafficWeightsNoService() {
    when(mockIstioApiNetworkingV1Alpha3Service.getIstioAlpha3VirtualService(any(), any())).thenReturn(null);
    when(mockIstioApiNetworkingV1Beta1Service.getIstioBeta1VirtualService(any(), any())).thenReturn(null);
    assertThat(istioApiNetworkingService.getVirtualServiceResourcesAnnotations(
                   KubernetesResourceId.builder().build(), KubernetesConfig.builder().build()))
        .isEmpty();
  }

  @Test
  @Owner(developers = TATHAGAT)
  @Category(UnitTests.class)
  public void testGetTrafficPercent() {
    when(mockIstioApiNetworkingV1Alpha3Service.getIstioAlpha3VirtualService(any(), any()))
        .thenReturn(getAlpha3VirtualService(ImmutableMap.of(HARNESS_KUBERNETES_MANAGED_LABEL_KEY, "true")));
    when(mockIstioApiNetworkingV1Beta1Service.getIstioBeta1VirtualService(any(), any()))
        .thenReturn(getBeta1VirtualService(ImmutableMap.of(HARNESS_KUBERNETES_MANAGED_LABEL_KEY, "false")));
    when(mockIstioApiNetworkingV1Alpha3Service.getTrafficPercentage(any(), anyInt())).thenReturn(10);
    when(mockIstioApiNetworkingV1Beta1Service.getTrafficPercentage(any(), anyInt())).thenReturn(20);
    assertThat(istioApiNetworkingService.getTrafficPercent(
                   KubernetesConfig.builder().build(), "service-name", Optional.of(10)))
        .isEqualTo(10);

    when(mockIstioApiNetworkingV1Alpha3Service.getIstioAlpha3VirtualService(any(), any())).thenReturn(null);
    assertThat(istioApiNetworkingService.getTrafficPercent(
                   KubernetesConfig.builder().build(), "service-name", Optional.of(10)))
        .isEqualTo(20);
  }

  @Test
  @Owner(developers = TATHAGAT)
  @Category(UnitTests.class)
  public void testGetTrafficPercentWithNoRevision() {
    when(mockIstioApiNetworkingV1Alpha3Service.getIstioAlpha3VirtualService(any(), any()))
        .thenReturn(getAlpha3VirtualService(ImmutableMap.of(HARNESS_KUBERNETES_MANAGED_LABEL_KEY, "true")));
    when(mockIstioApiNetworkingV1Beta1Service.getIstioBeta1VirtualService(any(), any()))
        .thenReturn(getBeta1VirtualService(ImmutableMap.of(HARNESS_KUBERNETES_MANAGED_LABEL_KEY, "false")));
    assertThat(istioApiNetworkingService.getTrafficPercent(
                   KubernetesConfig.builder().build(), "service-name", Optional.empty()))
        .isEqualTo(0);
    verify(mockIstioApiNetworkingV1Alpha3Service, never()).getTrafficPercentage(any(), any());
    verify(mockIstioApiNetworkingV1Beta1Service, never()).getTrafficPercentage(any(), any());
  }

  @Test
  @Owner(developers = TATHAGAT)
  @Category(UnitTests.class)
  public void testGetIstioVirtualService() {
    when(mockIstioApiNetworkingV1Alpha3Service.getIstioAlpha3VirtualService(any(), any()))
        .thenReturn(getAlpha3VirtualService(ImmutableMap.of(HARNESS_KUBERNETES_MANAGED_LABEL_KEY, "true")));

    istioApiNetworkingService.getIstioVirtualService(KubernetesConfig.builder().build(), "service-name");
    verify(mockIstioApiNetworkingV1Alpha3Service, times(1)).getIstioAlpha3VirtualService(any(), any());
    verify(mockIstioApiNetworkingV1Beta1Service, never()).getIstioBeta1VirtualService(any(), any());

    when(mockIstioApiNetworkingV1Alpha3Service.getIstioAlpha3VirtualService(any(), any())).thenReturn(null);

    istioApiNetworkingService.getIstioVirtualService(KubernetesConfig.builder().build(), "service-name");
    verify(mockIstioApiNetworkingV1Alpha3Service, times(2)).getIstioAlpha3VirtualService(any(), any());
    verify(mockIstioApiNetworkingV1Beta1Service, times(1)).getIstioBeta1VirtualService(any(), any());
  }

  private me.snowdrop.istio.api.networking.v1alpha3.VirtualService getAlpha3VirtualService(
      Map<String, String> annotations) {
    return new me.snowdrop.istio.api.networking.v1alpha3.VirtualServiceBuilder()
        .withMetadata(new ObjectMetaBuilder()
                          .withUid(UUID)
                          .withName(UUID + "-name")
                          .withNamespace("default")
                          .withAnnotations(annotations)
                          .build())
        .build();
  }

  private me.snowdrop.istio.api.networking.v1beta1.VirtualService getBeta1VirtualService(
      Map<String, String> annotations) {
    return new me.snowdrop.istio.api.networking.v1beta1.VirtualServiceBuilder()
        .withMetadata(new ObjectMetaBuilder()
                          .withUid(UUID)
                          .withName(UUID + "-name")
                          .withNamespace("default")
                          .withAnnotations(annotations)
                          .build())
        .build();
  }
}