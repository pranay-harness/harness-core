package software.wings.beans.command;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.substringBefore;
import static org.atteo.evo.inflector.English.plural;
import static software.wings.beans.ErrorCode.GENERAL_ERROR;
import static software.wings.cloudprovider.ContainerInfo.Status.SUCCESS;
import static software.wings.service.impl.KubernetesHelperService.printVirtualServiceRouteWeights;
import static software.wings.utils.KubernetesConvention.getPrefixFromControllerName;
import static software.wings.utils.KubernetesConvention.getRevisionFromControllerName;
import static software.wings.utils.KubernetesConvention.getServiceNameFromControllerName;

import com.google.inject.Inject;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.HorizontalPodAutoscaler;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import me.snowdrop.istio.api.model.IstioResource;
import me.snowdrop.istio.api.model.IstioResourceBuilder;
import me.snowdrop.istio.api.model.IstioResourceFluent.VirtualServiceSpecNested;
import me.snowdrop.istio.api.model.v1.networking.Destination;
import me.snowdrop.istio.api.model.v1.networking.DestinationWeight;
import me.snowdrop.istio.api.model.v1.networking.HTTPRoute;
import me.snowdrop.istio.api.model.v1.networking.VirtualService;
import me.snowdrop.istio.api.model.v1.networking.VirtualServiceFluent.HttpNested;
import org.mongodb.morphia.annotations.Transient;
import software.wings.api.ContainerServiceData;
import software.wings.api.DeploymentType;
import software.wings.beans.AzureConfig;
import software.wings.beans.ErrorCode;
import software.wings.beans.GcpConfig;
import software.wings.beans.KubernetesClusterConfig;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.Log.LogLevel;
import software.wings.cloudprovider.ContainerInfo;
import software.wings.cloudprovider.gke.GkeClusterService;
import software.wings.cloudprovider.gke.KubernetesContainerService;
import software.wings.exception.InvalidRequestException;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.azure.AzureHelperService;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.utils.Misc;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

/**
 * Created by brett on 3/3/17
 */
public class KubernetesResizeCommandUnit extends ContainerResizeCommandUnit {
  @Inject @Transient private transient GkeClusterService gkeClusterService;
  @Inject @Transient private transient KubernetesContainerService kubernetesContainerService;
  @Inject @Transient private transient AzureHelperService azureHelperService;

  public KubernetesResizeCommandUnit() {
    super(CommandUnitType.RESIZE_KUBERNETES);
    setDeploymentType(DeploymentType.KUBERNETES.name());
  }

  @Override
  protected List<ContainerInfo> executeResize(
      ContextData contextData, ContainerServiceData containerServiceData, ExecutionLogCallback executionLogCallback) {
    KubernetesResizeParams resizeParams = (KubernetesResizeParams) contextData.resizeParams;
    List<EncryptedDataDetail> encryptedDataDetails = new ArrayList<>();
    KubernetesConfig kubernetesConfig = getKubernetesConfig(contextData, encryptedDataDetails);
    String controllerName = containerServiceData.getName();
    HasMetadata controller =
        kubernetesContainerService.getController(kubernetesConfig, encryptedDataDetails, controllerName);
    if (controller == null) {
      throw new WingsException(GENERAL_ERROR).addParam("message", "No controller with name: " + controllerName);
    }
    if ("StatefulSet".equals(controller.getKind()) || "DaemonSet".equals(controller.getKind())) {
      executionLogCallback.saveExecutionLog(
          "\nResize Containers does not apply to Stateful Sets or Daemon Sets.\n", LogLevel.WARN);
      return emptyList();
    }

    if (resizeParams.isUseAutoscaler() && resizeParams.isRollback()) {
      HorizontalPodAutoscaler autoscaler = kubernetesContainerService.getAutoscaler(
          kubernetesConfig, encryptedDataDetails, controllerName, resizeParams.getApiVersion());
      if (autoscaler != null && controllerName.equals(autoscaler.getSpec().getScaleTargetRef().getName())) {
        executionLogCallback.saveExecutionLog("Deleting horizontal pod autoscaler: " + controllerName);
        kubernetesContainerService.deleteAutoscaler(kubernetesConfig, encryptedDataDetails, controllerName);
      }
    }

    int desiredCount = containerServiceData.getDesiredCount();
    int previousCount = containerServiceData.getPreviousCount();
    List<ContainerInfo> containerInfos = kubernetesContainerService.setControllerPodCount(kubernetesConfig,
        encryptedDataDetails, resizeParams.getClusterName(), controllerName, previousCount, desiredCount,
        resizeParams.getServiceSteadyStateTimeout(), executionLogCallback);

    boolean allContainersSuccess = containerInfos.stream().allMatch(info -> info.getStatus() == SUCCESS);

    if (containerInfos.size() != desiredCount || !allContainersSuccess) {
      try {
        if (containerInfos.size() != desiredCount) {
          executionLogCallback.saveExecutionLog(format("Expected data for %d %s but got %d", desiredCount,
                                                    plural("container", desiredCount), containerInfos.size()),
              LogLevel.ERROR);
        }
        List<ContainerInfo> failedContainers =
            containerInfos.stream().filter(info -> info.getStatus() != ContainerInfo.Status.SUCCESS).collect(toList());
        executionLogCallback.saveExecutionLog(
            format("The following %s did not have success status: %s", plural("container", failedContainers.size()),
                failedContainers.stream().map(ContainerInfo::getContainerId).collect(toList())),
            LogLevel.ERROR);
      } catch (Exception e) {
        Misc.logAllMessages(e, executionLogCallback);
      }
      throw new WingsException(GENERAL_ERROR).addParam("message", "Failed to resize controller");
    }

    return containerInfos;
  }

