package io.harness.istio.api.networking;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.istio.api.networking.IstioApiNetworkingHandlerHelper.getCustomResourceDefinition;
import static io.harness.k8s.KubernetesConvention.DASH;
import static io.harness.k8s.KubernetesConvention.getRevisionFromControllerName;

import static software.wings.beans.command.KubernetesSetupCommandUnit.HARNESS_KUBERNETES_MANAGED_LABEL_KEY;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.k8s.KubernetesHelperService;
import io.harness.k8s.model.HarnessLabelValues;
import io.harness.k8s.model.HarnessLabels;
import io.harness.k8s.model.IstioDestinationWeight;
import io.harness.k8s.model.KubernetesConfig;
import io.harness.k8s.model.KubernetesResource;
import io.harness.logging.LogCallback;
import io.harness.serializer.YamlUtils;

import software.wings.api.ContainerServiceData;
import software.wings.beans.command.ExecutionLogCallback;

import com.google.inject.Inject;
import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.KubernetesClient;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import me.snowdrop.istio.api.IstioResource;
import me.snowdrop.istio.api.networking.v1alpha3.Destination;
import me.snowdrop.istio.api.networking.v1alpha3.DestinationRule;
import me.snowdrop.istio.api.networking.v1alpha3.DestinationRuleBuilder;
import me.snowdrop.istio.api.networking.v1alpha3.DoneableDestinationRule;
import me.snowdrop.istio.api.networking.v1alpha3.DoneableVirtualService;
import me.snowdrop.istio.api.networking.v1alpha3.HTTPRoute;
import me.snowdrop.istio.api.networking.v1alpha3.HTTPRouteDestination;
import me.snowdrop.istio.api.networking.v1alpha3.PortSelector;
import me.snowdrop.istio.api.networking.v1alpha3.Subset;
import me.snowdrop.istio.api.networking.v1alpha3.TCPRoute;
import me.snowdrop.istio.api.networking.v1alpha3.TLSRoute;
import me.snowdrop.istio.api.networking.v1alpha3.VirtualService;
import me.snowdrop.istio.api.networking.v1alpha3.VirtualServiceBuilder;
import me.snowdrop.istio.api.networking.v1alpha3.VirtualServiceFluent.SpecNested;
import me.snowdrop.istio.api.networking.v1alpha3.VirtualServiceSpec;
import me.snowdrop.istio.api.networking.v1alpha3.VirtualServiceSpecFluent.HttpNested;
import me.snowdrop.istio.client.IstioClient;

@Slf4j
@OwnedBy(HarnessTeam.CDP)
public class V1Alpha3IstioApiNetworkingHandler implements IstioApiNetworkingHandler {
  @Inject KubernetesHelperService kubernetesHelperService;

  public void printVirtualServiceRouteWeights(
      IstioResource virtualService, String controllerPrefix, LogCallback logCallback) {
    VirtualServiceSpec virtualServiceSpec = ((VirtualService) virtualService).getSpec();
    if (isNotEmpty(virtualServiceSpec.getHttp().get(0).getRoute())) {
      List<HTTPRouteDestination> sorted = virtualServiceSpec.getHttp().get(0).getRoute();
      sorted.sort(Comparator.comparing(a -> Integer.valueOf(a.getDestination().getSubset())));
      for (HTTPRouteDestination httpRouteDestination : sorted) {
        int weight = httpRouteDestination.getWeight();
        String rev = httpRouteDestination.getDestination().getSubset();
        logCallback.saveExecutionLog(format("   %s%s%s: %d%%", controllerPrefix, DASH, rev, weight));
      }
    } else {
      logCallback.saveExecutionLog("   None specified");
    }
  }

