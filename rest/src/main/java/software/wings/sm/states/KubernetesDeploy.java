package software.wings.sm.states;

import static software.wings.beans.command.KubernetesResizeParams.KubernetesResizeParamsBuilder.aKubernetesResizeParams;
import static software.wings.sm.StateType.KUBERNETES_DEPLOY;

import com.github.reinert.jjschema.Attributes;
import org.apache.commons.lang3.StringUtils;
import software.wings.api.ContainerServiceData;
import software.wings.beans.InstanceUnitType;
import software.wings.beans.command.ContainerApiVersions;
import software.wings.beans.command.ContainerResizeParams;
import software.wings.sm.ContextElementType;
import software.wings.stencils.DefaultValue;
import software.wings.stencils.EnumData;

import java.util.List;

/**
 * Created by brett on 3/1/17
 */
public class KubernetesDeploy extends ContainerServiceDeploy {
  @Attributes(title = "Desired Instances (cumulative)") private String instanceCount;

  @Attributes(title = "Instance Unit Type (Count/Percent)")
  @EnumData(enumDataProvider = InstanceUnitTypeDataProvider.class)
  @DefaultValue("COUNT")
  private InstanceUnitType instanceUnitType = InstanceUnitType.COUNT;

  @Attributes(title = "Command")
  @EnumData(enumDataProvider = CommandStateEnumDataProvider.class)
  @DefaultValue("Resize Replication Controller")
  private String commandName;

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
  public InstanceUnitType getInstanceUnitType() {
    return instanceUnitType;
  }

  public void setInstanceUnitType(InstanceUnitType instanceUnitType) {
    this.instanceUnitType = instanceUnitType;
  }

  @Override
  protected ContainerResizeParams buildContainerResizeParams(
      ContextData contextData, List<ContainerServiceData> desiredCounts) {
    return aKubernetesResizeParams()
        .withClusterName(contextData.containerElement.getClusterName())
        .withDesiredCounts(desiredCounts)
        .withNamespace(contextData.containerElement.getNamespace())
        .withServiceSteadyStateTimeout(contextData.containerElement.getServiceSteadyStateTimeout())
        .withDeployingToHundredPercent(contextData.deployingToHundredPercent)
        .withUseAutoscaler(contextData.containerElement.isUseAutoscaler())
        .withSubscriptionId(contextData.containerServiceParams.getSubscriptionId())
        .withResourceGroup(contextData.containerServiceParams.getResourceGroup())
        .withApiVersion(getApiVersion(contextData))
        .withUseIstioRouteRule(contextData.containerElement.isUseIstioRouteRule())
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
