package software.wings.beans.command;

import static software.wings.cloudprovider.ContainerInfo.Status.SUCCESS;
import static software.wings.utils.KubernetesConvention.DOT;
import static software.wings.utils.KubernetesConvention.getRevisionFromControllerName;

import com.google.inject.Inject;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.fabric8.kubernetes.api.model.HorizontalPodAutoscaler;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import me.snowdrop.istio.api.model.IstioResource;
import me.snowdrop.istio.api.model.IstioResourceBuilder;
import me.snowdrop.istio.api.model.IstioResourceFluent.RouteRuleSpecNested;
import org.mongodb.morphia.annotations.Transient;
import software.wings.api.ContainerServiceData;
import software.wings.api.DeploymentType;
import software.wings.beans.AzureConfig;
import software.wings.beans.ErrorCode;
import software.wings.beans.GcpConfig;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.Log.LogLevel;
import software.wings.cloudprovider.ContainerInfo;
import software.wings.cloudprovider.gke.GkeClusterService;
import software.wings.cloudprovider.gke.KubernetesContainerService;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.azure.AzureHelperService;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.ArrayList;
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
  protected List<ContainerInfo> executeInternal(ContextData contextData, List<ContainerServiceData> desiredCounts,
      ContainerServiceData containerServiceData, ExecutionLogCallback executionLogCallback) {
    KubernetesResizeParams resizeParams = (KubernetesResizeParams) contextData.resizeParams;
    List<EncryptedDataDetail> encryptedDataDetails = new ArrayList<>();
    KubernetesConfig kubernetesConfig = getKubernetesConfig(contextData, encryptedDataDetails);

    if (resizeParams.isRollbackAutoscaler() && resizeParams.isUseAutoscaler()) {
      HorizontalPodAutoscaler autoscaler = kubernetesContainerService.getAutoscaler(
          kubernetesConfig, encryptedDataDetails, containerServiceData.getName(), resizeParams.getApiVersion());
      if (containerServiceData.getName().equals(autoscaler.getSpec().getScaleTargetRef().getName())) {
        executionLogCallback.saveExecutionLog("Disabling autoscaler " + containerServiceData.getName(), LogLevel.INFO);
        /*
         * Ideally we should be sending resizeParams.getApiVersion(), so we use "v2beta1" when we are dealing with
         * customMetricHPA, but there is a bug in fabric8 library in HasMetadataOperation.replace() method. For
         * customMetricHPA, metric config info resides in HPA.Spec.additionalProperties map. but during execution of
         * replace(), due to build() method in HorizontalPodAutoscalerSpecBuilder, this map goes away, and replace()
         * call actually removes all metricConfig from autoScalar. So currently use v1 version only, till this issue
         * gets fixed. (customMetricConfig is preserved as annotations in version_v1 HPA object, and that path is
         * working fine)
         * */
        kubernetesContainerService.disableAutoscaler(kubernetesConfig, encryptedDataDetails,
            containerServiceData.getName(), ContainerApiVersions.KUBERNETES_V1.getVersionName());
      }
    }

    List<ContainerInfo> containerInfos =
        kubernetesContainerService.setControllerPodCount(kubernetesConfig, encryptedDataDetails,
            resizeParams.getClusterName(), containerServiceData.getName(), containerServiceData.getPreviousCount(),
            containerServiceData.getDesiredCount(), resizeParams.getServiceSteadyStateTimeout(), executionLogCallback);

    boolean allContainersSuccess = containerInfos.stream().allMatch(info -> info.getStatus() == SUCCESS);
    int totalDesiredCount = desiredCounts.stream().mapToInt(ContainerServiceData::getDesiredCount).sum();
    if (totalDesiredCount > 0 && containerInfos.size() == totalDesiredCount && allContainersSuccess
        && contextData.deployingToHundredPercent) {
      if (resizeParams.isUseAutoscaler()) {
        executionLogCallback.saveExecutionLog("Enabling autoscaler " + containerServiceData.getName(), LogLevel.INFO);
        /*
         * Ideally we should be sending resizeParams.getApiVersion(), so we use "v2beta1" when we are dealing with
         * customMetricHPA, but there is a bug in fabric8 library in HasMetadataOperation.replace() method. For
         * customMetricHPA, metric config info resides in HPA.Spec.additionalProperties map. but during execution of
         * replace(), due to build() method in HorizontalPodAutoscalerSpecBuilder, this map goes away, and replace()
         * call actually removes all metricConfig from autoScalar. So currently use v1 version only, till this issue
         * gets fixed. (customMetricConfig is preserved as annotations in version_v1 HPA object, and that path is
         * working fine)
         * */
        kubernetesContainerService.enableAutoscaler(kubernetesConfig, encryptedDataDetails,
            containerServiceData.getName(), ContainerApiVersions.KUBERNETES_V1.getVersionName());
      }
    }

    // Edit weights for Istio route rule if applicable
    if (resizeParams.isUseIstioRouteRule()) {
      String controllerName = desiredCounts.get(0).getName();
      Map<String, Integer> activeControllers =
          kubernetesContainerService.getActiveServiceCounts(kubernetesConfig, encryptedDataDetails, controllerName);
      kubernetesContainerService.createOrReplaceRouteRule(kubernetesConfig, encryptedDataDetails,
          createRouteRuleDefinition(resizeParams.getNamespace(),
              controllerName.substring(0, controllerName.lastIndexOf(DOT)).replaceAll("\\.", "-"), activeControllers,
              executionLogCallback));
    }

    return containerInfos;
  }

  private KubernetesConfig getKubernetesConfig(
      ContextData contextData, List<EncryptedDataDetail> encryptedDataDetails) {
    KubernetesResizeParams resizeParams = (KubernetesResizeParams) contextData.resizeParams;
    KubernetesConfig kubernetesConfig;
    if (contextData.settingAttribute.getValue() instanceof KubernetesConfig) {
      kubernetesConfig = (KubernetesConfig) contextData.settingAttribute.getValue();
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
    return kubernetesContainerService.getActiveServiceCounts(
        kubernetesConfig, encryptedDataDetails, contextData.resizeParams.getContainerServiceName());
  }

  @Override
  protected Optional<Integer> getServiceDesiredCount(ContextData contextData) {
    List<EncryptedDataDetail> encryptedDataDetails = new ArrayList<>();
    KubernetesConfig kubernetesConfig = getKubernetesConfig(contextData, encryptedDataDetails);
    return kubernetesContainerService.getControllerPodCount(
        kubernetesConfig, encryptedDataDetails, contextData.resizeParams.getContainerServiceName());
  }

  private IstioResource createRouteRuleDefinition(String namespace, String kubernetesServiceName,
      Map<String, Integer> activeControllers, ExecutionLogCallback executionLogCallback) {
    RouteRuleSpecNested<IstioResourceBuilder> routeRuleSpecNested = new IstioResourceBuilder()
                                                                        .withNewMetadata()
                                                                        .withName(kubernetesServiceName)
                                                                        .endMetadata()
                                                                        .withNewRouteRuleSpec()
                                                                        .withNewDestination()
                                                                        .withName(kubernetesServiceName)
                                                                        .withNamespace(namespace)
                                                                        .endDestination();
    int totalInstances = activeControllers.values().stream().mapToInt(Integer::intValue).sum();
    executionLogCallback.saveExecutionLog("Setting Istio RouteRule weights:");
    for (String controller : activeControllers.keySet()) {
      int revision = getRevisionFromControllerName(controller).orElse(-1);
      int weight = (int) Math.round((activeControllers.get(controller) * 100.0) / totalInstances);
      executionLogCallback.saveExecutionLog("   " + controller + ": " + weight + "%");
      routeRuleSpecNested.addNewRoute()
          .addToLabels("revision", Integer.toString(revision))
          .withWeight(weight)
          .endRoute();
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
