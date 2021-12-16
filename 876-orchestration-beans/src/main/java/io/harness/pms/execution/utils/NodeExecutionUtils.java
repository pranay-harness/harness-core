package io.harness.pms.execution.utils;

import io.harness.execution.NodeExecution.NodeExecutionKeys;

import com.google.common.collect.Sets;
import java.util.Set;
import lombok.experimental.UtilityClass;

@UtilityClass
public class NodeExecutionUtils {
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
}