  protected void postExecution(
      ContextData contextData, List<ContainerServiceData> allData, ExecutionLogCallback executionLogCallback) {
    KubernetesResizeParams resizeParams = (KubernetesResizeParams) contextData.resizeParams;
    List<EncryptedDataDetail> encryptedDataDetails = new ArrayList<>();
    KubernetesConfig kubernetesConfig = getKubernetesConfig(contextData, encryptedDataDetails);
    boolean executedSomething = false;

    // Enable HPA
    if (!resizeParams.isRollback() && contextData.deployingToHundredPercent && resizeParams.isUseAutoscaler()) {
      HorizontalPodAutoscaler hpa = kubernetesContainerService.createOrReplaceAutoscaler(
          kubernetesConfig, encryptedDataDetails, resizeParams.getAutoscalerYaml());
      if (hpa != null) {
        String hpaName = hpa.getMetadata().getName();
        executionLogCallback.saveExecutionLog("Horizontal pod autoscaler enabled: " + hpaName + "\n");
        executedSomething = true;
      }
    }

    // Edit weights for Istio route rule if applicable
    if (resizeParams.isUseIstioRouteRule()) {
      String controllerName = resizeParams.getContainerServiceName();
      String kubernetesServiceName =
          getServiceNameFromControllerName(controllerName, resizeParams.isUseDashInHostName());
      String controllerPrefix = getPrefixFromControllerName(controllerName, resizeParams.isUseDashInHostName());
      IstioResource existingVirtualService = kubernetesContainerService.getIstioResource(
          kubernetesConfig, encryptedDataDetails, "VirtualService", kubernetesServiceName);

      IstioResource virtualServiceDefinition =
          createVirtualServiceDefinition(contextData, allData, existingVirtualService, kubernetesServiceName);

      if (!virtualServiceHttpRouteMatchesExisting(existingVirtualService, virtualServiceDefinition)) {
        executionLogCallback.saveExecutionLog("Setting Istio VirtualService Route destination weights:");
        printVirtualServiceRouteWeights(
            virtualServiceDefinition, controllerPrefix, resizeParams.isUseDashInHostName(), executionLogCallback);
        kubernetesContainerService.createOrReplaceIstioResource(
            kubernetesConfig, encryptedDataDetails, virtualServiceDefinition);
      } else {
        executionLogCallback.saveExecutionLog("No change to Istio VirtualService Route rules :");
        printVirtualServiceRouteWeights(
            existingVirtualService, controllerPrefix, resizeParams.isUseDashInHostName(), executionLogCallback);
      }
      executionLogCallback.saveExecutionLog("");
      executedSomething = true;
    }
    if (executedSomething) {
      executionLogCallback.saveExecutionLog(DASH_STRING + "\n");
    }
  }

  private boolean virtualServiceHttpRouteMatchesExisting(
      IstioResource existingVirtualService, IstioResource virtualService) {
    if (existingVirtualService == null) {
      return false;
    }

    HTTPRoute virtualServiceHttpRoute = ((VirtualService) virtualService.getSpec()).getHttp().get(0);
    HTTPRoute existingVirtualServiceHttpRoute = ((VirtualService) existingVirtualService.getSpec()).getHttp().get(0);

    if ((virtualServiceHttpRoute == null || existingVirtualServiceHttpRoute == null)
        && virtualServiceHttpRoute != existingVirtualServiceHttpRoute) {
      return false;
    }

    List<DestinationWeight> sorted = new ArrayList<>(virtualServiceHttpRoute.getRoute());
    List<DestinationWeight> existingSorted = new ArrayList<>(existingVirtualServiceHttpRoute.getRoute());
    Comparator<DestinationWeight> comparator =
        Comparator.comparing(a -> Integer.valueOf(a.getDestination().getSubset()));
    sorted.sort(comparator);
    existingSorted.sort(comparator);

    for (int i = 0; i < sorted.size(); i++) {
      DestinationWeight dw1 = sorted.get(i);
      DestinationWeight dw2 = existingSorted.get(i);
      if (!dw1.getDestination().getSubset().equals(dw2.getDestination().getSubset())
          || !dw1.getWeight().equals(dw2.getWeight())) {
        return false;
      }
    }

    return true;
  }

