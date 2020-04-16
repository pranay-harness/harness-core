package io.harness.delegate.task.spotinst.request;

import static com.google.common.collect.Sets.newHashSet;

import io.harness.delegate.task.TaskParameters;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

import java.util.Set;

@Data
@AllArgsConstructor
public class SpotInstTaskParameters implements TaskParameters {
  private static final Set<SpotInstTaskType> SYNC_TASK_TYPES = newHashSet(SpotInstTaskType.SPOT_INST_LIST_ELASTI_GROUPS,
      SpotInstTaskType.SPOT_INST_GET_ELASTI_GROUP_JSON, SpotInstTaskType.SPOT_INST_LIST_ELASTI_GROUP_INSTANCES);

  private String appId;
  private String accountId;
  private String activityId;
  private String commandName;
  private String workflowExecutionId;
  private Integer timeoutIntervalInMin;
  @NotEmpty private SpotInstTaskType commandType;
  @NotEmpty private String awsRegion;

  public enum SpotInstTaskType {
    SPOT_INST_SETUP,
    SPOT_INST_DEPLOY,
    SPOT_INST_SWAP_ROUTES,
    SPOT_INST_LIST_ELASTI_GROUPS,
    SPOT_INST_GET_ELASTI_GROUP_JSON,
    SPOT_INST_LIST_ELASTI_GROUP_INSTANCES,
    SPOT_INST_ALB_SHIFT_SETUP,
    SPOT_INST_ALB_SHIFT_DEPLOY,
    SPOT_INST_ALB_SHIFT_SWAP_ROUTES
  }

  public boolean isSyncTask() {
    return SYNC_TASK_TYPES.contains(commandType);
  }
}
