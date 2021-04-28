package io.harness.cdng.creator.plan.stage;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.UUIDGenerator.generateUuid;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.creator.plan.execution.CDExecutionPMSPlanCreator;
import io.harness.cdng.creator.plan.infrastructure.InfrastructurePmsPlanCreator;
import io.harness.cdng.creator.plan.service.ServicePMSPlanCreator;
import io.harness.cdng.pipeline.PipelineInfrastructure;
import io.harness.cdng.pipeline.beans.DeploymentStageStepParameters;
import io.harness.cdng.pipeline.steps.DeploymentStageStep;
import io.harness.cdng.visitor.YamlTypes;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.plancreator.stages.GenericStagePlanCreator;
import io.harness.plancreator.stages.stage.StageElementConfig;
import io.harness.plancreator.steps.GenericStepPMSPlanCreator;
import io.harness.plancreator.steps.common.SpecParameters;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;
import io.harness.pms.utilities.ResourceConstraintUtility;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;
import io.harness.serializer.KryoSerializer;
import io.harness.yaml.core.failurestrategy.FailureStrategyConfig;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@OwnedBy(CDC)
public class DeploymentStagePMSPlanCreator extends GenericStagePlanCreator {
  @Inject private KryoSerializer kryoSerializer;

  @Override
  public Set<String> getSupportedStageTypes() {
    return Collections.singleton("Deployment");
  }

  @Override
  public StepType getStepType(StageElementConfig stageElementConfig) {
    return DeploymentStageStep.STEP_TYPE;
  }

  @Override
  public SpecParameters getSpecParameters(String childNodeId) {
    return DeploymentStageStepParameters.getStepParameters(childNodeId);
  }

  @Override
  public LinkedHashMap<String, PlanCreationResponse> createPlanForChildrenNodes(
      PlanCreationContext ctx, StageElementConfig field) {
    LinkedHashMap<String, PlanCreationResponse> planCreationResponseMap = new LinkedHashMap<>();
    Map<String, YamlField> dependenciesNodeMap = new HashMap<>();

    // Validate Stage Failure strategy.
    validateFailureStrategy(field);

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

    PipelineInfrastructure pipelineInfrastructure = ((DeploymentStageConfig) field.getStageType()).getInfrastructure();
    PipelineInfrastructure actualInfraConfig =
        InfrastructurePmsPlanCreator.getActualInfraConfig(pipelineInfrastructure, infraField);

    PlanNode infraStepNode = InfrastructurePmsPlanCreator.getInfraStepPlanNode(pipelineInfrastructure, infraField);
    planCreationResponseMap.put(
        infraStepNode.getUuid(), PlanCreationResponse.builder().node(infraStepNode.getUuid(), infraStepNode).build());
    String infraSectionNodeChildId = infraStepNode.getUuid();

    if (InfrastructurePmsPlanCreator.isProvisionerConfigured(actualInfraConfig)) {
      planCreationResponseMap.putAll(InfrastructurePmsPlanCreator.createPlanForProvisioner(
          actualInfraConfig, infraField, infraStepNode.getUuid(), kryoSerializer));
      infraSectionNodeChildId = InfrastructurePmsPlanCreator.getProvisionerNodeId(infraField);
    }

    YamlNode infraNode = infraField.getNode();

    YamlField rcYamlField = constructResourceConstraintYamlField(infraNode);

    PlanNode infraSectionPlanNode = InfrastructurePmsPlanCreator.getInfraSectionPlanNode(
        infraNode, infraSectionNodeChildId, pipelineInfrastructure, kryoSerializer, infraField, rcYamlField);
    planCreationResponseMap.put(
        infraNode.getUuid(), PlanCreationResponse.builder().node(infraNode.getUuid(), infraSectionPlanNode).build());

    // Add dependency for resource constraint
    if (pipelineInfrastructure.isAllowSimultaneousDeployments()) {
      dependenciesNodeMap.put(rcYamlField.getNode().getUuid(), rcYamlField);
    }

    // Add dependency for execution
    YamlField executionField =
        ctx.getCurrentField().getNode().getField(YamlTypes.SPEC).getNode().getField(YAMLFieldNameConstants.EXECUTION);
    if (executionField == null) {
      throw new InvalidRequestException("Execution section cannot be absent in a pipeline");
    }
    PlanCreationResponse planForExecution = CDExecutionPMSPlanCreator.createPlanForExecution(executionField);
    planCreationResponseMap.put(executionField.getNode().getUuid(), planForExecution);

    planCreationResponseMap.put(
        rcYamlField.getNode().getUuid(), PlanCreationResponse.builder().dependencies(dependenciesNodeMap).build());
    return planCreationResponseMap;
  }

  private YamlField constructResourceConstraintYamlField(YamlNode infraNode) {
    JsonNode resourceConstraintJsonNode =
        ResourceConstraintUtility.getResourceConstraintJsonNode(obtainResourceUnitFromInfrastructure(infraNode));
    return new YamlField("step", new YamlNode(resourceConstraintJsonNode, infraNode.getParentNode()));
  }

  private String obtainResourceUnitFromInfrastructure(YamlNode infraNode) {
    JsonNode infrastructureKey = infraNode.getCurrJsonNode().get("infrastructureKey");
    String resourceUnit;
    if (infrastructureKey == null) {
      resourceUnit = generateUuid();
    } else {
      resourceUnit = infrastructureKey.asText();
    }
    return resourceUnit;
  }

  private void validateFailureStrategy(StageElementConfig stageElementConfig) {
    // Failure strategy should be present.
    List<FailureStrategyConfig> stageFailureStrategies = stageElementConfig.getFailureStrategies();
    if (EmptyPredicate.isEmpty(stageFailureStrategies)) {
      throw new InvalidRequestException("There should be atleast one failure strategy configured at stage level.");
    }

    // checking stageFailureStrategies is having one strategy with error type as AnyOther and along with that no
    // error type is involved
    if (!GenericStepPMSPlanCreator.containsOnlyAnyOtherErrorInSomeConfig(stageFailureStrategies)) {
      throw new InvalidRequestException(
          "There should be a Failure strategy that contains one error type as AnyOther, with no other error type along with it in that Failure Strategy.");
    }
  }
}
