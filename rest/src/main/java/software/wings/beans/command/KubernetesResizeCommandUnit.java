package software.wings.beans.command;

import static java.util.stream.Collectors.toList;
import static software.wings.beans.ErrorCode.INVALID_REQUEST;
import static software.wings.cloudprovider.ContainerInfo.Status.SUCCESS;
import static software.wings.common.Constants.HARNESS_REVISION;
import static software.wings.service.impl.KubernetesHelperService.printRouteRuleWeights;
import static software.wings.utils.KubernetesConvention.getPrefixFromControllerName;
import static software.wings.utils.KubernetesConvention.getRevisionFromControllerName;
import static software.wings.utils.KubernetesConvention.getServiceNameFromControllerName;

import com.google.inject.Inject;

import com.fasterxml.jackson.annotation.JsonTypeName;
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
import software.wings.exception.WingsException;
import software.wings.helpers.ext.azure.AzureHelperService;
import software.wings.security.encryption.EncryptedDataDetail;

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
  protected List<ContainerInfo> executeResize(ContextData contextData, int totalDesiredCount,
      ContainerServiceData containerServiceData, ExecutionLogCallback executionLogCallback) {
    KubernetesResizeParams resizeParams = (KubernetesResizeParams) contextData.resizeParams;
    List<EncryptedDataDetail> encryptedDataDetails = new ArrayList<>();
    KubernetesConfig kubernetesConfig = getKubernetesConfig(contextData, encryptedDataDetails);
    String controllerName = containerServiceData.getName();

    if (resizeParams.isUseAutoscaler() && resizeParams.isRollback()) {
      HorizontalPodAutoscaler autoscaler = kubernetesContainerService.getAutoscaler(
          kubernetesConfig, encryptedDataDetails, controllerName, resizeParams.getApiVersion());
      if (autoscaler != null && controllerName.equals(autoscaler.getSpec().getScaleTargetRef().getName())) {
        disableAutoscaler(kubernetesConfig, encryptedDataDetails, controllerName, executionLogCallback);
      }
    }

    List<ContainerInfo> containerInfos = kubernetesContainerService.setControllerPodCount(kubernetesConfig,
        encryptedDataDetails, resizeParams.getClusterName(), controllerName, containerServiceData.getPreviousCount(),
        containerServiceData.getDesiredCount(), resizeParams.getServiceSteadyStateTimeout(), executionLogCallback);

    boolean allContainersSuccess = containerInfos.stream().allMatch(info -> info.getStatus() == SUCCESS);
    if (containerInfos.size() == totalDesiredCount && allContainersSuccess) {
      if (totalDesiredCount > 0 && contextData.deployingToHundredPercent && resizeParams.isUseAutoscaler()) {
        enableAutoscaler(kubernetesConfig, encryptedDataDetails, controllerName, executionLogCallback);
      }
      return containerInfos;
    } else {
      try {
        if (containerInfos.size() != totalDesiredCount) {
          executionLogCallback.saveExecutionLog(
              String.format("\nExpected data for %d container%s but got %d", totalDesiredCount,
                  totalDesiredCount == 1 ? "" : "s", containerInfos.size()),
              LogLevel.WARN);
        }
        List<ContainerInfo> failedContainers =
            containerInfos.stream().filter(info -> info.getStatus() != ContainerInfo.Status.SUCCESS).collect(toList());
        executionLogCallback.saveExecutionLog(
            String.format("\nThe following container%s did not have success status: %s",
                failedContainers.size() == 1 ? "" : "s",
                failedContainers.stream().map(ContainerInfo::getContainerId).collect(toList())),
            LogLevel.WARN);
      } catch (Exception e) {
        // Ignore failure to log failing containers
      }

      throw new WingsException(INVALID_REQUEST).addParam("message", "Failed to resize controller");
    }
  }

  protected void postExecution(
      ContextData contextData, List<ContainerServiceData> allData, ExecutionLogCallback executionLogCallback) {
    KubernetesResizeParams resizeParams = (KubernetesResizeParams) contextData.resizeParams;
    // Edit weights for Istio route rule if applicable
    if (resizeParams.isUseIstioRouteRule()) {
      List<EncryptedDataDetail> encryptedDataDetails = new ArrayList<>();
      KubernetesConfig kubernetesConfig = getKubernetesConfig(contextData, encryptedDataDetails);
      String kubernetesServiceName =
          getServiceNameFromControllerName(contextData.resizeParams.getContainerServiceName());
      String controllerPrefix = getPrefixFromControllerName(resizeParams.getContainerServiceName());
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

  @Override
  protected Map<String, Integer> getTrafficWeights(ContextData contextData) {
    List<EncryptedDataDetail> encryptedDataDetails = new ArrayList<>();
    KubernetesConfig kubernetesConfig = getKubernetesConfig(contextData, encryptedDataDetails);
    return kubernetesContainerService.getTrafficWeights(
        kubernetesConfig, encryptedDataDetails, contextData.resizeParams.getContainerServiceName());
  }

  @Override
  protected int getPreviousTrafficPercent(ContextData contextData) {
    List<EncryptedDataDetail> encryptedDataDetails = new ArrayList<>();
    KubernetesConfig kubernetesConfig = getKubernetesConfig(contextData, encryptedDataDetails);
    return kubernetesContainerService.getTrafficPercent(
        kubernetesConfig, encryptedDataDetails, contextData.resizeParams.getContainerServiceName());
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
      int revision = getRevisionFromControllerName(containerServiceData.getName()).orElse(-1);
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

  private void enableAutoscaler(KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails,
      String name, ExecutionLogCallback executionLogCallback) {
    executionLogCallback.saveExecutionLog("Enabling autoscaler " + name, LogLevel.INFO);
    /*
     * Ideally we should be sending resizeParams.getApiVersion(), so we use "v2beta1" when we are dealing with
     * customMetricHPA, but there is a bug in fabric8 library in HasMetadataOperation.replace() method. For
     * customMetricHPA, metric config info resides in HPA.Spec.additionalProperties map. but during execution of
     * replace(), due to build() method in HorizontalPodAutoscalerSpecBuilder, this map goes away, and replace()
     * call actually removes all metricConfig from autoScalar. So currently use v1 version only, till this issue
     * gets fixed. (customMetricConfig is preserved as annotations in version_v1 HPA object, and that path is
     * working fine)
     * */
    kubernetesContainerService.enableAutoscaler(
        kubernetesConfig, encryptedDataDetails, name, ContainerApiVersions.KUBERNETES_V1.getVersionName());
  }

  private void disableAutoscaler(KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails,
      String name, ExecutionLogCallback executionLogCallback) {
    executionLogCallback.saveExecutionLog("Disabling autoscaler " + name, LogLevel.INFO);
    /*
     * Ideally we should be sending resizeParams.getApiVersion(), so we use "v2beta1" when we are dealing with
     * customMetricHPA, but there is a bug in fabric8 library in HasMetadataOperation.replace() method. For
     * customMetricHPA, metric config info resides in HPA.Spec.additionalProperties map. but during execution of
     * replace(), due to build() method in HorizontalPodAutoscalerSpecBuilder, this map goes away, and replace()
     * call actually removes all metricConfig from autoScalar. So currently use v1 version only, till this issue
     * gets fixed. (customMetricConfig is preserved as annotations in version_v1 HPA object, and that path is
     * working fine)
     * */
    kubernetesContainerService.disableAutoscaler(
        kubernetesConfig, encryptedDataDetails, name, ContainerApiVersions.KUBERNETES_V1.getVersionName());
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
