package software.wings.beans.command;

import static io.harness.container.ContainerInfo.Status.SUCCESS;
import static io.harness.eraro.ErrorCode.GENERAL_ERROR;
import static io.harness.k8s.KubernetesConvention.getPrefixFromControllerName;
import static io.harness.k8s.KubernetesConvention.getServiceNameFromControllerName;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.substringBefore;
import static org.atteo.evo.inflector.English.plural;

import io.harness.container.ContainerInfo;
import io.harness.eraro.ErrorCode;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.istio.api.networking.IstioApiNetworkingHandler;
import io.harness.istio.api.networking.IstioNetworkingApiFactory;
import io.harness.k8s.KubernetesContainerService;
import io.harness.k8s.model.KubernetesConfig;
import io.harness.logging.LogLevel;
import io.harness.logging.Misc;

import software.wings.api.ContainerServiceData;
import software.wings.api.DeploymentType;
import software.wings.beans.AzureConfig;
import software.wings.beans.GcpConfig;
import software.wings.beans.KubernetesClusterConfig;
import software.wings.cloudprovider.gke.GkeClusterService;
import software.wings.helpers.ext.azure.AzureHelperService;
import software.wings.service.intfc.security.EncryptionService;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.inject.Inject;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.HorizontalPodAutoscaler;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import me.snowdrop.istio.api.IstioResource;
import org.mongodb.morphia.annotations.Transient;

/**
 * Created by brett on 3/3/17
 */
@JsonTypeName("RESIZE_KUBERNETES")
public class KubernetesResizeCommandUnit extends ContainerResizeCommandUnit {
  @Inject @Transient private transient GkeClusterService gkeClusterService;
  @Inject @Transient private transient KubernetesContainerService kubernetesContainerService;
  @Inject @Transient private transient AzureHelperService azureHelperService;
  @Inject private EncryptionService encryptionService;
  @Inject private IstioNetworkingApiFactory istioNetworkingApiFactory;

  public KubernetesResizeCommandUnit() {
    super(CommandUnitType.RESIZE_KUBERNETES);
    setDeploymentType(DeploymentType.KUBERNETES.name());
  }