  private KubernetesConfig getKubernetesConfig(
      ContextData contextData, List<EncryptedDataDetail> encryptedDataDetails) {
    KubernetesResizeParams resizeParams = (KubernetesResizeParams) contextData.resizeParams;
    KubernetesConfig kubernetesConfig;
    if (contextData.settingAttribute.getValue() instanceof KubernetesConfig) {
      kubernetesConfig = (KubernetesConfig) contextData.settingAttribute.getValue();
      encryptedDataDetails.addAll(contextData.encryptedDataDetails);
    } else if (contextData.settingAttribute.getValue() instanceof KubernetesClusterConfig) {
      KubernetesClusterConfig config = (KubernetesClusterConfig) contextData.settingAttribute.getValue();
      String delegateName = System.getenv().get("DELEGATE_NAME");
      if (config.isUseKubernetesDelegate() && !config.getDelegateName().equals(delegateName)) {
        throw new InvalidRequestException(String.format("Kubernetes delegate name [%s] doesn't match "
                + "cloud provider delegate name [%s] for kubernetes cluster cloud provider [%s]",
            delegateName, config.getDelegateName(), contextData.settingAttribute.getName()));
      }
      kubernetesConfig = ((KubernetesClusterConfig) contextData.settingAttribute.getValue())
                             .createKubernetesConfig(resizeParams.getNamespace());
      encryptedDataDetails.addAll(contextData.encryptedDataDetails);
    } else if (contextData.settingAttribute.getValue() instanceof AzureConfig) {
      AzureConfig azureConfig = (AzureConfig) contextData.settingAttribute.getValue();
      kubernetesConfig = azureHelperService.getKubernetesClusterConfig(azureConfig, contextData.encryptedDataDetails,
          resizeParams.getSubscriptionId(), resizeParams.getResourceGroup(), resizeParams.getClusterName(),
          resizeParams.getNamespace());
      kubernetesConfig.setDecrypted(true);
    } else if (contextData.settingAttribute.getValue() instanceof GcpConfig) {
      kubernetesConfig = gkeClusterService.getCluster(contextData.settingAttribute, contextData.encryptedDataDetails,
          resizeParams.getClusterName(), resizeParams.getNamespace());
      kubernetesConfig.setDecrypted(true);
    } else {
      throw new WingsException(ErrorCode.INVALID_ARGUMENT)
          .addParam("args",
              "Unknown kubernetes cloud provider setting value: " + contextData.settingAttribute.getValue().getType());
    }
    return kubernetesConfig;
  }

  @Override
  protected Map<String, Integer> getActiveServiceCounts(ContextData contextData) {
    List<EncryptedDataDetail> encryptedDataDetails = new ArrayList<>();
    KubernetesConfig kubernetesConfig = getKubernetesConfig(contextData, encryptedDataDetails);
    KubernetesResizeParams resizeParams = (KubernetesResizeParams) contextData.resizeParams;
    String controllerName = resizeParams.getContainerServiceName();
    return kubernetesContainerService.getActiveServiceCounts(
        kubernetesConfig, encryptedDataDetails, controllerName, resizeParams.isUseDashInHostName());
  }

  @Override
  protected Map<String, String> getActiveServiceImages(ContextData contextData) {
    List<EncryptedDataDetail> encryptedDataDetails = new ArrayList<>();
    KubernetesConfig kubernetesConfig = getKubernetesConfig(contextData, encryptedDataDetails);
    KubernetesResizeParams resizeParams = (KubernetesResizeParams) contextData.resizeParams;
    String controllerName = resizeParams.getContainerServiceName();
    String imagePrefix = substringBefore(contextData.resizeParams.getImage(), ":");
    return kubernetesContainerService.getActiveServiceImages(
        kubernetesConfig, encryptedDataDetails, controllerName, imagePrefix, resizeParams.isUseDashInHostName());
  }

  @Override
  protected Optional<Integer> getServiceDesiredCount(ContextData contextData) {
    List<EncryptedDataDetail> encryptedDataDetails = new ArrayList<>();
    KubernetesConfig kubernetesConfig = getKubernetesConfig(contextData, encryptedDataDetails);
    return kubernetesContainerService.getControllerPodCount(
        kubernetesConfig, encryptedDataDetails, contextData.resizeParams.getContainerServiceName());
  }

