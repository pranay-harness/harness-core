package io.harness.steps.approval.step.harness.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.HasPredicate.hasNone;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;

import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@OwnedBy(CDC)
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ApproversDTO {
  List<String> userGroups;
  int minimumCount;
  boolean disallowPipelineExecutor;

  public static ApproversDTO fromApprovers(Approvers approvers) {
    if (approvers == null) {
      return null;
    }

    List<String> userGroups = (List<String>) approvers.getUserGroups().fetchFinalValue();
    if (hasNone(userGroups)) {
      throw new InvalidRequestException("At least 1 user group is required");
    }

    int minimumCount = (int) approvers.getMinimumCount().fetchFinalValue();
    if (minimumCount < 1) {
      throw new InvalidRequestException("Minimum count should be > 0");
    }

    return ApproversDTO.builder()
        .userGroups(userGroups)
        .minimumCount(minimumCount)
        .disallowPipelineExecutor((Boolean) approvers.getDisallowPipelineExecutor().fetchFinalValue())
        .build();
  }
}
