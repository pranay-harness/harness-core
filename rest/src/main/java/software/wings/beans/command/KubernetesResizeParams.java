package software.wings.beans.command;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.api.ContainerServiceData;
import software.wings.beans.InstanceUnitType;
import software.wings.beans.ResizeStrategy;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@EqualsAndHashCode(callSuper = true)
public class KubernetesResizeParams extends ContainerResizeParams {
  private String namespace;
  private boolean useAutoscaler;
  private String apiVersion;
  private String subscriptionId;
  private String resourceGroup;
  private boolean useIstioRouteRule;

  public static final class KubernetesResizeParamsBuilder {
    private String clusterName;
    private int serviceSteadyStateTimeout;
    private String namespace;
    private boolean rollback;
    private boolean useAutoscaler;
    private String containerServiceName;
    private String apiVersion;
    private ResizeStrategy resizeStrategy;
    private String subscriptionId;
    private boolean useFixedInstances;
    private String resourceGroup;
    private int maxInstances;
    private boolean useIstioRouteRule;
    private int fixedInstances;
    private List<ContainerServiceData> newInstanceData;
    private List<ContainerServiceData> oldInstanceData;
    private int instanceCount;
    private InstanceUnitType instanceUnitType;

    private KubernetesResizeParamsBuilder() {}

    public static KubernetesResizeParamsBuilder aKubernetesResizeParams() {
      return new KubernetesResizeParamsBuilder();
    }

    public KubernetesResizeParamsBuilder withClusterName(String clusterName) {
      this.clusterName = clusterName;
      return this;
    }

    public KubernetesResizeParamsBuilder withServiceSteadyStateTimeout(int serviceSteadyStateTimeout) {
      this.serviceSteadyStateTimeout = serviceSteadyStateTimeout;
      return this;
    }

    public KubernetesResizeParamsBuilder withNamespace(String namespace) {
      this.namespace = namespace;
      return this;
    }

    public KubernetesResizeParamsBuilder withRollback(boolean rollback) {
      this.rollback = rollback;
      return this;
    }

    public KubernetesResizeParamsBuilder withUseAutoscaler(boolean useAutoscaler) {
      this.useAutoscaler = useAutoscaler;
      return this;
    }

    public KubernetesResizeParamsBuilder withContainerServiceName(String containerServiceName) {
      this.containerServiceName = containerServiceName;
      return this;
    }

    public KubernetesResizeParamsBuilder withApiVersion(String apiVersion) {
      this.apiVersion = apiVersion;
      return this;
    }

    public KubernetesResizeParamsBuilder withResizeStrategy(ResizeStrategy resizeStrategy) {
      this.resizeStrategy = resizeStrategy;
      return this;
    }

    public KubernetesResizeParamsBuilder withSubscriptionId(String subscriptionId) {
      this.subscriptionId = subscriptionId;
      return this;
    }

    public KubernetesResizeParamsBuilder withUseFixedInstances(boolean useFixedInstances) {
      this.useFixedInstances = useFixedInstances;
      return this;
    }

    public KubernetesResizeParamsBuilder withResourceGroup(String resourceGroup) {
      this.resourceGroup = resourceGroup;
      return this;
    }

    public KubernetesResizeParamsBuilder withMaxInstances(int maxInstances) {
      this.maxInstances = maxInstances;
      return this;
    }

    public KubernetesResizeParamsBuilder withUseIstioRouteRule(boolean useIstioRouteRule) {
      this.useIstioRouteRule = useIstioRouteRule;
      return this;
    }

    public KubernetesResizeParamsBuilder withFixedInstances(int fixedInstances) {
      this.fixedInstances = fixedInstances;
      return this;
    }

    public KubernetesResizeParamsBuilder withNewInstanceData(List<ContainerServiceData> newInstanceData) {
      this.newInstanceData = newInstanceData;
      return this;
    }

    public KubernetesResizeParamsBuilder withOldInstanceData(List<ContainerServiceData> oldInstanceData) {
      this.oldInstanceData = oldInstanceData;
      return this;
    }

    public KubernetesResizeParamsBuilder withInstanceCount(int instanceCount) {
      this.instanceCount = instanceCount;
      return this;
    }

    public KubernetesResizeParamsBuilder withInstanceUnitType(InstanceUnitType instanceUnitType) {
      this.instanceUnitType = instanceUnitType;
      return this;
    }

    public KubernetesResizeParamsBuilder but() {
      return aKubernetesResizeParams()
          .withClusterName(clusterName)
          .withServiceSteadyStateTimeout(serviceSteadyStateTimeout)
          .withNamespace(namespace)
          .withRollback(rollback)
          .withUseAutoscaler(useAutoscaler)
          .withContainerServiceName(containerServiceName)
          .withApiVersion(apiVersion)
          .withResizeStrategy(resizeStrategy)
          .withSubscriptionId(subscriptionId)
          .withUseFixedInstances(useFixedInstances)
          .withResourceGroup(resourceGroup)
          .withMaxInstances(maxInstances)
          .withUseIstioRouteRule(useIstioRouteRule)
          .withFixedInstances(fixedInstances)
          .withNewInstanceData(newInstanceData)
          .withOldInstanceData(oldInstanceData)
          .withInstanceCount(instanceCount)
          .withInstanceUnitType(instanceUnitType);
    }

    public KubernetesResizeParams build() {
      KubernetesResizeParams kubernetesResizeParams = new KubernetesResizeParams();
      kubernetesResizeParams.setClusterName(clusterName);
      kubernetesResizeParams.setServiceSteadyStateTimeout(serviceSteadyStateTimeout);
      kubernetesResizeParams.setNamespace(namespace);
      kubernetesResizeParams.setRollback(rollback);
      kubernetesResizeParams.setUseAutoscaler(useAutoscaler);
      kubernetesResizeParams.setContainerServiceName(containerServiceName);
      kubernetesResizeParams.setApiVersion(apiVersion);
      kubernetesResizeParams.setResizeStrategy(resizeStrategy);
      kubernetesResizeParams.setSubscriptionId(subscriptionId);
      kubernetesResizeParams.setUseFixedInstances(useFixedInstances);
      kubernetesResizeParams.setResourceGroup(resourceGroup);
      kubernetesResizeParams.setMaxInstances(maxInstances);
      kubernetesResizeParams.setUseIstioRouteRule(useIstioRouteRule);
      kubernetesResizeParams.setFixedInstances(fixedInstances);
      kubernetesResizeParams.setNewInstanceData(newInstanceData);
      kubernetesResizeParams.setOldInstanceData(oldInstanceData);
      kubernetesResizeParams.setInstanceCount(instanceCount);
      kubernetesResizeParams.setInstanceUnitType(instanceUnitType);
      return kubernetesResizeParams;
    }
  }
}
