/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.engine.utils;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.plan.Node;
import io.harness.pms.contracts.ambiance.Level;

@OwnedBy(HarnessTeam.PIPELINE)
public class PmsLevelUtils {
  public static Level buildLevelFromNode(String runtimeId, Node node) {
    return buildLevelFromNode(runtimeId, 0, node);
  }

  public static Level buildLevelFromNode(String runtimeId, int retryIndex, Node node) {
    Level.Builder levelBuilder = Level.newBuilder()
                                     .setSetupId(node.getUuid())
                                     .setRuntimeId(runtimeId)
                                     .setIdentifier(node.getIdentifier())
                                     .setRetryIndex(retryIndex)
                                     .setSkipExpressionChain(node.isSkipExpressionChain())
                                     .setStartTs(System.currentTimeMillis())
                                     .setStepType(node.getStepType())
                                     .setNodeType(node.getNodeType().toString());
    if (node.getGroup() != null) {
      levelBuilder.setGroup(node.getGroup());
    }
    return levelBuilder.build();
  }
}
