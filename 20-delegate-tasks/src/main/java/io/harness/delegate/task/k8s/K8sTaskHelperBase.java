package io.harness.delegate.task.k8s;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.helm.HelmConstants.HELM_RELEASE_LABEL;
import static io.harness.k8s.manifest.ManifestHelper.getFirstLoadBalancerService;
import static io.harness.k8s.model.K8sExpressions.canaryDestinationExpression;
import static io.harness.k8s.model.K8sExpressions.stableDestinationExpression;
import static io.harness.logging.CommandExecutionStatus.FAILURE;
import static io.harness.logging.LogLevel.ERROR;
import static io.harness.logging.LogLevel.INFO;
import static io.harness.state.StateConstants.DEFAULT_STEADY_STATE_TIMEOUT;
import static io.harness.threading.Morpheus.sleep;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.Duration.ofMinutes;
import static java.time.Duration.ofSeconds;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.common.util.concurrent.UncheckedTimeoutException;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.api.model.LoadBalancerIngress;
import io.fabric8.kubernetes.api.model.LoadBalancerStatus;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.harness.container.ContainerInfo;
import io.harness.exception.InvalidRequestException;
import io.harness.k8s.KubernetesContainerService;
import io.harness.k8s.KubernetesHelperService;
import io.harness.k8s.kubectl.AbstractExecutable;
import io.harness.k8s.model.HarnessAnnotations;
import io.harness.k8s.model.HarnessLabelValues;
import io.harness.k8s.model.HarnessLabels;
import io.harness.k8s.model.IstioDestinationWeight;
import io.harness.k8s.model.K8sContainer;
import io.harness.k8s.model.K8sPod;
import io.harness.k8s.model.Kind;
import io.harness.k8s.model.KubernetesConfig;
import io.harness.k8s.model.KubernetesResource;
import io.harness.k8s.model.KubernetesResourceComparer;
import io.harness.k8s.model.KubernetesResourceId;
import io.harness.logging.LogCallback;
import io.harness.logging.LogLevel;
import io.harness.serializer.YamlUtils;
import lombok.extern.slf4j.Slf4j;
import me.snowdrop.istio.api.networking.v1alpha3.Destination;
import me.snowdrop.istio.api.networking.v1alpha3.DestinationRule;
import me.snowdrop.istio.api.networking.v1alpha3.DestinationRuleBuilder;
import me.snowdrop.istio.api.networking.v1alpha3.DestinationWeight;
import me.snowdrop.istio.api.networking.v1alpha3.DoneableDestinationRule;
import me.snowdrop.istio.api.networking.v1alpha3.DoneableVirtualService;
import me.snowdrop.istio.api.networking.v1alpha3.HTTPRoute;
import me.snowdrop.istio.api.networking.v1alpha3.PortSelector;
import me.snowdrop.istio.api.networking.v1alpha3.Subset;
import me.snowdrop.istio.api.networking.v1alpha3.TCPRoute;
import me.snowdrop.istio.api.networking.v1alpha3.TLSRoute;
import me.snowdrop.istio.api.networking.v1alpha3.VirtualService;
import me.snowdrop.istio.api.networking.v1alpha3.VirtualServiceBuilder;
import org.apache.commons.io.IOUtils;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.stream.LogOutputStream;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Singleton
@Slf4j
public class K8sTaskHelperBase {
  @Inject private TimeLimiter timeLimiter;
  @Inject private KubernetesContainerService kubernetesContainerService;
  @Inject private KubernetesHelperService kubernetesHelperService;

  public static final String ISTIO_DESTINATION_TEMPLATE = "host: $ISTIO_DESTINATION_HOST_NAME\n"
      + "subset: $ISTIO_DESTINATION_SUBSET_NAME";

  public static LogOutputStream getExecutionLogOutputStream(LogCallback executionLogCallback, LogLevel logLevel) {
    return new LogOutputStream() {
      @Override
      protected void processLine(String line) {
        executionLogCallback.saveExecutionLog(line, logLevel);
      }
    };
  }

