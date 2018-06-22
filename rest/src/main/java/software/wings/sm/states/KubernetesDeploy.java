package software.wings.sm.states;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.beans.command.KubernetesResizeParams.KubernetesResizeParamsBuilder.aKubernetesResizeParams;
import static software.wings.sm.StateType.KUBERNETES_DEPLOY;

import org.apache.commons.lang3.StringUtils;
import software.wings.beans.FeatureName;
import software.wings.beans.InstanceUnitType;
import software.wings.beans.command.ContainerApiVersions;
import software.wings.beans.command.ContainerResizeParams;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;

/**
 * Created by brett on 3/1/17
 */
public class KubernetesDeploy extends ContainerServiceDeploy {
  private String instanceCount;
  private String downsizeInstanceCount;
  private String trafficPercent;
  private InstanceUnitType instanceUnitType = InstanceUnitType.PERCENTAGE;
  private InstanceUnitType downsizeInstanceUnitType = InstanceUnitType.PERCENTAGE;
  private String commandName = "Resize Replication Controller";

  public KubernetesDeploy(String name) {
    super(name, KUBERNETES_DEPLOY.name());
  }

  @Override
  public String getCommandName() {
    return commandName;
  }

  public void setCommandName(String commandName) {
    this.commandName = commandName;
  }

  @Override
  public String getInstanceCount() {
    return instanceCount;
  }

  public void setInstanceCount(String instanceCount) {
    this.instanceCount = instanceCount;
  }

  @Override
  public String getDownsizeInstanceCount() {
    return downsizeInstanceCount;
  }

  public void setDownsizeInstanceCount(String downsizeInstanceCount) {
    this.downsizeInstanceCount = downsizeInstanceCount;
  }

  @Override
  public InstanceUnitType getInstanceUnitType() {
    return instanceUnitType;
  }

  public void setInstanceUnitType(InstanceUnitType instanceUnitType) {
    this.instanceUnitType = instanceUnitType;
  }

  @Override
  public InstanceUnitType getDownsizeInstanceUnitType() {
    return downsizeInstanceUnitType;
  }

  public void setDownsizeInstanceUnitType(InstanceUnitType downsizeInstanceUnitType) {
    this.downsizeInstanceUnitType = downsizeInstanceUnitType;
  }

  public String getTrafficPercent() {
    return trafficPercent;
  }

  public void setTrafficPercent(String trafficPercent) {
    this.trafficPercent = trafficPercent;
  }

  @Override
  protected ContainerResizeParams buildContainerResizeParams(ExecutionContext context, ContextData contextData) {
    Integer trafficPercent =
        isNotBlank(getTrafficPercent()) ? Integer.valueOf(context.renderExpression(getTrafficPercent())) : null;
    boolean useDashInHostName =
        featureFlagService.isEnabled(FeatureName.USE_DASH_IN_HOSTNAME, contextData.app.getAccountId());

    return aKubernetesResizeParams()
        .withClusterName(contextData.containerElement.getClusterName())
        .withNamespace(contextData.containerElement.getNamespace())
        .withSubscriptionId(contextData.subscriptionId)
        .withResourceGroup(contextData.resourceGroup)
        .withServiceSteadyStateTimeout(contextData.containerElement.getServiceSteadyStateTimeout())
        .withUseAutoscaler(contextData.containerElement.isUseAutoscaler())
        .withAutoscalerYaml(contextData.containerElement.getAutoscalerYaml())
        .withApiVersion(getApiVersion(contextData))
        .withUseIstioRouteRule(contextData.containerElement.isUseIstioRouteRule())
        .withContainerServiceName(contextData.containerElement.getName())
        .withImage(contextData.containerElement.getImage())
        .withResizeStrategy(contextData.containerElement.getResizeStrategy())
        .withUseFixedInstances(contextData.containerElement.isUseFixedInstances())
        .withMaxInstances(contextData.containerElement.getMaxInstances())
        .withFixedInstances(contextData.containerElement.getFixedInstances())
        .withOriginalServiceCounts(contextData.containerElement.getActiveServiceCounts())
        .withOriginalTrafficWeights(contextData.containerElement.getTrafficWeights())
        .withRollback(false)
        .withInstanceCount(contextData.instanceCount)
        .withInstanceUnitType(getInstanceUnitType())
        .withDownsizeInstanceCount(contextData.downsizeInstanceCount)
        .withDownsizeInstanceUnitType(getDownsizeInstanceUnitType())
        .withTrafficPercent(trafficPercent)
        .withUseDashInHostName(useDashInHostName)
        .build();
  }

  private String getApiVersion(ContextData contextData) {
    return StringUtils.isEmpty(contextData.containerElement.getCustomMetricYamlConfig())
        ? ContainerApiVersions.KUBERNETES_V1.getVersionName()
        : ContainerApiVersions.KUBERNETES_V2_BETA1.getVersionName();
  }

  public static final class KubernetesDeployBuilder {
    private String id;
    private String name;
    private ContextElementType requiredContextElementType;
    private String stateType;
    private String commandName;
    private String instanceCount;
    private InstanceUnitType instanceUnitType = InstanceUnitType.COUNT;

    private KubernetesDeployBuilder(String name) {
      this.name = name;
    }

    public static KubernetesDeployBuilder aKubernetesDeploy(String name) {
      return new KubernetesDeployBuilder(name);
    }

    public KubernetesDeployBuilder withId(String id) {
      this.id = id;
      return this;
    }

    public KubernetesDeployBuilder withName(String name) {
      this.name = name;
      return this;
    }

    public KubernetesDeployBuilder withRequiredContextElementType(ContextElementType requiredContextElementType) {
      this.requiredContextElementType = requiredContextElementType;
      return this;
    }

    public KubernetesDeployBuilder withStateType(String stateType) {
      this.stateType = stateType;
      return this;
    }

    public KubernetesDeployBuilder withCommandName(String commandName) {
      this.commandName = commandName;
      return this;
    }

    public KubernetesDeployBuilder withInstanceCount(String instanceCount) {
      this.instanceCount = instanceCount;
      return this;
    }

    public KubernetesDeployBuilder withInstanceUnitType(InstanceUnitType instanceUnitType) {
      this.instanceUnitType = instanceUnitType;
      return this;
    }

    public KubernetesDeploy build() {
      KubernetesDeploy kubernetesReplicationControllerDeploy = new KubernetesDeploy(name);
      kubernetesReplicationControllerDeploy.setId(id);
      kubernetesReplicationControllerDeploy.setRequiredContextElementType(requiredContextElementType);
      kubernetesReplicationControllerDeploy.setStateType(stateType);
      kubernetesReplicationControllerDeploy.setRollback(false);
      kubernetesReplicationControllerDeploy.setCommandName(commandName);
      kubernetesReplicationControllerDeploy.setInstanceCount(instanceCount);
      kubernetesReplicationControllerDeploy.setInstanceUnitType(instanceUnitType);
      return kubernetesReplicationControllerDeploy;
    }
  }
}
