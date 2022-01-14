package io.harness.plan;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

/**
 * This represents node type in a plan
 * A node can be of the type
 *  PLAN -- Complete plan is a node
 *  PLAN_NODE -- A sigle node
 *
 *  More node types can be introduced and on the basis of that execution engine can make certain execution choices
 *  RESUMEABLE_PLAN_NODE
 *
 */
@OwnedBy(HarnessTeam.PIPELINE) public enum NodeType { PLAN, PLAN_NODE, IDENTITY_PLAN_NODE }
