package io.harness.pms.sdk.core.plan.creation.mappers;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.data.structure.CollectionUtils;
import io.harness.pms.plan.PlanNodeProto;
import io.harness.pms.sdk.core.plan.PlanNode;

import lombok.experimental.UtilityClass;

@UtilityClass
public class PlanNodeProtoMapper {
  public PlanNodeProto toPlanNodeProto(PlanNode node) {
    PlanNodeProto.Builder builder =
        PlanNodeProto.newBuilder()
            .setUuid(node.getUuid())
            .setName(isEmpty(node.getName()) ? "" : node.getName())
            .setStepType(node.getStepType())
            .setIdentifier(isEmpty(node.getIdentifier()) ? "" : node.getIdentifier())
            .setStepParameters(node.getStepParameters() == null ? "" : node.getStepParameters().toJson())
            .addAllRebObjects(CollectionUtils.emptyIfNull(node.getRefObjects()))
            .addAllAdviserObtainments(CollectionUtils.emptyIfNull(node.getAdviserObtainments()))
            .addAllFacilitatorObtainments(CollectionUtils.emptyIfNull(node.getFacilitatorObtainments()))
            .setSkipExpressionChain(node.isSkipExpressionChain())
            .setSkipType(node.getSkipGraphType());
    if (node.getGroup() != null) {
      builder.setGroup(node.getGroup());
    }
    return builder.build();
  }
}