  @Override
  public void updateVirtualServiceWithDestinationWeights(List<IstioDestinationWeight> istioDestinationWeights,
      HasMetadata virtualService, LogCallback executionLogCallback) throws IOException {
    VirtualService v1Aplha3VirtualService = (VirtualService) virtualService;
    validateRoutesInVirtualService(v1Aplha3VirtualService);

    executionLogCallback.saveExecutionLog("\nUpdating VirtualService with destination weights");

    List<HTTPRoute> http = v1Aplha3VirtualService.getSpec().getHttp();
    if (isNotEmpty(http)) {
      String host = getHostFromRoute(http.get(0).getRoute());
      PortSelector portSelector = getPortSelectorFromRoute(http.get(0).getRoute());
      http.get(0).setRoute(generateDestinationWeights(istioDestinationWeights, host, portSelector));
    }
  }

  @Override
  public void deleteHarnessManagedVirtualService(KubernetesConfig kubernetesConfig, HasMetadata virtualService,
      String virtualServiceName, ExecutionLogCallback executionLogCallback) {
    if (null != virtualService) {
      VirtualService alpha3VirtualService = (VirtualService) virtualService;
      if (alpha3VirtualService.getMetadata().getLabels().containsKey(HARNESS_KUBERNETES_MANAGED_LABEL_KEY)) {
        executionLogCallback.saveExecutionLog("Deleting Istio VirtualService" + virtualServiceName);
        deleteIstioVirtualService(kubernetesConfig, virtualServiceName);
      }
    }
  }

  @Override
  public void deleteHarnessManagedDestinationRule(KubernetesConfig kubernetesConfig, HasMetadata destinationRule,
      String virtualServiceName, ExecutionLogCallback executionLogCallback) {
    if (destinationRule != null) {
      DestinationRule alpha3destinationRule = (DestinationRule) destinationRule;

      if (alpha3destinationRule.getMetadata().getLabels().containsKey(HARNESS_KUBERNETES_MANAGED_LABEL_KEY)) {
        executionLogCallback.saveExecutionLog("Deleting Istio DestinationRule" + virtualServiceName);
        deleteIstioDestinationRule(kubernetesConfig, virtualServiceName);
      }
    }
  }

  @Override
  public HasMetadata updateDestinationRuleManifestFilesWithSubsets(List<String> subsets,
      KubernetesResource kubernetesResource, KubernetesClient kubernetesClient, HasMetadata destinationRule)
      throws IOException {
    DestinationRule destinationRuleAlpha3 = (DestinationRule) destinationRule;
    kubernetesClient.customResources(
        getCustomResourceDefinition(kubernetesClient, new DestinationRuleBuilder().build()), DestinationRule.class,
        KubernetesResourceList.class, DoneableDestinationRule.class);

    destinationRuleAlpha3.getSpec().setSubsets(generateSubsetsForDestinationRule(subsets));

    kubernetesResource.setSpec(KubernetesHelper.toYaml(destinationRule));

    return destinationRule;
  }

  @Override
  public HasMetadata updateVirtualServiceManifestFilesWithRoutes(KubernetesResource kubernetesResource,
      KubernetesConfig kubernetesConfig, List<IstioDestinationWeight> istioDestinationWeights,
      LogCallback executionLogCallback, KubernetesClient kubernetesClient, HasMetadata virtualService)
      throws IOException {
    VirtualService virtualServiceAlpha3 = (VirtualService) virtualService;

    kubernetesClient.customResources(getCustomResourceDefinition(kubernetesClient, new VirtualServiceBuilder().build()),
        VirtualService.class, KubernetesResourceList.class, DoneableVirtualService.class);

    updateVirtualServiceWithDestinationWeights(istioDestinationWeights, virtualServiceAlpha3, executionLogCallback);

    kubernetesResource.setSpec(KubernetesHelper.toYaml(virtualServiceAlpha3));
    return virtualService;
  }

