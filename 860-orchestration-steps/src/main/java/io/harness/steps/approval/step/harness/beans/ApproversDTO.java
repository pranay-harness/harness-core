package io.harness.steps.approval.step.harness.beans;

import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ApproversDTO {
  List<String> userGroups;
  List<String> users;
  int minimumCount;
  boolean disallowPipelineExecutor;

  public static ApproversDTO fromApprovers(Approvers approvers) {
    if (approvers == null) {
      return null;
    }

    return ApproversDTO.builder()
        .userGroups((List<String>) approvers.getUserGroups().fetchFinalValue())
        .users((List<String>) approvers.getUsers().fetchFinalValue())
        .minimumCount((Integer) approvers.getMinimumCount().fetchFinalValue())
        .disallowPipelineExecutor((Boolean) approvers.getDisallowPipelineExecutor().fetchFinalValue())
        .build();
  }
}
