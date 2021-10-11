package io.harness.istio.api.networking;

import static io.harness.istio.api.networking.IstioApiNetworkingUtils.HARNESS_KUBERNETES_MANAGED_LABEL_KEY;
import static io.harness.k8s.model.K8sExpressions.canaryDestinationExpression;
import static io.harness.k8s.model.K8sExpressions.stableDestinationExpression;
import static io.harness.rule.OwnerRule.TATHAGAT;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.k8s.KubernetesHelperService;
import io.harness.k8s.model.HarnessLabelValues;
import io.harness.k8s.model.IstioDestinationWeight;
import io.harness.k8s.model.KubernetesConfig;
import io.harness.k8s.model.KubernetesResource;
import io.harness.logging.LogCallback;
import io.harness.rule.Owner;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinitionList;
import io.fabric8.kubernetes.api.model.apiextensions.DoneableCustomResourceDefinition;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.kubernetes.client.openapi.ApiException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import me.snowdrop.istio.api.networking.v1alpha3.Destination;
import me.snowdrop.istio.api.networking.v1alpha3.DestinationBuilder;
import me.snowdrop.istio.api.networking.v1alpha3.DestinationRule;
import me.snowdrop.istio.api.networking.v1alpha3.DestinationRuleBuilder;
import me.snowdrop.istio.api.networking.v1alpha3.DestinationRuleSpec;
import me.snowdrop.istio.api.networking.v1alpha3.DestinationRuleSpecBuilder;
import me.snowdrop.istio.api.networking.v1alpha3.HTTPRoute;
import me.snowdrop.istio.api.networking.v1alpha3.HTTPRouteBuilder;
import me.snowdrop.istio.api.networking.v1alpha3.HTTPRouteDestination;
import me.snowdrop.istio.api.networking.v1alpha3.PortSelector;
import me.snowdrop.istio.api.networking.v1alpha3.Subset;
import me.snowdrop.istio.api.networking.v1alpha3.TCPRoute;
import me.snowdrop.istio.api.networking.v1alpha3.VirtualService;
import me.snowdrop.istio.api.networking.v1alpha3.VirtualServiceBuilder;
import me.snowdrop.istio.api.networking.v1alpha3.VirtualServiceSpec;
import me.snowdrop.istio.api.networking.v1alpha3.VirtualServiceSpecBuilder;
import me.snowdrop.istio.client.IstioClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(HarnessTeam.CDP)
public class V1Alpha3IstioApiNetworkingHandlerTest extends CategoryTest {
  @Mock private LogCallback logCallback;
  @Mock private KubernetesHelperService kubernetesHelperService;
  @Mock private KubernetesClient kubernetesClient;
  @Mock
  private NonNamespaceOperation<CustomResourceDefinition, CustomResourceDefinitionList,
      DoneableCustomResourceDefinition, Resource<CustomResourceDefinition, DoneableCustomResourceDefinition>>
      customResourceDefinitionOperation;
  @Mock private Resource<CustomResourceDefinition, DoneableCustomResourceDefinition> customResourceDefinition;
  @Mock private CustomResourceDefinition resourceDefinition;
  @InjectMocks
  private V1Alpha3IstioApiNetworkingHandler v1Alpha3IstioApiNetworkingHandler = new V1Alpha3IstioApiNetworkingHandler();
  private static final String UUID = "uuid";
  private static final VirtualServiceSpec defaultServiceSpec =
      new VirtualServiceSpecBuilder().withHttp(new HTTPRoute()).withTcp(new TCPRoute()).build();
  Map<String, String> defaultLabels = ImmutableMap.of(HARNESS_KUBERNETES_MANAGED_LABEL_KEY, "true");
  private static final DestinationRuleSpec defaultDestinationRuleSpec = new DestinationRuleSpecBuilder().build();

  @Before
  public void setup() throws ApiException {
    MockitoAnnotations.initMocks(this);
    when(kubernetesClient.customResources(any(), any(), any(), any())).thenReturn(null);
    when(kubernetesClient.customResourceDefinitions()).thenReturn(customResourceDefinitionOperation);
  }

