package io.harness.delegate.task;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.data.structure.HarnessStringUtils;
import io.harness.delegate.beans.DelegateTaskRank;
import io.harness.logging.AutoLogContext;

import com.google.common.collect.ImmutableMap;
import java.util.List;

@TargetModule(HarnessModule._920_DELEGATE_AGENT_BEANS)
public class TaskLogContext extends AutoLogContext {
  // keys
  private static final String TASK_ID = "taskId";
  private static final String TASK_TYPE = "taskType";
  private static final String RANK = "rank";
  private static final String TASK_GROUP = "taskGroup";
  private static final String CAPABILITY_DETAILS = "capabilityDetails";

  public TaskLogContext(String taskId, OverrideBehavior behavior) {
    super(TASK_ID, taskId, behavior);
  }

  public TaskLogContext(String taskId, DelegateTaskRank rank, OverrideBehavior behavior) {
    super(ImmutableMap.of(TASK_ID, taskId, RANK, rank == null ? DelegateTaskRank.CRITICAL.name() : rank.name()),
        behavior);
  }

  public TaskLogContext(String taskId, String taskType, String taskGroup, OverrideBehavior behavior) {
    super(ImmutableMap.of(TASK_ID, taskId, TASK_TYPE, taskType, TASK_GROUP, taskGroup), behavior);
  }

  public TaskLogContext(
      String taskId, String taskType, String taskGroup, DelegateTaskRank rank, OverrideBehavior behavior) {
    super(ImmutableMap.of(TASK_ID, taskId, TASK_TYPE, taskType, TASK_GROUP, taskGroup, RANK,
              rank == null ? DelegateTaskRank.CRITICAL.name() : rank.name()),
        behavior);
  }

  public TaskLogContext(String taskId, String taskType, List<String> capabilityDetails, OverrideBehavior behavior) {
    super(ImmutableMap.of(TASK_ID, taskId, TASK_TYPE, taskType, CAPABILITY_DETAILS,
              HarnessStringUtils.join("|", capabilityDetails)),
        behavior);
  }

  public TaskLogContext(String taskId, String taskType, List<String> capabilityDetails, DelegateTaskRank rank,
      OverrideBehavior behavior) {
    super(ImmutableMap.of(TASK_ID, taskId, TASK_TYPE, taskType, CAPABILITY_DETAILS,
              HarnessStringUtils.join("|", capabilityDetails), RANK,
              rank == null ? DelegateTaskRank.CRITICAL.name() : rank.name()),
        behavior);
  }
}