  @Override
  protected List<ContainerInfo> executeResize(
      ContextData contextData, ContainerServiceData containerServiceData, ExecutionLogCallback executionLogCallback) {
    KubernetesResizeParams resizeParams = (KubernetesResizeParams) contextData.resizeParams;
    KubernetesConfig kubernetesConfig = getKubernetesConfig(contextData);

    String controllerName = containerServiceData.getName();
    HasMetadata controller = kubernetesContainerService.getController(kubernetesConfig, controllerName);
    if (controller == null) {
      throw new WingsException(GENERAL_ERROR).addParam("message", "No controller with name: " + controllerName);
    }
    if ("StatefulSet".equals(controller.getKind()) || "DaemonSet".equals(controller.getKind())) {
      executionLogCallback.saveExecutionLog(
          "\nResize Containers does not apply to Stateful Sets or Daemon Sets.\n", LogLevel.WARN);
      return emptyList();
    }

    if (resizeParams.isUseAutoscaler() && resizeParams.isRollback()) {
      HorizontalPodAutoscaler autoscaler =
          kubernetesContainerService.getAutoscaler(kubernetesConfig, controllerName, resizeParams.getApiVersion());
      if (autoscaler != null && controllerName.equals(autoscaler.getSpec().getScaleTargetRef().getName())) {
        executionLogCallback.saveExecutionLog("Deleting horizontal pod autoscaler: " + controllerName);
        kubernetesContainerService.deleteAutoscaler(kubernetesConfig, controllerName);
      }
    }

    int desiredCount = containerServiceData.getDesiredCount();
    int previousCount = containerServiceData.getPreviousCount();
    List<ContainerInfo> containerInfos = kubernetesContainerService.setControllerPodCount(kubernetesConfig,
        resizeParams.getClusterName(), controllerName, previousCount, desiredCount,
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

  @Override
  protected void postExecution(
      ContextData contextData, List<ContainerServiceData> allData, ExecutionLogCallback executionLogCallback) {
    KubernetesResizeParams resizeParams = (KubernetesResizeParams) contextData.resizeParams;
    KubernetesConfig kubernetesConfig = getKubernetesConfig(contextData);

    boolean executedSomething = false;

    // Enable HPA
    if (!resizeParams.isRollback() && contextData.deployingToHundredPercent && resizeParams.isUseAutoscaler()) {
      HorizontalPodAutoscaler hpa =
          kubernetesContainerService.createOrReplaceAutoscaler(kubernetesConfig, resizeParams.getAutoscalerYaml());
      if (hpa != null) {
        String hpaName = hpa.getMetadata().getName();
        executionLogCallback.saveExecutionLog("Horizontal pod autoscaler enabled: " + hpaName + "\n");
        executedSomething = true;
      }
    }

    // Edit weights for Istio route rule if applicable
    if (resizeParams.isUseIstioRouteRule()) {
      String controllerName = resizeParams.getContainerServiceName();
      String kubernetesServiceName = getServiceNameFromControllerName(controllerName);
      String controllerPrefix = getPrefixFromControllerName(controllerName);

      HasMetadata istioVirtualService =
          kubernetesContainerService.getIstioVirtualService(kubernetesConfig, kubernetesServiceName);
      IstioResource existingVirtualService = (IstioResource) istioVirtualService;

      if (existingVirtualService == null) {
        throw new InvalidRequestException(format("Virtual Service [%s] not found", kubernetesServiceName));
      }

      IstioApiNetworkingHandler istioApiNetworkingHandler =
          istioNetworkingApiFactory.obtainHandler(existingVirtualService.getApiVersion());

      IstioResource virtualService = istioApiNetworkingHandler.createVirtualServiceDefinition(
          allData, existingVirtualService, kubernetesServiceName);

      if (!istioApiNetworkingHandler.virtualServiceHttpRouteMatchesExisting(existingVirtualService, virtualService)) {
        executionLogCallback.saveExecutionLog("Setting Istio VirtualService Route destination weights:");

        istioApiNetworkingHandler.printVirtualServiceRouteWeights(
            virtualService, controllerPrefix, executionLogCallback);
        kubernetesContainerService.createOrReplaceIstioResource(kubernetesConfig, virtualService);
      } else {
        executionLogCallback.saveExecutionLog("No change to Istio VirtualService Route rules :");
        istioApiNetworkingHandler.printVirtualServiceRouteWeights(
            virtualService, controllerPrefix, executionLogCallback);
      }
      executionLogCallback.saveExecutionLog("");
      executedSomething = true;
    }
    if (executedSomething) {
      executionLogCallback.saveExecutionLog(DASH_STRING + "\n");
    }
  }

  private KubernetesConfig getKubernetesConfig(ContextData contextData) {
    KubernetesResizeParams resizeParams = (KubernetesResizeParams) contextData.resizeParams;
    KubernetesConfig kubernetesConfig;
    if (contextData.settingAttribute.getValue() instanceof KubernetesClusterConfig) {
      KubernetesClusterConfig config = (KubernetesClusterConfig) contextData.settingAttribute.getValue();
      encryptionService.decrypt(config, contextData.encryptedDataDetails, false);

      kubernetesConfig = ((KubernetesClusterConfig) contextData.settingAttribute.getValue())
                             .createKubernetesConfig(resizeParams.getNamespace());
    } else if (contextData.settingAttribute.getValue() instanceof AzureConfig) {
      AzureConfig azureConfig = (AzureConfig) contextData.settingAttribute.getValue();
      kubernetesConfig = azureHelperService.getKubernetesClusterConfig(azureConfig, contextData.encryptedDataDetails,
          resizeParams.getSubscriptionId(), resizeParams.getResourceGroup(), resizeParams.getClusterName(),
          resizeParams.getNamespace(), false);
    } else if (contextData.settingAttribute.getValue() instanceof GcpConfig) {
      kubernetesConfig = gkeClusterService.getCluster(contextData.settingAttribute, contextData.encryptedDataDetails,
          resizeParams.getClusterName(), resizeParams.getNamespace(), false);
    } else {
      throw new WingsException(ErrorCode.INVALID_ARGUMENT)
          .addParam("args",
              "Unknown kubernetes cloud provider setting value: " + contextData.settingAttribute.getValue().getType());
    }

    return kubernetesConfig;
  }

  @Override
  protected Map<String, Integer> getActiveServiceCounts(ContextData contextData) {
    KubernetesConfig kubernetesConfig = getKubernetesConfig(contextData);

    KubernetesResizeParams resizeParams = (KubernetesResizeParams) contextData.resizeParams;
    return kubernetesContainerService.getActiveServiceCountsWithLabels(
        kubernetesConfig, resizeParams.getLookupLabels());
  }

  @Override
  protected Map<String, String> getActiveServiceImages(ContextData contextData) {
    KubernetesConfig kubernetesConfig = getKubernetesConfig(contextData);

    KubernetesResizeParams resizeParams = (KubernetesResizeParams) contextData.resizeParams;
    String controllerName = resizeParams.getContainerServiceName();
    String imagePrefix = substringBefore(contextData.resizeParams.getImage(), ":");
    return kubernetesContainerService.getActiveServiceImages(kubernetesConfig, controllerName, imagePrefix);
  }

  @Override
  protected Optional<Integer> getServiceDesiredCount(ContextData contextData) {
    KubernetesConfig kubernetesConfig = getKubernetesConfig(contextData);

    return kubernetesContainerService.getControllerPodCount(
        kubernetesConfig, contextData.resizeParams.getContainerServiceName());
  }

  @Override
  protected Map<String, Integer> getTrafficWeights(ContextData contextData) {
    KubernetesResizeParams resizeParams = (KubernetesResizeParams) contextData.resizeParams;
    if (!resizeParams.isUseIstioRouteRule()) {
      return new HashMap<>();
    }

    KubernetesConfig kubernetesConfig = getKubernetesConfig(contextData);

    String controllerName = resizeParams.getContainerServiceName();
    return kubernetesContainerService.getTrafficWeights(kubernetesConfig, controllerName);
  }

  @Override
  protected int getPreviousTrafficPercent(ContextData contextData) {
    KubernetesConfig kubernetesConfig = getKubernetesConfig(contextData);

    KubernetesResizeParams resizeParams = (KubernetesResizeParams) contextData.resizeParams;
    String controllerName = resizeParams.getContainerServiceName();
    return kubernetesContainerService.getTrafficPercent(kubernetesConfig, controllerName);
  }

  @Override
  protected Integer getDesiredTrafficPercent(ContextData contextData) {
    return ((KubernetesResizeParams) contextData.resizeParams).getTrafficPercent();
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