  public static String getResourcesInStringFormat(List<KubernetesResourceId> resourceIds) {
    StringBuilder sb = new StringBuilder(1024);
    resourceIds.forEach(resourceId -> sb.append("\n- ").append(resourceId.namespaceKindNameRef()));
    return sb.toString();
  }

  public static long getTimeoutMillisFromMinutes(Integer timeoutMinutes) {
    if (timeoutMinutes == null || timeoutMinutes <= 0) {
      timeoutMinutes = DEFAULT_STEADY_STATE_TIMEOUT;
    }

    return ofMinutes(timeoutMinutes).toMillis();
  }

  public static LogOutputStream getEmptyLogOutputStream() {
    return new LogOutputStream() {
      @Override
      protected void processLine(String line) {}
    };
  }

  public static ProcessResult executeCommandSilent(AbstractExecutable command, String workingDirectory)
      throws Exception {
    try (LogOutputStream emptyLogOutputStream = getEmptyLogOutputStream();) {
      return command.execute(workingDirectory, emptyLogOutputStream, emptyLogOutputStream, false);
    }
  }

  public static ProcessResult executeCommand(
      AbstractExecutable command, String workingDirectory, LogCallback executionLogCallback) throws Exception {
    try (LogOutputStream logOutputStream = getExecutionLogOutputStream(executionLogCallback, INFO);
         LogOutputStream logErrorStream = getExecutionLogOutputStream(executionLogCallback, ERROR);) {
      return command.execute(workingDirectory, logOutputStream, logErrorStream, true);
    }
  }

  public List<K8sPod> getPodDetailsWithLabels(KubernetesConfig kubernetesConfig, String namespace, String releaseName,
      Map<String, String> labels, long timeoutInMillis) throws Exception {
    return timeLimiter.callWithTimeout(
        ()
            -> kubernetesContainerService.getRunningPodsWithLabels(kubernetesConfig, namespace, labels)
                   .stream()
                   .map(pod
                       -> K8sPod.builder()
                              .uid(pod.getMetadata().getUid())
                              .name(pod.getMetadata().getName())
                              .namespace(pod.getMetadata().getNamespace())
                              .releaseName(releaseName)
                              .podIP(pod.getStatus().getPodIP())
                              .containerList(pod.getStatus()
                                                 .getContainerStatuses()
                                                 .stream()
                                                 .map(container
                                                     -> K8sContainer.builder()
                                                            .containerId(container.getContainerID())
                                                            .name(container.getName())
                                                            .image(container.getImage())
                                                            .build())
                                                 .collect(toList()))
                              .labels(pod.getMetadata().getLabels())
                              .build())
                   .collect(toList()),
        timeoutInMillis, TimeUnit.MILLISECONDS, true);
  }

  public List<K8sPod> getPodDetailsWithTrack(KubernetesConfig kubernetesConfig, String namespace, String releaseName,
      String track, long timeoutInMillis) throws Exception {
    Map<String, String> labels = ImmutableMap.of(HarnessLabels.releaseName, releaseName, HarnessLabels.track, track);
    return getPodDetailsWithLabels(kubernetesConfig, namespace, releaseName, labels, timeoutInMillis);
  }

  public List<K8sPod> getPodDetailsWithColor(KubernetesConfig kubernetesConfig, String namespace, String releaseName,
      String color, long timeoutInMillis) throws Exception {
    Map<String, String> labels = ImmutableMap.of(HarnessLabels.releaseName, releaseName, HarnessLabels.color, color);
    return getPodDetailsWithLabels(kubernetesConfig, namespace, releaseName, labels, timeoutInMillis);
  }

  private Service waitForLoadBalancerService(
      KubernetesConfig kubernetesConfig, String serviceName, String namespace, int timeoutInSeconds) {
    try {
      return timeLimiter.callWithTimeout(() -> {
        while (true) {
          Service service = kubernetesContainerService.getService(kubernetesConfig, serviceName, namespace);

          LoadBalancerStatus loadBalancerStatus = service.getStatus().getLoadBalancer();
          if (!loadBalancerStatus.getIngress().isEmpty()) {
            return service;
          }
          int sleepTimeInSeconds = 5;
          logger.info("waitForLoadBalancerService: LoadBalancer Service {} not ready. Sleeping for {} seconds",
              serviceName, sleepTimeInSeconds);
          sleep(ofSeconds(sleepTimeInSeconds));
        }
      }, timeoutInSeconds, TimeUnit.SECONDS, true);
    } catch (UncheckedTimeoutException e) {
      logger.error("Timed out waiting for LoadBalancer service. Moving on.", e);
    } catch (Exception e) {
      logger.error("Exception while trying to get LoadBalancer service", e);
    }
    return null;
  }