  @Override
  public IstioResource createVirtualServiceDefinition(
      List<ContainerServiceData> allData, IstioResource existingVirtualService, String kubernetesServiceName) {
    VirtualServiceSpec existingVirtualServiceSpec = ((VirtualService) existingVirtualService).getSpec();

    SpecNested<VirtualServiceBuilder> virtualServiceSpecNested =
        new VirtualServiceBuilder()
            .withApiVersion(existingVirtualService.getApiVersion())
            .withKind(existingVirtualService.getKind())
            .withNewMetadata()
            .withName(existingVirtualService.getMetadata().getName())
            .withNamespace(existingVirtualService.getMetadata().getNamespace())
            .withAnnotations(existingVirtualService.getMetadata().getAnnotations())
            .withLabels(existingVirtualService.getMetadata().getLabels())
            .endMetadata()
            .withNewSpec()
            .withHosts(existingVirtualServiceSpec.getHosts())
            .withGateways(existingVirtualServiceSpec.getGateways());

    HttpNested virtualServiceHttpNested = virtualServiceSpecNested.addNewHttp();

    for (ContainerServiceData containerServiceData : allData) {
      String controllerName = containerServiceData.getName();
      Optional<Integer> revision = getRevisionFromControllerName(controllerName);
      if (revision.isPresent()) {
        int weight = containerServiceData.getDesiredTraffic();
        if (weight > 0) {
          Destination destination = new Destination();
          destination.setHost(kubernetesServiceName);
          destination.setSubset(Integer.toString(revision.get()));
          HTTPRouteDestination HTTPRouteDestination = new HTTPRouteDestination();
          HTTPRouteDestination.setWeight(weight);
          HTTPRouteDestination.setDestination(destination);
          virtualServiceHttpNested.addToRoute(HTTPRouteDestination);
        }
      }
    }
    virtualServiceHttpNested.endHttp();
    return virtualServiceSpecNested.endSpec().build();
  }

  @Override
  public boolean virtualServiceHttpRouteMatchesExisting(
      IstioResource existingVirtualService, HasMetadata virtualService) {
    if (existingVirtualService == null) {
      return false;
    }

    HTTPRoute virtualServiceHttpRoute = (((VirtualService) virtualService).getSpec()).getHttp().get(0);
    HTTPRoute existingVirtualServiceHttpRoute = (((VirtualService) existingVirtualService).getSpec()).getHttp().get(0);

    if ((virtualServiceHttpRoute == null || existingVirtualServiceHttpRoute == null)
        && virtualServiceHttpRoute != existingVirtualServiceHttpRoute) {
      return false;
    }

    List<HTTPRouteDestination> sorted = new ArrayList<>(virtualServiceHttpRoute.getRoute());
    List<HTTPRouteDestination> existingSorted = new ArrayList<>(existingVirtualServiceHttpRoute.getRoute());
    Comparator<HTTPRouteDestination> comparator =
        Comparator.comparing(a -> Integer.valueOf(a.getDestination().getSubset()));
    sorted.sort(comparator);
    existingSorted.sort(comparator);

    for (int i = 0; i < sorted.size(); i++) {
      HTTPRouteDestination dw1 = sorted.get(i);
      HTTPRouteDestination dw2 = existingSorted.get(i);
      if (!dw1.getDestination().getSubset().equals(dw2.getDestination().getSubset())
          || !dw1.getWeight().equals(dw2.getWeight())) {
        return false;
      }
    }

    return true;
  }

  private List<Subset> generateSubsetsForDestinationRule(List<String> subsetNames) {
    List<Subset> subsets = new ArrayList<>();

    for (String subsetName : subsetNames) {
      Subset subset = new Subset();
      subset.setName(subsetName);

      if (subsetName.equals(HarnessLabelValues.trackCanary)) {
        Map<String, String> labels = new HashMap<>();
        labels.put(HarnessLabels.track, HarnessLabelValues.trackCanary);
        subset.setLabels(labels);
      } else if (subsetName.equals(HarnessLabelValues.trackStable)) {
        Map<String, String> labels = new HashMap<>();
        labels.put(HarnessLabels.track, HarnessLabelValues.trackStable);
        subset.setLabels(labels);
      } else if (subsetName.equals(HarnessLabelValues.colorBlue)) {
        Map<String, String> labels = new HashMap<>();
        labels.put(HarnessLabels.color, HarnessLabelValues.colorBlue);
        subset.setLabels(labels);
      } else if (subsetName.equals(HarnessLabelValues.colorGreen)) {
        Map<String, String> labels = new HashMap<>();
        labels.put(HarnessLabels.color, HarnessLabelValues.colorGreen);
        subset.setLabels(labels);
      }

      subsets.add(subset);
    }

    return subsets;
  }

