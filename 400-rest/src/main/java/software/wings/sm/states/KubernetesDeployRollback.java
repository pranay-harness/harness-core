/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.sm.states;

import static software.wings.beans.command.KubernetesResizeParams.KubernetesResizeParamsBuilder.aKubernetesResizeParams;
import static software.wings.sm.StateType.KUBERNETES_DEPLOY_ROLLBACK;

import software.wings.beans.InstanceUnitType;
import software.wings.beans.command.ContainerResizeParams;
import software.wings.beans.container.Label;
import software.wings.sm.ExecutionContext;

import com.github.reinert.jjschema.Attributes;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by brett on 4/24/17
 */
public class KubernetesDeployRollback extends ContainerServiceDeploy {
  @Attributes(title = "Rollback all phases at once") private boolean rollbackAllPhases;

  private String commandName = "Resize Replication Controller";

  public KubernetesDeployRollback(String name) {
    super(name, KUBERNETES_DEPLOY_ROLLBACK.name());
  }

  public boolean isRollbackAllPhases() {
    return rollbackAllPhases;
  }

  public void setRollbackAllPhases(boolean rollbackAllPhases) {
    this.rollbackAllPhases = rollbackAllPhases;
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
    return "0";
  }

  @Override
  public InstanceUnitType getInstanceUnitType() {
    return null;
  }

  @Override
  public String getDownsizeInstanceCount() {
    return null;
  }

  @Override
  public InstanceUnitType getDownsizeInstanceUnitType() {
    return null;
  }

  @Override
  protected ContainerResizeParams buildContainerResizeParams(ExecutionContext context, ContextData contextData) {
    boolean useNewLabelMechanism = true;

    Map<String, String> labelMap = (contextData.containerElement.getLookupLabels() != null)
        ? contextData.containerElement.getLookupLabels().stream().collect(
            Collectors.toMap(Label::getName, Label::getValue))
        : null;

    return aKubernetesResizeParams()
        .withClusterName(contextData.containerElement.getClusterName())
        .withNamespace(contextData.containerElement.getNamespace())
        .withServiceSteadyStateTimeout(contextData.containerElement.getServiceSteadyStateTimeout())
        .withUseAutoscaler(contextData.containerElement.isUseAutoscaler())
        .withSubscriptionId(contextData.subscriptionId)
        .withResourceGroup(contextData.resourceGroup)
        .withUseIstioRouteRule(contextData.containerElement.isUseIstioRouteRule())
        .withContainerServiceName(contextData.containerElement.getName())
        .withResizeStrategy(contextData.containerElement.getResizeStrategy())
        .withUseFixedInstances(contextData.containerElement.isUseFixedInstances())
        .withMaxInstances(contextData.containerElement.getMaxInstances())
        .withFixedInstances(contextData.containerElement.getFixedInstances())
        .withNewInstanceData(contextData.rollbackElement.getNewInstanceData())
        .withOldInstanceData(contextData.rollbackElement.getOldInstanceData())
        .withNamespace(contextData.rollbackElement.getNamespace())
        .withOriginalServiceCounts(contextData.containerElement.getActiveServiceCounts())
        .withOriginalTrafficWeights(contextData.containerElement.getTrafficWeights())
        .withRollback(true)
        .withRollbackAllPhases(rollbackAllPhases)
        .withUseNewLabelMechanism(useNewLabelMechanism)
        .withLookupLabels(labelMap)
        .build();
  }
}
