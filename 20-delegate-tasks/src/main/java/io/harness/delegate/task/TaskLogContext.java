package io.harness.delegate.task;

import com.google.common.collect.ImmutableMap;

import io.harness.data.structure.HarnessStringUtils;
import io.harness.delegate.beans.DelegateTaskRank;
import io.harness.logging.AutoLogContext;

import java.util.List;

public class TaskLogContext extends AutoLogContext {
  public TaskLogContext(String taskId, OverrideBehavior behavior) {
    super("taskId", taskId, behavior);
  }

  public TaskLogContext(String taskId, DelegateTaskRank rank, OverrideBehavior behavior) {
    super(ImmutableMap.of("taskId", taskId, "rank", rank == null ? DelegateTaskRank.CRITICAL.name() : rank.name()),
        behavior);
  }

  public TaskLogContext(String taskId, String taskType, String taskGroup, OverrideBehavior behavior) {
    super(ImmutableMap.of("taskId", taskId, "taskType", taskType, "taskGroup", taskGroup), behavior);
  }

  public TaskLogContext(
      String taskId, String taskType, String taskGroup, DelegateTaskRank rank, OverrideBehavior behavior) {
    super(ImmutableMap.of("taskId", taskId, "taskType", taskType, "taskGroup", taskGroup, "rank",
              rank == null ? DelegateTaskRank.CRITICAL.name() : rank.name()),
        behavior);
  }

  public TaskLogContext(String taskId, String taskType, List<String> capabilityDetails, OverrideBehavior behavior) {
    super(ImmutableMap.of("taskId", taskId, "taskType", taskType, "capabilityDetails",
              HarnessStringUtils.join("|", capabilityDetails)),
        behavior);
  }
}
