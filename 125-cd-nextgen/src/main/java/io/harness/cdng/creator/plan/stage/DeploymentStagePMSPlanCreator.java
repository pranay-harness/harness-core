package io.harness.cdng.creator.plan.stage;

import static io.harness.pms.yaml.YAMLFieldNameConstants.STAGES;

import io.harness.cdng.creator.plan.infrastructure.InfrastructurePmsPlanCreator;
import io.harness.cdng.creator.plan.service.ServicePMSPlanCreator;
import io.harness.cdng.pipeline.PipelineInfrastructure;
import io.harness.cdng.pipeline.beans.DeploymentStageStepParameters;
import io.harness.cdng.pipeline.steps.DeploymentStageStep;
import io.harness.cdng.visitor.YamlTypes;
import io.harness.exception.InvalidRequestException;
import io.harness.plancreator.stages.stage.StageElementConfig;
import io.harness.pms.contracts.advisers.AdviserObtainment;
import io.harness.pms.contracts.advisers.AdviserType;
import io.harness.pms.contracts.facilitators.FacilitatorObtainment;
import io.harness.pms.execution.utils.RunInfoUtils;
import io.harness.pms.execution.utils.SkipInfoUtils;
import io.harness.pms.sdk.core.adviser.OrchestrationAdviserTypes;
import io.harness.pms.sdk.core.adviser.success.OnSuccessAdviserParameters;
import io.harness.pms.sdk.core.facilitator.child.ChildFacilitator;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;
import io.harness.pms.sdk.core.plan.creation.creators.ChildrenPlanCreator;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;
import io.harness.serializer.KryoSerializer;
import io.harness.steps.StepOutcomeGroup;

import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DeploymentStagePMSPlanCreator extends ChildrenPlanCreator<StageElementConfig> {
  @Inject private KryoSerializer kryoSerializer;

  @Override
  public LinkedHashMap<String, PlanCreationResponse> createPlanForChildrenNodes(
      PlanCreationContext ctx, StageElementConfig field) {
    LinkedHashMap<String, PlanCreationResponse> planCreationResponseMap = new LinkedHashMap<>();
    Map<String, YamlField> dependenciesNodeMap = new HashMap<>();

    // Adding service child
    YamlField serviceField =
        ctx.getCurrentField().getNode().getField(YamlTypes.SPEC).getNode().getField(YamlTypes.SERVICE_CONFIG);

    if (serviceField != null) {
      PlanNode servicePlanNode = ServicePMSPlanCreator.createPlanForServiceNode(
          serviceField, ((DeploymentStageConfig) field.getStageType()).getServiceConfig(), kryoSerializer);
      planCreationResponseMap.put(serviceField.getNode().getUuid(),
          PlanCreationResponse.builder().node(serviceField.getNode().getUuid(), servicePlanNode).build());
    }

    // Adding infrastructure node
    YamlField infraField =
        ctx.getCurrentField().getNode().getField(YamlTypes.SPEC).getNode().getField(YamlTypes.PIPELINE_INFRASTRUCTURE);
    if (infraField == null) {
      throw new InvalidRequestException("Infrastructure section cannot be absent in a pipeline");
    }
    PipelineInfrastructure actualInfraConfig = InfrastructurePmsPlanCreator.getActualInfraConfig(
        ((DeploymentStageConfig) field.getStageType()).getInfrastructure(), infraField);

    PlanNode infraStepNode = InfrastructurePmsPlanCreator.getInfraStepPlanNode(
        ((DeploymentStageConfig) field.getStageType()).getInfrastructure(), infraField);
    planCreationResponseMap.put(
        infraStepNode.getUuid(), PlanCreationResponse.builder().node(infraStepNode.getUuid(), infraStepNode).build());
    String infraSectionNodeChildId = infraStepNode.getUuid();

    if (InfrastructurePmsPlanCreator.isProvisionerConfigured(actualInfraConfig)) {
      planCreationResponseMap.putAll(InfrastructurePmsPlanCreator.createPlanForProvisioner(
          actualInfraConfig, infraField, infraStepNode.getUuid(), kryoSerializer));
      infraSectionNodeChildId = InfrastructurePmsPlanCreator.getProvisionerNodeId(infraField);
    }

    YamlNode infraNode = infraField.getNode();

    PlanNode infraSectionPlanNode =
        InfrastructurePmsPlanCreator.getInfraSectionPlanNode(infraNode, infraSectionNodeChildId,
            ((DeploymentStageConfig) field.getStageType()).getInfrastructure(), kryoSerializer, infraField);
    planCreationResponseMap.put(
        infraNode.getUuid(), PlanCreationResponse.builder().node(infraNode.getUuid(), infraSectionPlanNode).build());

    // Add dependency for execution
    YamlField executionField =
        ctx.getCurrentField().getNode().getField(YamlTypes.SPEC).getNode().getField(YAMLFieldNameConstants.EXECUTION);
    if (executionField == null) {
      throw new InvalidRequestException("Execution section cannot be absent in a pipeline");
    }
    dependenciesNodeMap.put(executionField.getNode().getUuid(), executionField);

    planCreationResponseMap.put(
        executionField.getNode().getUuid(), PlanCreationResponse.builder().dependencies(dependenciesNodeMap).build());
    return planCreationResponseMap;
  }

  @Override
  public PlanNode createPlanForParentNode(
      PlanCreationContext ctx, StageElementConfig config, List<String> childrenNodeIds) {
    StepParameters stepParameters = DeploymentStageStepParameters.getStepParameters(config, childrenNodeIds.get(0));
    return PlanNode.builder()
        .uuid(config.getUuid())
        .name(config.getName())
        .identifier(config.getIdentifier())
        .group(StepOutcomeGroup.STAGE.name())
        .stepParameters(stepParameters)
        .stepType(DeploymentStageStep.STEP_TYPE)
        .skipCondition(SkipInfoUtils.getSkipCondition(config.getSkipCondition()))
        .whenCondition(RunInfoUtils.getRunCondition(config.getWhen(), true))
        .facilitatorObtainment(FacilitatorObtainment.newBuilder().setType(ChildFacilitator.FACILITATOR_TYPE).build())
        .adviserObtainments(getAdviserObtainmentFromMetaData(ctx.getCurrentField()))
        .build();
  }

  private List<AdviserObtainment> getAdviserObtainmentFromMetaData(YamlField currentField) {
    List<AdviserObtainment> adviserObtainments = new ArrayList<>();
    if (currentField != null && currentField.getNode() != null) {
      if (currentField.checkIfParentIsParallel(STAGES)) {
        return adviserObtainments;
      }
      YamlField siblingField = currentField.getNode().nextSiblingFromParentArray(
          currentField.getName(), Arrays.asList(YAMLFieldNameConstants.STAGE, YAMLFieldNameConstants.PARALLEL));
      if (siblingField != null && siblingField.getNode().getUuid() != null) {
        adviserObtainments.add(
            AdviserObtainment.newBuilder()
                .setType(AdviserType.newBuilder().setType(OrchestrationAdviserTypes.ON_SUCCESS.name()).build())
                .setParameters(ByteString.copyFrom(kryoSerializer.asBytes(
                    OnSuccessAdviserParameters.builder().nextNodeId(siblingField.getNode().getUuid()).build())))
                .build());
      }
    }
    return adviserObtainments;
  }

  @Override
  public Class<StageElementConfig> getFieldClass() {
    return StageElementConfig.class;
  }

  @Override
  public Map<String, Set<String>> getSupportedTypes() {
    return Collections.singletonMap(YAMLFieldNameConstants.STAGE, Collections.singleton("Deployment"));
  }
}
