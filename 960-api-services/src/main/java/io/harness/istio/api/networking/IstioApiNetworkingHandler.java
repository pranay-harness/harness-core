package io.harness.istio.api.networking;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.k8s.model.IstioDestinationWeight;
import io.harness.k8s.model.KubernetesConfig;
import io.harness.k8s.model.KubernetesResource;
import io.harness.logging.LogCallback;

import software.wings.api.ContainerServiceData;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import java.io.IOException;
import java.util.List;
import lombok.NonNull;
import me.snowdrop.istio.api.IstioResource;

@OwnedBy(HarnessTeam.CDP)
public interface IstioApiNetworkingHandler {
  void printVirtualServiceRouteWeights(IstioResource virtualService, String controllerPrefix, LogCallback logCallback);

  void updateVirtualServiceWithDestinationWeights(List<IstioDestinationWeight> istioDestinationWeights,
      HasMetadata virtualService, LogCallback executionLogCallback) throws IOException;

  void deleteHarnessManagedVirtualService(KubernetesConfig kubernetesConfig, HasMetadata virtualService,
      String virtualServiceName, LogCallback executionLogCallback);

  void deleteHarnessManagedDestinationRule(KubernetesConfig kubernetesConfig, HasMetadata destinationRule,
      String virtualServiceName, LogCallback executionLogCallback);

  HasMetadata updateDestinationRuleManifestFilesWithSubsets(List<String> subsets, KubernetesResource kubernetesResource,
      KubernetesClient kubernetesClient, HasMetadata destinationRule) throws IOException;

  HasMetadata updateVirtualServiceManifestFilesWithRoutes(KubernetesResource kubernetesResource,
      KubernetesConfig kubernetesConfig, List<IstioDestinationWeight> istioDestinationWeights,
      LogCallback executionLogCallback, KubernetesClient kubernetesClient, HasMetadata virtualService)
      throws IOException;

  @NonNull
  IstioResource createVirtualServiceDefinition(
      List<ContainerServiceData> allData, IstioResource existingVirtualService, String kubernetesServiceName);

  boolean virtualServiceHttpRouteMatchesExisting(
      IstioResource existingVirtualService, HasMetadata virtualServiceDefinition);
}