  public String getLoadBalancerEndpoint(KubernetesConfig kubernetesConfig, List<KubernetesResource> resources) {
    KubernetesResource loadBalancerResource = getFirstLoadBalancerService(resources);
    if (loadBalancerResource == null) {
      return null;
    }

    // NOTE(hindwani): We are not using timeOutInMillis for waiting because of the bug: CDP-13872
    Service service = waitForLoadBalancerService(kubernetesConfig, loadBalancerResource.getResourceId().getName(),
        loadBalancerResource.getResourceId().getNamespace(), 60);

    if (service == null) {
      logger.warn("Could not get the Service Status {} from cluster.", loadBalancerResource.getResourceId().getName());
      return null;
    }

    LoadBalancerIngress loadBalancerIngress = service.getStatus().getLoadBalancer().getIngress().get(0);
    String loadBalancerHost =
        isNotBlank(loadBalancerIngress.getHostname()) ? loadBalancerIngress.getHostname() : loadBalancerIngress.getIp();

    boolean port80Found = false;
    boolean port443Found = false;
    Integer firstPort = null;

    for (ServicePort servicePort : service.getSpec().getPorts()) {
      firstPort = servicePort.getPort();

      if (servicePort.getPort() == 80) {
        port80Found = true;
      }
      if (servicePort.getPort() == 443) {
        port443Found = true;
      }
    }

    if (port443Found) {
      return "https://" + loadBalancerHost + "/";
    } else if (port80Found) {
      return "http://" + loadBalancerHost + "/";
    } else if (firstPort != null) {
      return loadBalancerHost + ":" + firstPort;
    } else {
      return loadBalancerHost;
    }
  }

  public void setNamespaceToKubernetesResourcesIfRequired(
      List<KubernetesResource> kubernetesResources, String namespace) {
    if (isEmpty(kubernetesResources)) {
      return;
    }

    for (KubernetesResource kubernetesResource : kubernetesResources) {
      if (isBlank(kubernetesResource.getResourceId().getNamespace())) {
        kubernetesResource.getResourceId().setNamespace(namespace);
      }
    }
  }

  public List<K8sPod> getPodDetails(
      KubernetesConfig kubernetesConfig, String namespace, String releaseName, long timeoutInMillis) throws Exception {
    Map<String, String> labels = ImmutableMap.of(HarnessLabels.releaseName, releaseName);
    return getPodDetailsWithLabels(kubernetesConfig, namespace, releaseName, labels, timeoutInMillis);
  }

  /**
   * This method arranges resources to be deleted in the reverse order of their creation.
   * To see order of create, please refer to KubernetesResourceComparer.kindOrder
   * @param resourceIdsToDelete
   */
  public List<KubernetesResourceId> arrangeResourceIdsInDeletionOrder(List<KubernetesResourceId> resourceIdsToDelete) {
    List<KubernetesResource> kubernetesResources =
        resourceIdsToDelete.stream()
            .map(resourceId -> KubernetesResource.builder().resourceId(resourceId).build())
            .collect(Collectors.toList());
    kubernetesResources =
        kubernetesResources.stream().sorted(new KubernetesResourceComparer().reversed()).collect(Collectors.toList());
    return kubernetesResources.stream()
        .map(kubernetesResource -> kubernetesResource.getResourceId())
        .collect(Collectors.toList());
  }

  public Integer getTargetInstancesForCanary(
      Integer percentInstancesInDelegateRequest, Integer maxInstances, LogCallback logCallback) {
    Integer targetInstances = (int) Math.round(percentInstancesInDelegateRequest * maxInstances / 100.0);
    if (targetInstances < 1) {
      logCallback.saveExecutionLog("\nTarget instances computed to be less than 1. Bumped up to 1");
      targetInstances = 1;
    }
    return targetInstances;
  }