  @Test
  @Owner(developers = TATHAGAT)
  @Category(UnitTests.class)
  public void testGenerateSubsetsForDestinationRule() {
    List<String> subsetNames = new ArrayList<>();
    subsetNames.add(HarnessLabelValues.trackCanary);
    subsetNames.add(HarnessLabelValues.trackStable);
    subsetNames.add(HarnessLabelValues.colorBlue);
    subsetNames.add(HarnessLabelValues.colorGreen);

    final List<Subset> result = v1Alpha3IstioApiNetworkingHandler.generateSubsetsForDestinationRule(subsetNames);

    assertThat(result.size()).isEqualTo(4);
  }

  @Test
  @Owner(developers = TATHAGAT)
  @Category(UnitTests.class)
  public void testDeleteHarnessManagedVirtualService() {
    IstioClient istioClient = mock(IstioClient.class);
    when(kubernetesHelperService.getIstioClient(any())).thenReturn(istioClient);
    v1Alpha3IstioApiNetworkingHandler.deleteHarnessManagedVirtualService(KubernetesConfig.builder().build(),
        getVirtualService(defaultLabels, defaultServiceSpec, UUID), "virtualServiceName", logCallback);
    verify(istioClient, times(1)).unregisterCustomResource(any());
    v1Alpha3IstioApiNetworkingHandler.deleteHarnessManagedVirtualService(KubernetesConfig.builder().build(),
        getVirtualService(null, defaultServiceSpec, UUID), "virtualServiceName", logCallback);
    // only once for the harness managed virtual service
    verify(istioClient, times(1)).unregisterCustomResource(any());
  }

  @Test
  @Owner(developers = TATHAGAT)
  @Category(UnitTests.class)
  public void testDeleteHarnessManagedDestinationRule() {
    IstioClient istioClient = mock(IstioClient.class);
    when(kubernetesHelperService.getIstioClient(any())).thenReturn(istioClient);

    v1Alpha3IstioApiNetworkingHandler.deleteHarnessManagedDestinationRule(KubernetesConfig.builder().build(),
        getDestinationRule(defaultLabels, defaultDestinationRuleSpec, UUID), "virtualServiceName", logCallback);

    v1Alpha3IstioApiNetworkingHandler.deleteHarnessManagedDestinationRule(KubernetesConfig.builder().build(),
        getDestinationRule(null, defaultDestinationRuleSpec, UUID), "virtualServiceName", logCallback);

    // only once for the harness managed destination rule
    verify(istioClient, times(1)).unregisterCustomResource(any());
  }

  @Test
  @Owner(developers = TATHAGAT)
  @Category(UnitTests.class)
  public void testUpdateDestinationRuleManifestFilesWithSubsets() throws IOException {
    DestinationRule destinationRule = getDestinationRule(defaultLabels, defaultDestinationRuleSpec, UUID);
    KubernetesResource kubernetesResource = KubernetesResource.builder().build();

    when(kubernetesClient.customResources(any(), any(), any(), any())).thenReturn(null);
    when(kubernetesClient.customResourceDefinitions()).thenReturn(customResourceDefinitionOperation);
    when(customResourceDefinitionOperation.withName("destinationrules.networking.istio.io"))
        .thenReturn(customResourceDefinition);
    when(customResourceDefinition.get()).thenReturn(resourceDefinition);

    v1Alpha3IstioApiNetworkingHandler.updateDestinationRuleManifestFilesWithSubsets(
        asList(HarnessLabelValues.trackCanary, HarnessLabelValues.trackStable), kubernetesResource, kubernetesClient,
        destinationRule);

    // assertThat(kubernetesResource.getSpec()).isNotBlank();
    assertThat(destinationRule.getSpec().getSubsets()).hasSize(2);
    assertThat(destinationRule.getSpec().getSubsets().stream().map(Subset::getName).collect(Collectors.toList()))
        .containsExactlyInAnyOrder("canary", "stable");

    assertThat(destinationRule.getSpec()
                   .getSubsets()
                   .stream()
                   .map(Subset::getLabels)
                   .map(Map::values)
                   .collect(Collectors.toList())
                   .stream()
                   .flatMap(Collection::stream)
                   .collect(Collectors.toList()))
        .containsExactlyInAnyOrder("canary", "stable");

    assertThat(kubernetesResource).isNotNull();
  }

