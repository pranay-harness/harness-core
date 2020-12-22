package io.harness.ci.plan.creator.stage;

import static io.harness.common.CICommonPodConstants.POD_NAME_PREFIX;
import static io.harness.pms.yaml.YAMLFieldNameConstants.EXECUTION;
import static io.harness.pms.yaml.YAMLFieldNameConstants.PARALLEL;
import static io.harness.pms.yaml.YAMLFieldNameConstants.SPEC;
import static io.harness.pms.yaml.YAMLFieldNameConstants.STAGE;

import static java.lang.Character.toLowerCase;
import static org.apache.commons.lang3.CharUtils.isAsciiAlphanumeric;

import io.harness.beans.stages.IntegrationStageStepParametersPMS;
import io.harness.ci.integrationstage.CILiteEngineIntegrationStageModifier;
import io.harness.exception.InvalidRequestException;
import io.harness.executionplan.service.ExecutionPlanCreatorHelper;
import io.harness.plancreator.execution.ExecutionElementConfig;
import io.harness.plancreator.stages.stage.StageElementConfig;
import io.harness.pms.contracts.advisers.AdviserObtainment;
import io.harness.pms.contracts.advisers.AdviserType;
import io.harness.pms.contracts.facilitators.FacilitatorObtainment;
import io.harness.pms.sdk.core.adviser.OrchestrationAdviserTypes;
import io.harness.pms.sdk.core.adviser.success.OnSuccessAdviserParameters;
import io.harness.pms.sdk.core.facilitator.child.ChildFacilitator;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;
import io.harness.pms.sdk.core.plan.creation.creators.ChildrenPlanCreator;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;
import io.harness.pms.yaml.YamlUtils;
import io.harness.serializer.KryoSerializer;
import io.harness.states.IntegrationStageStepPMS;
import io.harness.steps.StepOutcomeGroup;
import io.harness.yaml.utils.JsonPipelineUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class IntegrationStagePMSPlanCreator extends ChildrenPlanCreator<StageElementConfig> {
  @Inject private KryoSerializer kryoSerializer;

  public static final String STAGE_NAME = "ci";
  static final String SOURCE = "123456789bcdfghjklmnpqrstvwxyz";
  static final Integer RANDOM_LENGTH = 8;
  @Inject private ExecutionPlanCreatorHelper executionPlanCreatorHelper;
  @Inject private CILiteEngineIntegrationStageModifier ciLiteEngineIntegrationStageModifier;
  private static final SecureRandom random = new SecureRandom();

  @Override
  public Map<String, PlanCreationResponse> createPlanForChildrenNodes(
      PlanCreationContext ctx, StageElementConfig stageElementConfig) {
    // TODO REMOVE THIS AFTER PMS FIX
    stageElementConfig.setType("ci");
    Map<String, PlanCreationResponse> planCreationResponseMap = new LinkedHashMap<>();
    Map<String, YamlField> dependenciesNodeMap = new HashMap<>();
    final String podName = generatePodName(stageElementConfig.getIdentifier());
    YamlField executionField = ctx.getCurrentField().getNode().getField(SPEC).getNode().getField(EXECUTION);

    ExecutionElementConfig executionElementConfig;

    try {
      executionElementConfig = YamlUtils.read(executionField.getNode().toString(), ExecutionElementConfig.class);
    } catch (IOException e) {
      throw new InvalidRequestException("Invalid yaml", e);
    }
    YamlNode parentNode = executionField.getNode().getParentNode();
    ExecutionElementConfig modifiedExecutionPlan = ciLiteEngineIntegrationStageModifier.modifyExecutionPlan(
        executionElementConfig, stageElementConfig, ctx, podName);

    try {
      String jsonString = JsonPipelineUtils.writeJsonString(modifiedExecutionPlan);
      JsonNode jsonNode = JsonPipelineUtils.getMapper().readTree(jsonString);
      YamlNode rootYamlNode = new YamlNode(jsonNode, parentNode);
      dependenciesNodeMap.put(executionField.getNode().getUuid(), new YamlField(EXECUTION, rootYamlNode));
    } catch (IOException e) {
      throw new InvalidRequestException("Invalid yaml", e);
    }
    planCreationResponseMap.put(
        executionField.getNode().getUuid(), PlanCreationResponse.builder().dependencies(dependenciesNodeMap).build());
    return planCreationResponseMap;
  }

  @Override
  public PlanNode createPlanForParentNode(
      PlanCreationContext ctx, StageElementConfig stageElementConfig, List<String> childrenNodeIds) {
    StepParameters stepParameters =
        IntegrationStageStepParametersPMS.getStepParameters(stageElementConfig, childrenNodeIds.get(0));

    return PlanNode.builder()
        .uuid(stageElementConfig.getUuid())
        .name(stageElementConfig.getName())
        .identifier(stageElementConfig.getIdentifier())
        .group(StepOutcomeGroup.STAGE.name())
        .stepParameters(stepParameters)
        .stepType(IntegrationStageStepPMS.STEP_TYPE)
        .facilitatorObtainment(FacilitatorObtainment.newBuilder().setType(ChildFacilitator.FACILITATOR_TYPE).build())
        .adviserObtainments(getAdviserObtainmentFromMetaData(ctx.getCurrentField()))
        .build();
  }

  private List<AdviserObtainment> getAdviserObtainmentFromMetaData(YamlField currentField) {
    List<AdviserObtainment> adviserObtainments = new ArrayList<>();
    if (currentField != null && currentField.getNode() != null) {
      if (checkIfParentIsParallel(currentField)) {
        return adviserObtainments;
      }
      YamlField siblingField =
          currentField.getNode().nextSiblingFromParentArray(currentField.getName(), Arrays.asList(STAGE, PARALLEL));
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
    return Collections.singletonMap(STAGE, Collections.singleton(STAGE_NAME));
  }

  private boolean checkIfParentIsParallel(YamlField currentField) {
    return YamlUtils.getGivenYamlNodeFromParentPath(currentField.getNode(), PARALLEL) != null;
  }

  private String generatePodName(String identifier) {
    return POD_NAME_PREFIX + "-" + getK8PodIdentifier(identifier) + "-"
        + generateRandomAlphaNumericString(RANDOM_LENGTH);
  }

  public String getK8PodIdentifier(String identifier) {
    StringBuilder sb = new StringBuilder(15);
    for (char c : identifier.toCharArray()) {
      if (isAsciiAlphanumeric(c)) {
        sb.append(toLowerCase(c));
      }
      if (sb.length() == 15) {
        return sb.toString();
      }
    }
    return sb.toString();
  }

  public static String generateRandomAlphaNumericString(int length) {
    StringBuilder sb = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
      sb.append(SOURCE.charAt(random.nextInt(SOURCE.length())));
    }
    return sb.toString();
  }
}