  public List<Subset> generateSubsetsForDestinationRule(List<String> subsetNames) {
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

  private String generateDestination(String host, String subset) {
    return ISTIO_DESTINATION_TEMPLATE.replace("$ISTIO_DESTINATION_HOST_NAME", host)
        .replace("$ISTIO_DESTINATION_SUBSET_NAME", subset);
  }

  private String getDestinationYaml(String destination, String host) {
    if (canaryDestinationExpression.equals(destination)) {
      return generateDestination(host, HarnessLabelValues.trackCanary);
    } else if (stableDestinationExpression.equals(destination)) {
      return generateDestination(host, HarnessLabelValues.trackStable);
    } else {
      return destination;
    }
  }

  private List<DestinationWeight> generateDestinationWeights(
      List<IstioDestinationWeight> istioDestinationWeights, String host, PortSelector portSelector) throws IOException {
    List<DestinationWeight> destinationWeights = new ArrayList<>();

    for (IstioDestinationWeight istioDestinationWeight : istioDestinationWeights) {
      String destinationYaml = getDestinationYaml(istioDestinationWeight.getDestination(), host);
      Destination destination = new YamlUtils().read(destinationYaml, Destination.class);
      destination.setPort(portSelector);

      DestinationWeight destinationWeight = new DestinationWeight();
      destinationWeight.setWeight(Integer.parseInt(istioDestinationWeight.getWeight()));
      destinationWeight.setDestination(destination);

      destinationWeights.add(destinationWeight);
    }

    return destinationWeights;
  }

  private String getHostFromRoute(List<DestinationWeight> routes) {
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

  private PortSelector getPortSelectorFromRoute(List<DestinationWeight> routes) {
    return routes.get(0).getDestination().getPort();
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

  public void updateVirtualServiceWithDestinationWeights(List<IstioDestinationWeight> istioDestinationWeights,
      VirtualService virtualService, LogCallback executionLogCallback) throws IOException {
    validateRoutesInVirtualService(virtualService);

    executionLogCallback.saveExecutionLog("\nUpdating VirtualService with destination weights");

    List<HTTPRoute> http = virtualService.getSpec().getHttp();
    if (isNotEmpty(http)) {
      String host = getHostFromRoute(http.get(0).getRoute());
      PortSelector portSelector = getPortSelectorFromRoute(http.get(0).getRoute());
      http.get(0).setRoute(generateDestinationWeights(istioDestinationWeights, host, portSelector));
    }
  }

  private VirtualService updateVirtualServiceManifestFilesWithRoutes(List<KubernetesResource> resources,
      KubernetesConfig kubernetesConfig, List<IstioDestinationWeight> istioDestinationWeights,
      LogCallback executionLogCallback) throws IOException {
    List<KubernetesResource> virtualServiceResources =
        resources.stream()
            .filter(
                kubernetesResource -> kubernetesResource.getResourceId().getKind().equals(Kind.VirtualService.name()))
            .filter(KubernetesResource::isManaged)
            .collect(toList());

    if (isEmpty(virtualServiceResources)) {
      return null;
    }

    if (virtualServiceResources.size() > 1) {
      String msg = "\nMore than one VirtualService found. Only one VirtualService can be marked with annotation "
          + HarnessAnnotations.managed + ": true";
      executionLogCallback.saveExecutionLog(msg + "\n", ERROR, FAILURE);
      throw new InvalidRequestException(msg, USER);
    }

    KubernetesClient kubernetesClient = kubernetesHelperService.getKubernetesClient(kubernetesConfig);
    kubernetesClient.customResources(
        kubernetesContainerService.getCustomResourceDefinition(kubernetesClient, new VirtualServiceBuilder().build()),
        VirtualService.class, KubernetesResourceList.class, DoneableVirtualService.class);

    KubernetesResource kubernetesResource = virtualServiceResources.get(0);
    InputStream inputStream = IOUtils.toInputStream(kubernetesResource.getSpec(), UTF_8);
    VirtualService virtualService = (VirtualService) kubernetesClient.load(inputStream).get().get(0);
    updateVirtualServiceWithDestinationWeights(istioDestinationWeights, virtualService, executionLogCallback);

    kubernetesResource.setSpec(KubernetesHelper.toYaml(virtualService));

    return virtualService;
  }

  public VirtualService updateVirtualServiceManifestFilesWithRoutesForCanary(List<KubernetesResource> resources,
      KubernetesConfig kubernetesConfig, LogCallback executionLogCallback) throws IOException {
    List<IstioDestinationWeight> istioDestinationWeights = new ArrayList<>();
    istioDestinationWeights.add(
        IstioDestinationWeight.builder().destination(stableDestinationExpression).weight("100").build());
    istioDestinationWeights.add(
        IstioDestinationWeight.builder().destination(canaryDestinationExpression).weight("0").build());

    return updateVirtualServiceManifestFilesWithRoutes(
        resources, kubernetesConfig, istioDestinationWeights, executionLogCallback);
  }

  public DestinationRule updateDestinationRuleManifestFilesWithSubsets(List<KubernetesResource> resources,
      List<String> subsets, KubernetesConfig kubernetesConfig, LogCallback executionLogCallback) throws IOException {
    List<KubernetesResource> destinationRuleResources =
        resources.stream()
            .filter(
                kubernetesResource -> kubernetesResource.getResourceId().getKind().equals(Kind.DestinationRule.name()))
            .filter(KubernetesResource::isManaged)
            .collect(toList());

    if (isEmpty(destinationRuleResources)) {
      return null;
    }

    if (destinationRuleResources.size() > 1) {
      String msg = "More than one DestinationRule found. Only one DestinationRule can be marked with annotation "
          + HarnessAnnotations.managed + ": true";
      executionLogCallback.saveExecutionLog(msg + "\n", ERROR, FAILURE);
      throw new InvalidRequestException(msg, USER);
    }

    KubernetesClient kubernetesClient = kubernetesHelperService.getKubernetesClient(kubernetesConfig);
    kubernetesClient.customResources(
        kubernetesContainerService.getCustomResourceDefinition(kubernetesClient, new DestinationRuleBuilder().build()),
        DestinationRule.class, KubernetesResourceList.class, DoneableDestinationRule.class);

    KubernetesResource kubernetesResource = destinationRuleResources.get(0);
    InputStream inputStream = IOUtils.toInputStream(kubernetesResource.getSpec(), UTF_8);
    DestinationRule destinationRule = (DestinationRule) kubernetesClient.load(inputStream).get().get(0);
    destinationRule.getSpec().setSubsets(generateSubsetsForDestinationRule(subsets));

    kubernetesResource.setSpec(KubernetesHelper.toYaml(destinationRule));

    return destinationRule;
  }

  private String getPodContainerId(K8sPod pod) {
    return isEmpty(pod.getContainerList()) ? EMPTY : pod.getContainerList().get(0).getContainerId();
  }

  private List<K8sPod> getHelmPodDetails(
      KubernetesConfig kubernetesConfig, String namespace, String releaseName, long timeoutInMillis) throws Exception {
    Map<String, String> labels = ImmutableMap.of(HELM_RELEASE_LABEL, releaseName);
    return getPodDetailsWithLabels(kubernetesConfig, namespace, releaseName, labels, timeoutInMillis);
  }

  public List<ContainerInfo> getContainerInfos(
      KubernetesConfig kubernetesConfig, String releaseName, String namespace, long timeoutInMillis) throws Exception {
    List<K8sPod> helmPods = getHelmPodDetails(kubernetesConfig, namespace, releaseName, timeoutInMillis);

    return helmPods.stream()
        .map(pod
            -> ContainerInfo.builder()
                   .hostName(pod.getName())
                   .ip(pod.getPodIP())
                   .containerId(getPodContainerId(pod))
                   .podName(pod.getName())
                   .newContainer(true)
                   .status(ContainerInfo.Status.SUCCESS)
                   .releaseName(releaseName)
                   .build())
        .collect(Collectors.toList());
  }
}
