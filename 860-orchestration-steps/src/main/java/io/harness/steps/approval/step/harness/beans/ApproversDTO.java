package io.harness.steps.approval.step.harness.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
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

    Object userGroupsObj = approvers.getUserGroups().fetchFinalValue();
    if (userGroupsObj == null) {
      throw new InvalidRequestException("At least 1 user group is required");
    }
    if (userGroupsObj instanceof String) {
      throw new InvalidRequestException(String.format(
          "User groups should be a list of user group identifiers, got a single string value '%s'", userGroupsObj));
    }
    if (!(userGroupsObj instanceof List)) {
      throw new InvalidRequestException(
          String.format("User groups should be a list of user group identifiers, got value %s of type %s",
              userGroupsObj, userGroupsObj.getClass().getSimpleName()));
    }

    List<String> userGroups = (List<String>) userGroupsObj;
    if (EmptyPredicate.isEmpty(userGroups)) {
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
