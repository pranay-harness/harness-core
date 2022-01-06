/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.plan;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.steps.SkipType;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.data.stepparameters.PmsStepParameters;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@OwnedBy(HarnessTeam.PIPELINE)
@Value
@Builder
@FieldNameConstants(innerTypeName = "IdentityPlanNodeKeys")
public class IdentityPlanNode implements Node {
  @NotNull String uuid;
  @NotNull String name;
  @NotNull String identifier;
  String group;
  boolean isSkipExpressionChain;
  String whenCondition;
  String skipCondition;
  @Builder.Default SkipType skipGraphType = SkipType.NOOP;
  StepType originalStepType;
  String stageFqn;
  StepType stepType;
  String originalNodeExecutionId;
  String serviceName;

  @Override
  public String getStageFqn() {
    return this.stageFqn;
  }

  @Override
  public NodeType getNodeType() {
    return NodeType.IDENTITY_PLAN_NODE;
  }

  @Override
  public PmsStepParameters getStepParameters() {
    PmsStepParameters stepParameters = new PmsStepParameters();
    stepParameters.put(IdentityPlanNodeKeys.originalNodeExecutionId, originalNodeExecutionId);
    return stepParameters;
  }

  @Override
  public boolean isSkipExpressionChain() {
    return this.isSkipExpressionChain;
  }

  @Override
  public String getWhenCondition() {
    return this.whenCondition;
  }

  @Override
  public String getSkipCondition() {
    return this.skipCondition;
  }

  @Override
  public SkipType getSkipGraphType() {
    return this.skipGraphType;
  }

  @Override
  public StepCategory getStepCategory() {
    return this.originalStepType.getStepCategory();
  }

  public static IdentityPlanNode mapPlanNodeToIdentityNode(
      Node node, StepType stepType, String originalNodeExecutionUuid) {
    return IdentityPlanNode.builder()
        .uuid(node.getUuid())
        .name(node.getName())
        .identifier(node.getIdentifier())
        .group(node.getGroup())
        .skipGraphType(node.getSkipGraphType())
        .stepType(stepType)
        .isSkipExpressionChain(node.isSkipExpressionChain())
        .serviceName(node.getServiceName())
        .originalStepType(node.getNodeType() == NodeType.IDENTITY_PLAN_NODE
                ? ((IdentityPlanNode) node).getOriginalStepType()
                : node.getStepType())
        .stageFqn(node.getStageFqn())
        .whenCondition(node.getWhenCondition())
        .originalNodeExecutionId(originalNodeExecutionUuid)
        .build();
  }
}
