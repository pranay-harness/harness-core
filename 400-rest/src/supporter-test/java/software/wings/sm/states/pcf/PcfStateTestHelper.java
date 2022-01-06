/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.sm.states.pcf;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.delegate.beans.pcf.ResizeStrategy.RESIZE_NEW_FIRST;

import static software.wings.sm.StateExecutionInstance.Builder.aStateExecutionInstance;
import static software.wings.sm.WorkflowStandardParams.Builder.aWorkflowStandardParams;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_ID;
import static software.wings.utils.WingsTestConstants.COMPUTE_PROVIDER_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID;
import static software.wings.utils.WingsTestConstants.PCF_SERVICE_NAME;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_NAME;
import static software.wings.utils.WingsTestConstants.STATE_NAME;
import static software.wings.utils.WingsTestConstants.WORKFLOW_EXECUTION_ID;

import static java.util.Collections.singletonList;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.api.ContainerServiceElement;
import software.wings.api.DeploymentType;
import software.wings.api.PhaseElement;
import software.wings.api.ServiceElement;
import software.wings.api.pcf.PcfPluginStateExecutionData;
import software.wings.api.pcf.PcfSetupStateExecutionData;
import software.wings.beans.PcfInfrastructureMapping;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.WorkflowStandardParams;

import com.google.common.collect.Lists;
import java.util.List;

@OwnedBy(CDP)
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
public class PcfStateTestHelper {
  public static final String ORG = "ORG";
  public static final String SPACE = "SPACE";
  public static final String PHASE_NAME = "phaseName";

  public ServiceElement getServiceElement() {
    return ServiceElement.builder().uuid(SERVICE_ID).name(SERVICE_NAME).build();
  }

  public PhaseElement getPhaseElement(ServiceElement serviceElement) {
    return PhaseElement.builder()
        .uuid(generateUuid())
        .serviceElement(serviceElement)
        .infraMappingId(INFRA_MAPPING_ID)
        .deploymentType(DeploymentType.PCF.name())
        .workflowExecutionId(WORKFLOW_EXECUTION_ID)
        .phaseName(PHASE_NAME)
        .build();
  }

  public WorkflowStandardParams getWorkflowStandardParams() {
    return aWorkflowStandardParams()
        .withAppId(APP_ID)
        .withEnvId(ENV_ID)
        .withArtifactIds(Lists.newArrayList(ARTIFACT_ID))
        .build();
  }

  public StateExecutionInstance getStateExecutionInstanceForSetupState(
      WorkflowStandardParams workflowStandardParams, PhaseElement phaseElement, ServiceElement serviceElement) {
    return getStateExecutionInstancBuilder(workflowStandardParams, phaseElement, serviceElement)
        .addContextElement(ContainerServiceElement.builder()
                               .uuid(serviceElement.getUuid())
                               .maxInstances(10)
                               .name(PCF_SERVICE_NAME)
                               .resizeStrategy(RESIZE_NEW_FIRST)
                               .infraMappingId(INFRA_MAPPING_ID)
                               .deploymentType(DeploymentType.PCF)
                               .build())
        .build();
  }

  public StateExecutionInstance getStateExecutionInstanceForDeployState(
      WorkflowStandardParams workflowStandardParams, PhaseElement phaseElement, ServiceElement serviceElement) {
    return getStateExecutionInstancBuilder(workflowStandardParams, phaseElement, serviceElement).build();
  }

  public StateExecutionInstance getStateExecutionInstanceForRollbackState(
      WorkflowStandardParams workflowStandardParams, PhaseElement phaseElement, ServiceElement serviceElement) {
    return getStateExecutionInstancBuilder(workflowStandardParams, phaseElement, serviceElement).build();
  }

  public StateExecutionInstance getStateExecutionInstanceForRouteUpdateState(
      WorkflowStandardParams workflowStandardParams, PhaseElement phaseElement, ServiceElement serviceElement) {
    return getStateExecutionInstancBuilder(workflowStandardParams, phaseElement, serviceElement).build();
  }

  private StateExecutionInstance.Builder getStateExecutionInstancBuilder(
      WorkflowStandardParams workflowStandardParams, PhaseElement phaseElement, ServiceElement serviceElement) {
    return aStateExecutionInstance()
        .displayName(STATE_NAME)
        .addContextElement(workflowStandardParams)
        .addContextElement(phaseElement)
        .addStateExecutionData(PcfSetupStateExecutionData.builder().tags(singletonList("tag1")).build());
  }

  public PcfInfrastructureMapping getPcfInfrastructureMapping(List<String> route, List<String> tempRoute) {
    return PcfInfrastructureMapping.builder()
        .organization(ORG)
        .space(SPACE)
        .routeMaps(route)
        .tempRouteMap(tempRoute)
        .computeProviderSettingId(COMPUTE_PROVIDER_ID)
        .build();
  }

  public StateExecutionInstance getStateExecutionInstanceForPluginState(
      WorkflowStandardParams workflowStandardParams, PhaseElement phaseElement, ServiceElement serviceElement) {
    return aStateExecutionInstance()
        .displayName(STATE_NAME)
        .addContextElement(phaseElement)
        .addStateExecutionData(PcfPluginStateExecutionData.builder().build())
        .addContextElement(workflowStandardParams)
        .addContextElement(ContainerServiceElement.builder()
                               .uuid(serviceElement.getUuid())
                               .resizeStrategy(RESIZE_NEW_FIRST)
                               .maxInstances(10)
                               .name(PCF_SERVICE_NAME)
                               .deploymentType(DeploymentType.PCF)
                               .infraMappingId(INFRA_MAPPING_ID)
                               .build())
        .build();
  }
}