  private void deleteIstioDestinationRule(KubernetesConfig kubernetesConfig, String name) {
    IstioClient istioClient = kubernetesHelperService.getIstioClient(kubernetesConfig);
    try {
      istioClient.unregisterCustomResource(new DestinationRuleBuilder()
                                               .withNewMetadata()
                                               .withName(name)
                                               .withNamespace(kubernetesConfig.getNamespace())
                                               .endMetadata()
                                               .build());
    } catch (Exception e) {
      log.info(e.getMessage());
    }
  }

  public void deleteIstioVirtualService(KubernetesConfig kubernetesConfig, String name) {
    IstioClient istioClient = kubernetesHelperService.getIstioClient(kubernetesConfig);
    try {
      istioClient.unregisterCustomResource(new VirtualServiceBuilder()
                                               .withNewMetadata()
                                               .withName(name)
                                               .withNamespace(kubernetesConfig.getNamespace())
                                               .endMetadata()
                                               .build());
    } catch (Exception e) {
      log.info(e.getMessage());
    }
  }

  private void validateRoutesInVirtualService(VirtualService virtualService) {
    List<HTTPRoute> http = virtualService.getSpec().getHttp();
    List<TCPRoute> tcp = virtualService.getSpec().getTcp();
    List<TLSRoute> tls = virtualService.getSpec().getTls();

    if (isEmpty(http)) {
      throw new InvalidRequestException(
          "Http route is not present in VirtualService. Only Http routes are allowed", USER);
    }

    if (isNotEmpty(tcp) || isNotEmpty(tls)) {
      throw new InvalidRequestException("Only Http routes are allowed in VirtualService for Traffic split", USER);
    }

    if (http.size() > 1) {
      throw new InvalidRequestException("Only one route is allowed in VirtualService", USER);
    }
  }

  private String getHostFromRoute(List<HTTPRouteDestination> routes) {
    if (isEmpty(routes)) {
      throw new InvalidRequestException("No routes exist in VirtualService", USER);
    }

    if (null == routes.get(0).getDestination()) {
      throw new InvalidRequestException("No destination exist in VirtualService", USER);
    }

    if (isBlank(routes.get(0).getDestination().getHost())) {
      throw new InvalidRequestException("No host exist in VirtualService", USER);
    }

    return routes.get(0).getDestination().getHost();
  }

  private PortSelector getPortSelectorFromRoute(List<HTTPRouteDestination> routes) {
    return routes.get(0).getDestination().getPort();
  }

  private List<HTTPRouteDestination> generateDestinationWeights(
      List<IstioDestinationWeight> istioDestinationWeights, String host, PortSelector portSelector) throws IOException {
    List<HTTPRouteDestination> httpRouteDestinations = new ArrayList<>();

    for (IstioDestinationWeight istioDestinationWeight : istioDestinationWeights) {
      String destinationYaml =
          IstioApiNetworkingHandlerHelper.getDestinationYaml(istioDestinationWeight.getDestination(), host);
      Destination destination = new YamlUtils().read(destinationYaml, Destination.class);
      destination.setPort(portSelector);

      HTTPRouteDestination httpRouteDestination = new HTTPRouteDestination();
      httpRouteDestination.setWeight(Integer.parseInt(istioDestinationWeight.getWeight()));
      httpRouteDestination.setDestination(destination);

      httpRouteDestinations.add(httpRouteDestination);
    }

    return httpRouteDestinations;
  }

  private DestinationRule getIstioDestinationRule(KubernetesConfig kubernetesConfig, String name) {
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
}