  @Override
  protected Map<String, Integer> getTrafficWeights(ContextData contextData) {
    List<EncryptedDataDetail> encryptedDataDetails = new ArrayList<>();
    KubernetesConfig kubernetesConfig = getKubernetesConfig(contextData, encryptedDataDetails);
    KubernetesResizeParams resizeParams = (KubernetesResizeParams) contextData.resizeParams;
    String controllerName = resizeParams.getContainerServiceName();
    return kubernetesContainerService.getTrafficWeights(
        kubernetesConfig, encryptedDataDetails, controllerName, resizeParams.isUseDashInHostName());
  }

  @Override
  protected int getPreviousTrafficPercent(ContextData contextData) {
    List<EncryptedDataDetail> encryptedDataDetails = new ArrayList<>();
    KubernetesConfig kubernetesConfig = getKubernetesConfig(contextData, encryptedDataDetails);
    KubernetesResizeParams resizeParams = (KubernetesResizeParams) contextData.resizeParams;
    String controllerName = resizeParams.getContainerServiceName();
    return kubernetesContainerService.getTrafficPercent(
        kubernetesConfig, encryptedDataDetails, controllerName, resizeParams.isUseDashInHostName());
  }

  @Override
  protected Integer getDesiredTrafficPercent(ContextData contextData) {
    return ((KubernetesResizeParams) contextData.resizeParams).getTrafficPercent();
  }

  private IstioResource createVirtualServiceDefinition(ContextData contextData, List<ContainerServiceData> allData,
      IstioResource existingVirtualService, String kubernetesServiceName) {
    KubernetesResizeParams resizeParams = (KubernetesResizeParams) contextData.resizeParams;

    VirtualService existingVirtualServiceSpec = (VirtualService) existingVirtualService.getSpec();

    VirtualServiceSpecNested<IstioResourceBuilder> virtualServiceSpecNested =
        new IstioResourceBuilder()
            .withApiVersion(existingVirtualService.getApiVersion())
            .withKind(existingVirtualService.getKind())
            .withNewMetadata()
            .withName(existingVirtualService.getMetadata().getName())
            .withNamespace(existingVirtualService.getMetadata().getNamespace())
            .withAnnotations(existingVirtualService.getMetadata().getAnnotations())
            .withLabels(existingVirtualService.getMetadata().getLabels())
            .endMetadata()
            .withNewVirtualServiceSpec()
            .withHosts(existingVirtualServiceSpec.getHosts())
            .withGateways(existingVirtualServiceSpec.getGateways());

    HttpNested virtualServiceHttpNested = virtualServiceSpecNested.addNewHttp();

    if (resizeParams.isRollback()) {
      Map<String, Integer> activeControllers = getActiveServiceCounts(contextData);
      int totalInstances = activeControllers.values().stream().mapToInt(Integer::intValue).sum();
      for (Entry<String, Integer> entry : activeControllers.entrySet()) {
        Optional<Integer> revision = getRevisionFromControllerName(entry.getKey(), resizeParams.isUseDashInHostName());
        if (revision.isPresent()) {
          int weight = (int) Math.round((entry.getValue() * 100.0) / totalInstances);
          if (weight > 0) {
            Destination destination = new Destination();
            destination.setHost(kubernetesServiceName);
            destination.setSubset(revision.get().toString());
            DestinationWeight destinationWeight = new DestinationWeight();
            destinationWeight.setWeight(weight);
            destinationWeight.setDestination(destination);
            virtualServiceHttpNested.addToRoute(destinationWeight);
          }
        }
      }
    } else {
      for (ContainerServiceData containerServiceData : allData) {
        String controllerName = containerServiceData.getName();
        Optional<Integer> revision = getRevisionFromControllerName(controllerName, resizeParams.isUseDashInHostName());
        int weight = containerServiceData.getDesiredTraffic();
        if (weight > 0) {
          Destination destination = new Destination();
          destination.setHost(kubernetesServiceName);
          destination.setSubset(Integer.toString(revision.get()));
          DestinationWeight destinationWeight = new DestinationWeight();
          destinationWeight.setWeight(weight);
          destinationWeight.setDestination(destination);
          virtualServiceHttpNested.addToRoute(destinationWeight);
        }
      }
    }
    virtualServiceHttpNested.endHttp();
    return virtualServiceSpecNested.endVirtualServiceSpec().build();
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @JsonTypeName("RESIZE_KUBERNETES")
  public static class Yaml extends ContainerResizeCommandUnit.Yaml {
    public Yaml() {
      super(CommandUnitType.RESIZE_KUBERNETES.name());
    }

    @Builder
    public Yaml(String name, String deploymentType) {
      super(name, CommandUnitType.RESIZE_KUBERNETES.name(), deploymentType);
    }
  }
}
