package software.wings.beans.command;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.substringBefore;
import static org.atteo.evo.inflector.English.plural;
import static software.wings.beans.ErrorCode.GENERAL_ERROR;
import static software.wings.cloudprovider.ContainerInfo.Status.SUCCESS;
import static software.wings.common.Constants.HARNESS_REVISION;
import static software.wings.service.impl.KubernetesHelperService.printRouteRuleWeights;
import static software.wings.utils.KubernetesConvention.DOT;
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
import me.snowdrop.istio.api.model.IstioResourceFluent.RouteRuleSpecNested;
import me.snowdrop.istio.api.model.v1.routing.DestinationWeight;
import me.snowdrop.istio.api.model.v1.routing.RouteRule;
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

    if (desiredCount > 0 && desiredCount > previousCount && contextData.deployingToHundredPercent
        && resizeParams.isUseAutoscaler()) {
      HorizontalPodAutoscaler hpa = kubernetesContainerService.createOrReplaceAutoscaler(
          kubernetesConfig, encryptedDataDetails, resizeParams.getAutoscalerYaml());
      if (hpa != null) {
        String hpaName = hpa.getMetadata().getName();
        executionLogCallback.saveExecutionLog("\nHorizontal pod autoscaler enabled: " + hpaName);
      }
    }

    return containerInfos;
  }

  protected void postExecution(
      ContextData contextData, List<ContainerServiceData> allData, ExecutionLogCallback executionLogCallback) {
    KubernetesResizeParams resizeParams = (KubernetesResizeParams) contextData.resizeParams;
    // Edit weights for Istio route rule if applicable
    if (resizeParams.isUseIstioRouteRule()) {
      List<EncryptedDataDetail> encryptedDataDetails = new ArrayList<>();
      KubernetesConfig kubernetesConfig = getKubernetesConfig(contextData, encryptedDataDetails);
      String controllerName = resizeParams.getContainerServiceName();
      String kubernetesServiceName = getServiceNameFromControllerName(
          controllerName, !controllerName.contains(DOT), resizeParams.isUseDashInHostName());
      String controllerPrefix = getPrefixFromControllerName(
          controllerName, !controllerName.contains(DOT), resizeParams.isUseDashInHostName());
      IstioResource routeRuleDefinition = createRouteRuleDefinition(contextData, allData, kubernetesServiceName);
      IstioResource existingRouteRule =
          kubernetesContainerService.getRouteRule(kubernetesConfig, encryptedDataDetails, kubernetesServiceName);
      if (!routeRuleMatchesExisting(existingRouteRule, routeRuleDefinition)) {
        executionLogCallback.saveExecutionLog("Setting Istio route rule weights:");
        printRouteRuleWeights(routeRuleDefinition, controllerPrefix, executionLogCallback);
        kubernetesContainerService.createOrReplaceRouteRule(
            kubernetesConfig, encryptedDataDetails, routeRuleDefinition);
      } else {
        executionLogCallback.saveExecutionLog("No change to Istio route rule:");
        printRouteRuleWeights(existingRouteRule, controllerPrefix, executionLogCallback);
      }
      executionLogCallback.saveExecutionLog(DASH_STRING + "\n");
    }
  }

  private boolean routeRuleMatchesExisting(IstioResource existingRouteRule, IstioResource routeRule) {
    if (existingRouteRule == null) {
      return false;
    }

    RouteRule routeRuleSpec = (RouteRule) routeRule.getSpec();
    RouteRule existingRouteRuleSpec = (RouteRule) existingRouteRule.getSpec();

    if (routeRuleSpec.getRoute().size() != existingRouteRuleSpec.getRoute().size()) {
      return false;
    }

    List<DestinationWeight> sorted = new ArrayList<>(routeRuleSpec.getRoute());
    List<DestinationWeight> existingSorted = new ArrayList<>(existingRouteRuleSpec.getRoute());
    Comparator<DestinationWeight> comparator =
        Comparator.comparing(a -> Integer.valueOf(a.getLabels().get(HARNESS_REVISION)));
    sorted.sort(comparator);
    existingSorted.sort(comparator);

    for (int i = 0; i < sorted.size(); i++) {
      DestinationWeight dw1 = sorted.get(i);
      DestinationWeight dw2 = existingSorted.get(i);
      if (!dw1.getLabels().get(HARNESS_REVISION).equals(dw2.getLabels().get(HARNESS_REVISION))
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
    return kubernetesContainerService.getActiveServiceCounts(kubernetesConfig, encryptedDataDetails, controllerName,
        !controllerName.contains(DOT), resizeParams.isUseDashInHostName());
  }

  @Override
  protected Map<String, String> getActiveServiceImages(ContextData contextData) {
    List<EncryptedDataDetail> encryptedDataDetails = new ArrayList<>();
    KubernetesConfig kubernetesConfig = getKubernetesConfig(contextData, encryptedDataDetails);
    KubernetesResizeParams resizeParams = (KubernetesResizeParams) contextData.resizeParams;
    String controllerName = resizeParams.getContainerServiceName();
    String imagePrefix = substringBefore(contextData.resizeParams.getImage(), ":");
    return kubernetesContainerService.getActiveServiceImages(kubernetesConfig, encryptedDataDetails, controllerName,
        !controllerName.contains(DOT), imagePrefix, resizeParams.isUseDashInHostName());
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
    return kubernetesContainerService.getTrafficWeights(kubernetesConfig, encryptedDataDetails, controllerName,
        !controllerName.contains(DOT), resizeParams.isUseDashInHostName());
  }

  @Override
  protected int getPreviousTrafficPercent(ContextData contextData) {
    List<EncryptedDataDetail> encryptedDataDetails = new ArrayList<>();
    KubernetesConfig kubernetesConfig = getKubernetesConfig(contextData, encryptedDataDetails);
    KubernetesResizeParams resizeParams = (KubernetesResizeParams) contextData.resizeParams;
    String controllerName = resizeParams.getContainerServiceName();
    return kubernetesContainerService.getTrafficPercent(kubernetesConfig, encryptedDataDetails, controllerName,
        !controllerName.contains(DOT), resizeParams.isUseDashInHostName());
  }

  @Override
  protected Integer getDesiredTrafficPercent(ContextData contextData) {
    return ((KubernetesResizeParams) contextData.resizeParams).getTrafficPercent();
  }

  private IstioResource createRouteRuleDefinition(
      ContextData contextData, List<ContainerServiceData> allData, String kubernetesServiceName) {
    KubernetesResizeParams resizeParams = (KubernetesResizeParams) contextData.resizeParams;

    RouteRuleSpecNested<IstioResourceBuilder> routeRuleSpecNested = new IstioResourceBuilder()
                                                                        .withNewMetadata()
                                                                        .withName(kubernetesServiceName)
                                                                        .endMetadata()
                                                                        .withNewRouteRuleSpec()
                                                                        .withNewDestination()
                                                                        .withName(kubernetesServiceName)
                                                                        .withNamespace(resizeParams.getNamespace())
                                                                        .endDestination();

    for (ContainerServiceData containerServiceData : allData) {
      String controllerName = containerServiceData.getName();
      int revision = getRevisionFromControllerName(
          controllerName, !controllerName.contains(DOT), resizeParams.isUseDashInHostName())
                         .orElse(-1);
      int weight = containerServiceData.getDesiredTraffic();
      if (weight > 0) {
        routeRuleSpecNested.addNewRoute()
            .addToLabels(HARNESS_REVISION, Integer.toString(revision))
            .withWeight(weight)
            .endRoute();
      }
    }

    return routeRuleSpecNested.endRouteRuleSpec().build();
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
