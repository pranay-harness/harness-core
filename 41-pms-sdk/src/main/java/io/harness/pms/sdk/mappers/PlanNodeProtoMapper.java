package io.harness.pms.sdk.mappers;

import io.harness.data.structure.CollectionUtils;
import io.harness.pms.plan.PlanNodeProto;
import io.harness.pms.sdk.beans.PlanNode;

import lombok.experimental.UtilityClass;

@UtilityClass
public class PlanNodeProtoMapper {
  public PlanNodeProto toPlanNodeProto(PlanNode node) {
    PlanNodeProto.Builder builder =
        PlanNodeProto.newBuilder()
            .setUuid(node.getUuid())
            .setName(node.getName())
            .setStepType(node.getStepType())
            .setIdentifier(node.getIdentifier())
            .setStepParameters(node.getStepParameters().toJson())
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
