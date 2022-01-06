/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.sm.states;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import static software.wings.beans.command.KubernetesResizeParams.KubernetesResizeParamsBuilder.aKubernetesResizeParams;
import static software.wings.sm.StateType.KUBERNETES_DEPLOY;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.context.ContextElementType;
import io.harness.k8s.model.ContainerApiVersions;

import software.wings.beans.InstanceUnitType;
import software.wings.beans.command.ContainerResizeParams;
import software.wings.beans.container.Label;
import software.wings.sm.ExecutionContext;

import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

/**
 * Created by brett on 3/1/17
 */
@OwnedBy(CDP)
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
public class KubernetesDeploy extends ContainerServiceDeploy {
  public static final String INSTANCE_UNIT_TYPE_KEY = "instanceUnitType";
  public static final String INSTANCE_COUNT_KEY = "instanceCount";

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

    boolean useNewLabelMechanism = true;

    Map<String, String> labelMap = (contextData.containerElement.getLookupLabels() != null)
        ? contextData.containerElement.getLookupLabels().stream().collect(
            Collectors.toMap(Label::getName, Label::getValue))
        : null;

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
        .withUseNewLabelMechanism(useNewLabelMechanism)
        .withLookupLabels(labelMap)
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
