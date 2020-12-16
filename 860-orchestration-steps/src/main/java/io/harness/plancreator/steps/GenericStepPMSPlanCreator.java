package io.harness.plancreator.steps;

import io.harness.data.structure.EmptyPredicate;
import io.harness.pms.contracts.advisers.AdviserObtainment;
import io.harness.pms.contracts.advisers.AdviserType;
import io.harness.pms.contracts.facilitators.FacilitatorObtainment;
import io.harness.pms.contracts.facilitators.FacilitatorType;
import io.harness.pms.sdk.core.adviser.OrchestrationAdviserTypes;
import io.harness.pms.sdk.core.adviser.success.OnSuccessAdviserParameters;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;
import io.harness.pms.sdk.core.plan.creation.creators.PartialPlanCreator;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlUtils;
import io.harness.serializer.KryoSerializer;
import io.harness.steps.StepOutcomeGroup;

import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import java.util.*;

public abstract class GenericStepPMSPlanCreator implements PartialPlanCreator<StepElementConfig> {
  @Inject private KryoSerializer kryoSerializer;

  public abstract Set<String> getSupportedStepTypes();

  @Override
  public Class<StepElementConfig> getFieldClass() {
    return StepElementConfig.class;
  }

  @Override
  public Map<String, Set<String>> getSupportedTypes() {
    Set<String> stepTypes = getSupportedStepTypes();
    if (EmptyPredicate.isEmpty(stepTypes)) {
      return Collections.emptyMap();
    }
    return Collections.singletonMap("step", stepTypes);
  }

  @Override
  public PlanCreationResponse createPlanForField(PlanCreationContext ctx, StepElementConfig stepElement) {
    String nodeName;

    if (EmptyPredicate.isEmpty(stepElement.getName())) {
      nodeName = stepElement.getIdentifier();
    } else {
      nodeName = stepElement.getName();
    }

    PlanNode stepPlanNode =
        PlanNode.builder()
            .uuid(ctx.getCurrentField().getNode().getUuid())
            .name(nodeName)
            .identifier(stepElement.getIdentifier())
            .stepType(stepElement.getStepSpecType().getStepType())
            .group(StepOutcomeGroup.STEP.name())
            .stepParameters(stepElement.getStepSpecType().getStepParameters())
            .facilitatorObtainment(FacilitatorObtainment.newBuilder()
                                       .setType(FacilitatorType.newBuilder()
                                                    .setType(stepElement.getStepSpecType().getFacilitatorType())
                                                    .build())
                                       .build())
            .adviserObtainments(getAdviserObtainmentFromMetaData(ctx.getCurrentField()))
            .build();
    return PlanCreationResponse.builder().node(stepPlanNode.getUuid(), stepPlanNode).build();
  }

  private List<AdviserObtainment> getAdviserObtainmentFromMetaData(YamlField currentField) {
    List<AdviserObtainment> adviserObtainments = new ArrayList<>();
    if (currentField != null && currentField.getNode() != null) {
      if (checkIfParentIsParallel(currentField)) {
        return adviserObtainments;
      }
      YamlField siblingField = currentField.getNode().nextSiblingFromParentArray(
          currentField.getName(), Arrays.asList("step", "parallel", "stepGroup"));
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

  private boolean checkIfParentIsParallel(YamlField currentField) {
    return YamlUtils.getGivenYamlNodeFromParentPath(currentField.getNode(), "parallel") != null;
  }
}
