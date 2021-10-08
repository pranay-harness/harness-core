package io.harness.istio.api.networking;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.istio.api.networking.IstioApiNetworkingUtils.getCustomResourceDefinition;
import static io.harness.k8s.KubernetesConvention.DASH;

import static java.util.stream.Collectors.toMap;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.k8s.KubernetesHelperService;
import io.harness.k8s.model.KubernetesConfig;

import com.google.inject.Inject;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.KubernetesClient;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import me.snowdrop.istio.api.networking.v1beta1.DestinationRule;
import me.snowdrop.istio.api.networking.v1beta1.DestinationRuleBuilder;
import me.snowdrop.istio.api.networking.v1beta1.DoneableDestinationRule;
import me.snowdrop.istio.api.networking.v1beta1.HTTPRouteDestination;
import me.snowdrop.istio.api.networking.v1beta1.VirtualService;
import me.snowdrop.istio.api.networking.v1beta1.VirtualServiceSpec;

@Slf4j
@OwnedBy(HarnessTeam.CDP)
public class IstioApiNetworkingV1Beta1Service {
  @Inject private KubernetesHelperService kubernetesHelperService;

  public DestinationRule getV1Beta1IstioDestinationRule(KubernetesConfig kubernetesConfig, String name) {
    KubernetesClient kubernetesClient = kubernetesHelperService.getKubernetesClient(kubernetesConfig);
    try {
      DestinationRule destinationRule = new DestinationRuleBuilder().build();

      return kubernetesClient
          .customResources(getCustomResourceDefinition(kubernetesClient, destinationRule), DestinationRule.class,
              KubernetesResourceList.class, DoneableDestinationRule.class)
          .inNamespace(kubernetesConfig.getNamespace())
          .withName(name)
          .get();
    } catch (Exception e) {
      log.error("Failed to get istio DestinationRule/{}", name, e);
      return null;
    }
  }

  public me.snowdrop.istio.api.networking.v1beta1.VirtualService getIstioBeta1VirtualService(
      KubernetesConfig kubernetesConfig, String name) {
    KubernetesClient kubernetesClient = kubernetesHelperService.getKubernetesClient(kubernetesConfig);
    try {
      me.snowdrop.istio.api.networking.v1beta1.VirtualService virtualService =
          new me.snowdrop.istio.api.networking.v1beta1.VirtualServiceBuilder().build();

      return kubernetesClient
          .customResources(getCustomResourceDefinition(kubernetesClient, virtualService),
              me.snowdrop.istio.api.networking.v1beta1.VirtualService.class, KubernetesResourceList.class,
              me.snowdrop.istio.api.networking.v1beta1.DoneableVirtualService.class)
          .inNamespace(kubernetesConfig.getNamespace())
          .withName(name)
          .get();
    } catch (Exception e) {
      log.error("Failed to get istio VirtualService/{}", name, e);
      return null;
    }
  }

  public Map<String, Integer> getTrafficWeights(HasMetadata istioBeta1VirtualService, String controllerNamePrefix) {
    VirtualService virtualService = (VirtualService) istioBeta1VirtualService;
    VirtualServiceSpec virtualServiceSpec = virtualService.getSpec();
    if (isEmpty(virtualServiceSpec.getHttp()) || isEmpty(virtualServiceSpec.getHttp().get(0).getRoute())) {
      return new HashMap<>();
    }
    List<HTTPRouteDestination> httpRouteDestinations = virtualServiceSpec.getHttp().get(0).getRoute();
    return httpRouteDestinations.stream().collect(
        toMap(dw -> controllerNamePrefix + DASH + dw.getDestination().getSubset(), HTTPRouteDestination::getWeight));
  }

  public int getTrafficPercentage(HasMetadata istioVirtualService, Integer revision) {
    VirtualService virtualService = (VirtualService) istioVirtualService;
    VirtualServiceSpec virtualServiceSpec = virtualService.getSpec();
    if (isEmpty(virtualServiceSpec.getHttp()) || isEmpty(virtualServiceSpec.getHttp().get(0).getRoute())) {
      return 0;
    }

    return virtualServiceSpec.getHttp()
        .get(0)
        .getRoute()
        .stream()
        .filter(dw -> Integer.toString(revision).equals(dw.getDestination().getSubset()))
        .map(HTTPRouteDestination::getWeight)
        .findFirst()
        .orElse(0);
  }
}