  @Test
  @Owner(developers = TATHAGAT)
  @Category(UnitTests.class)
  public void testUpdateVirtualServiceManifestFilesWithRoutes() throws IOException {
    VirtualService virtualServiceInput = getVirtualService(defaultLabels, defaultServiceSpec, UUID);
    virtualServiceInput.getSpec().setHttp(asList(
        new HTTPRouteBuilder()
            .withRoute(new HTTPRouteDestination(
                new DestinationBuilder().withHost("localhost").withPort(new PortSelector(2304)).build(), null, null))
            .build()));
    virtualServiceInput.getSpec().setTcp(null);
    virtualServiceInput.getSpec().setTls(null);

    List<IstioDestinationWeight> destinationWeights =
        asList(IstioDestinationWeight.builder().destination(canaryDestinationExpression).weight("10").build(),
            IstioDestinationWeight.builder().destination(stableDestinationExpression).weight("40").build(),
            IstioDestinationWeight.builder().destination("host: test\nsubset: default").weight("50").build());

    when(customResourceDefinitionOperation.withName("virtualservices.networking.istio.io"))
        .thenReturn(customResourceDefinition);
    when(customResourceDefinition.get()).thenReturn(resourceDefinition);

    VirtualService virtualServiceOutput =
        (VirtualService) v1Alpha3IstioApiNetworkingHandler.updateVirtualServiceManifestFilesWithRoutes(
            KubernetesResource.builder().build(), KubernetesConfig.builder().build(), destinationWeights, logCallback,
            kubernetesClient, virtualServiceInput);

    List<HTTPRouteDestination> routes = virtualServiceOutput.getSpec().getHttp().get(0).getRoute();
    assertThat(routes.stream().map(HTTPRouteDestination::getWeight)).containsExactly(10, 40, 50);
    assertThat(routes.stream().map(HTTPRouteDestination::getDestination).map(Destination::getSubset))
        .containsExactly(HarnessLabelValues.trackCanary, HarnessLabelValues.trackStable, "default");
  }

  @Test
  @Owner(developers = TATHAGAT)
  @Category(UnitTests.class)
  public void testVirtualServiceHttpRouteMatchesExisting() {
    VirtualService existingVirtualService = null;
    VirtualService virtualService = getVirtualService(defaultLabels, defaultServiceSpec, "id2");
    assertThat(v1Alpha3IstioApiNetworkingHandler.virtualServiceHttpRouteMatchesExisting(
                   existingVirtualService, virtualService))
        .isFalse();

    List<HTTPRoute> httpRoutes = asList(new HTTPRouteBuilder()
                                            .withRoute(new HTTPRouteDestination(new DestinationBuilder()
                                                                                    .withSubset("subset")
                                                                                    .withHost("localhost")
                                                                                    .withPort(new PortSelector(2304))
                                                                                    .build(),
                                                null, 50))
                                            .build());

    existingVirtualService = getVirtualService(defaultLabels, defaultServiceSpec, "id1");
    existingVirtualService.getSpec().setHttp(httpRoutes);
    virtualService.getSpec().setHttp(httpRoutes);
    assertThat(v1Alpha3IstioApiNetworkingHandler.virtualServiceHttpRouteMatchesExisting(
                   existingVirtualService, virtualService))
        .isTrue();
  }

  private DestinationRule getDestinationRule(
      Map<String, String> labels, DestinationRuleSpec destinationRuleSpec, String uuid) {
    return new DestinationRuleBuilder()
        .withSpec(destinationRuleSpec)
        .withMetadata(new ObjectMetaBuilder()
                          .withUid(uuid)
                          .withName(uuid + "-name")
                          .withNamespace("default")
                          .withLabels(labels)
                          .build())
        .build();
  }

  private VirtualService getVirtualService(
      Map<String, String> labels, VirtualServiceSpec virtualServiceSpec, String uuid) {
    return new VirtualServiceBuilder()
        .withSpec(virtualServiceSpec)
        .withMetadata(new ObjectMetaBuilder()
                          .withUid(uuid)
                          .withName(uuid + "-name")
                          .withNamespace("default")
                          .withLabels(labels)
                          .build())
        .build();
  }
}