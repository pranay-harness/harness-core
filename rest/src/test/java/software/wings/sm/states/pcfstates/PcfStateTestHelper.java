package software.wings.sm.states.pcfstates;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static software.wings.api.CommandStateExecutionData.Builder.aCommandStateExecutionData;
import static software.wings.api.PhaseElement.PhaseElementBuilder.aPhaseElement;
import static software.wings.api.ServiceElement.Builder.aServiceElement;
import static software.wings.beans.ResizeStrategy.RESIZE_NEW_FIRST;
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

import com.google.common.collect.Lists;

import software.wings.api.ContainerServiceElement;
import software.wings.api.DeploymentType;
import software.wings.api.PhaseElement;
import software.wings.api.ServiceElement;
import software.wings.api.pcf.PcfDeployContextElement;
import software.wings.api.pcf.PcfServiceData;
import software.wings.api.pcf.PcfSetupContextElement;
import software.wings.beans.PcfInfrastructureMapping;
import software.wings.helpers.ext.pcf.request.PcfCommandSetupRequest;
import software.wings.helpers.ext.pcf.response.PcfAppSetupTimeDetails;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.WorkflowStandardParams;

import java.util.Arrays;
import java.util.List;

public class PcfStateTestHelper {
  public static final String ORG = "ORG";
  public static final String SPACE = "SPACE";

  public ServiceElement getServiceElement() {
    return aServiceElement().withUuid(SERVICE_ID).withName(SERVICE_NAME).build();
  }

  public PhaseElement getPhaseElement(ServiceElement serviceElement) {
    return aPhaseElement()
        .withUuid(generateUuid())
        .withServiceElement(serviceElement)
        .withInfraMappingId(INFRA_MAPPING_ID)
        .withDeploymentType(DeploymentType.PCF.name())
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
    return getStateExecutionInstancBuilder(workflowStandardParams, phaseElement, serviceElement)
        .addContextElement(
            PcfSetupContextElement.builder()
                .uuid(serviceElement.getUuid())
                .name(PCF_SERVICE_NAME)
                .maxInstanceCount(10)
                .pcfCommandRequest(PcfCommandSetupRequest.builder().space(SPACE).organization(ORG).build())
                .newPcfApplicationDetails(PcfAppSetupTimeDetails.builder()
                                              .applicationName("APP_NAME_SERVICE_NAME_ENV_NAME__1")
                                              .applicationGuid("1")
                                              .build())
                .infraMappingId(INFRA_MAPPING_ID)
                .resizeStrategy(RESIZE_NEW_FIRST)
                .routeMaps(Arrays.asList("R1", "R2"))
                .build())
        .build();
  }

  public StateExecutionInstance getStateExecutionInstanceForRollbackState(
      WorkflowStandardParams workflowStandardParams, PhaseElement phaseElement, ServiceElement serviceElement) {
    return getStateExecutionInstancBuilder(workflowStandardParams, phaseElement, serviceElement)
        .addContextElement(
            PcfDeployContextElement.builder()
                .uuid(serviceElement.getUuid())
                .name(PCF_SERVICE_NAME)
                .instanceData(Arrays.asList(
                    PcfServiceData.builder().previousCount(1).desiredCount(0).name("APP_SERVICE_ENV__1").build(),
                    PcfServiceData.builder().previousCount(1).desiredCount(0).name("APP_SERVICE_ENV__2").build(),
                    PcfServiceData.builder().previousCount(0).desiredCount(2).name("APP_SERVICE_ENV__3").build()))
                //.resizeStrategy(RESIZE_NEW_FIRST)
                .build())
        .build();
  }

  public StateExecutionInstance getStateExecutionInstanceForRouteUpdateState(
      WorkflowStandardParams workflowStandardParams, PhaseElement phaseElement, ServiceElement serviceElement) {
    return getStateExecutionInstancBuilder(workflowStandardParams, phaseElement, serviceElement)
        .addContextElement(
            PcfSetupContextElement.builder()
                .uuid(serviceElement.getUuid())
                .name(PCF_SERVICE_NAME)
                .maxInstanceCount(10)
                .pcfCommandRequest(PcfCommandSetupRequest.builder().space(SPACE).organization(ORG).build())
                .newPcfApplicationDetails(PcfAppSetupTimeDetails.builder()
                                              .applicationName("APP_NAME_SERVICE_NAME_ENV_NAME__2")
                                              .applicationGuid("1")
                                              .urls(Arrays.asList("R1", "R2"))
                                              .build())
                .infraMappingId(INFRA_MAPPING_ID)
                .resizeStrategy(RESIZE_NEW_FIRST)
                .routeMaps(Arrays.asList("R1", "R2"))
                .tempRouteMap(Arrays.asList("R3"))
                .appDetailsToBeDownsized(Arrays.asList(PcfAppSetupTimeDetails.builder()
                                                           .applicationName("APP_NAME_SERVICE_NAME_ENV_NAME__1")
                                                           .urls(Arrays.asList("R3"))
                                                           .build()))
                .build())
        .build();
  }

  private StateExecutionInstance.Builder getStateExecutionInstancBuilder(
      WorkflowStandardParams workflowStandardParams, PhaseElement phaseElement, ServiceElement serviceElement) {
    return aStateExecutionInstance()
        .withDisplayName(STATE_NAME)
        .addContextElement(workflowStandardParams)
        .addContextElement(phaseElement)
        .addStateExecutionData(aCommandStateExecutionData().build());
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
}
