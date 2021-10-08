package io.harness.istio.api.networking;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.k8s.model.KubernetesConfig;
import io.harness.k8s.model.KubernetesResourceId;

import com.google.inject.Inject;
import io.fabric8.kubernetes.api.model.HasMetadata;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.CDP)
public class IstioApiNetworkingService {
  @Inject public IstioApiNetworkingV1Beta1Service istioApiNetworkingV1Beta1Service;
  @Inject public IstioApiNetworkingV1Alpha3Service istioApiNetworkingV1Alpha3Service;

  public Map<String, String> getVirtualServiceResourcesAnnotations(
      KubernetesResourceId resourceId, KubernetesConfig kubernetesConfig) {
    HasMetadata istioAlpha3VirtualService =
        istioApiNetworkingV1Alpha3Service.getIstioAlpha3VirtualService(kubernetesConfig, resourceId.getName());
    if (istioAlpha3VirtualService != null) {
      return getResourcesMetadataAnnotations(istioAlpha3VirtualService);
    }

    HasMetadata istioBeta1VirtualService =
        istioApiNetworkingV1Beta1Service.getIstioBeta1VirtualService(kubernetesConfig, resourceId.getName());
    if (istioBeta1VirtualService != null) {
      return getResourcesMetadataAnnotations(istioBeta1VirtualService);
    }
    return Collections.emptyMap();
  }

  public Map<String, Integer> getVirtualServiceTrafficWeights(
      KubernetesConfig kubernetesConfig, String serviceName, String controllerNamePrefix) {
    HasMetadata istioAlpha3VirtualService =
        istioApiNetworkingV1Alpha3Service.getIstioAlpha3VirtualService(kubernetesConfig, serviceName);
    if (istioAlpha3VirtualService != null) {
      return istioApiNetworkingV1Alpha3Service.getTrafficWeights(istioAlpha3VirtualService, controllerNamePrefix);
    }

    HasMetadata istioBeta1VirtualService =
        istioApiNetworkingV1Beta1Service.getIstioBeta1VirtualService(kubernetesConfig, serviceName);
    if (istioBeta1VirtualService != null) {
      return istioApiNetworkingV1Beta1Service.getTrafficWeights(istioBeta1VirtualService, controllerNamePrefix);
    }
    return Collections.emptyMap();
  }

  public int getTrafficPercent(KubernetesConfig kubernetesConfig, String serviceName, Optional<Integer> revision) {
    if (!revision.isPresent()) {
      HasMetadata istioAlpha3VirtualService =
          istioApiNetworkingV1Alpha3Service.getIstioAlpha3VirtualService(kubernetesConfig, serviceName);
      if (istioAlpha3VirtualService != null) {
        return istioApiNetworkingV1Alpha3Service.getTrafficPercentage(istioAlpha3VirtualService, revision.get());
      }
      HasMetadata istioBeta1VirtualService =
          istioApiNetworkingV1Beta1Service.getIstioBeta1VirtualService(kubernetesConfig, serviceName);
      if (istioBeta1VirtualService != null) {
        return istioApiNetworkingV1Beta1Service.getTrafficPercentage(istioBeta1VirtualService, revision.get());
      }
    }
    return 0;
  }

  public HasMetadata getIstioVirtualService(KubernetesConfig kubernetesConfig, String serviceName) {
    HasMetadata istioAlpha3VirtualService =
        istioApiNetworkingV1Alpha3Service.getIstioAlpha3VirtualService(kubernetesConfig, serviceName);
    if (istioAlpha3VirtualService != null) {
      return istioAlpha3VirtualService;
    }

    HasMetadata istioBeta1VirtualService =
        istioApiNetworkingV1Beta1Service.getIstioBeta1VirtualService(kubernetesConfig, serviceName);
    if (istioBeta1VirtualService != null) {
      return istioBeta1VirtualService;
    }
    return null;
  }

  public static Map<String, String> getResourcesMetadataAnnotations(HasMetadata hasMetadata) {
    if (hasMetadata != null && hasMetadata.getMetadata() != null
        && isNotEmpty(hasMetadata.getMetadata().getAnnotations())) {
      return hasMetadata.getMetadata().getAnnotations();
    }
    return Collections.emptyMap();
  }
}
