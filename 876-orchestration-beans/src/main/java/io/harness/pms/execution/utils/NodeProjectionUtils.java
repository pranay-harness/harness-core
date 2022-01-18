/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.pms.execution.utils;

import io.harness.execution.NodeExecution.NodeExecutionKeys;

import com.google.common.collect.Sets;
import java.util.Set;
import lombok.experimental.UtilityClass;

@UtilityClass
public class NodeProjectionUtils {
  public static final Set<String> fieldsForRetryInterruptHandler = Sets.newHashSet(
      NodeExecutionKeys.ambiance, NodeExecutionKeys.status, NodeExecutionKeys.oldRetry, NodeExecutionKeys.mode);
  public static final Set<String> withAmbianceAndStatus =
      Sets.newHashSet(NodeExecutionKeys.ambiance, NodeExecutionKeys.status);
  public static final Set<String> withAmbiance = Sets.newHashSet(NodeExecutionKeys.ambiance);
  public static final Set<String> withStatus = Sets.newHashSet(NodeExecutionKeys.status);
  public static final Set<String> withStatusAndMode = Sets.newHashSet(NodeExecutionKeys.status, NodeExecutionKeys.mode);
  public static final Set<String> withStatusAndNode =
      Sets.newHashSet(NodeExecutionKeys.status, NodeExecutionKeys.planNode, NodeExecutionKeys.node);
  public static final Set<String> withStatusAndAdviserResponse =
      Sets.newHashSet(NodeExecutionKeys.status, NodeExecutionKeys.adviserResponse);
  public static final Set<String> fieldsForNodeUpdateObserver =
      Sets.newHashSet(NodeExecutionKeys.ambiance, NodeExecutionKeys.status, NodeExecutionKeys.planNode,
          NodeExecutionKeys.node, NodeExecutionKeys.endTs, NodeExecutionKeys.oldRetry);
  public static final Set<String> withAmbianceAndNode =
      Sets.newHashSet(NodeExecutionKeys.ambiance, NodeExecutionKeys.planNode, NodeExecutionKeys.node);
  public static final Set<String> fieldsForResume = Sets.newHashSet(NodeExecutionKeys.status,
      NodeExecutionKeys.ambiance, NodeExecutionKeys.planNode, NodeExecutionKeys.node,
      NodeExecutionKeys.executableResponses, NodeExecutionKeys.resolvedStepParameters, NodeExecutionKeys.mode);
  public static final Set<String> fieldsForExpressionEngine = Sets.newHashSet(NodeExecutionKeys.status,
      NodeExecutionKeys.ambiance, NodeExecutionKeys.planNode, NodeExecutionKeys.node,
      NodeExecutionKeys.resolvedStepParameters, NodeExecutionKeys.mode, NodeExecutionKeys.startTs,
      NodeExecutionKeys.endTs, NodeExecutionKeys.resolvedStepParameters, NodeExecutionKeys.parentId);
}
