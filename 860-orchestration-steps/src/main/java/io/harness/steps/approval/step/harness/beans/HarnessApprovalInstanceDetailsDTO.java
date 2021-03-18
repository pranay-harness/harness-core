package io.harness.steps.approval.step.harness.beans;

import io.harness.steps.approval.step.beans.ApprovalInstanceDetailsDTO;
import io.harness.steps.approval.step.harness.entities.HarnessApprovalInstance;

import io.swagger.annotations.ApiModel;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@ApiModel("HarnessApprovalInstanceDetails")
public class HarnessApprovalInstanceDetailsDTO implements ApprovalInstanceDetailsDTO {
  @NotNull ApproversDTO approvers;
  List<HarnessApprovalActivity> approvalActivities;
  List<ApproverInputInfoDTO> approverInputs;

  public static HarnessApprovalInstanceDetailsDTO fromHarnessApprovalInstance(HarnessApprovalInstance instance) {
    if (instance == null) {
      return null;
    }

    return HarnessApprovalInstanceDetailsDTO.builder()
        .approvers(ApproversDTO.fromApprovers(instance.getApprovers()))
        .approvalActivities(instance.getApprovalActivities())
        .approverInputs(instance.getApproverInputs() == null ? null
                                                             : instance.getApproverInputs()
                                                                   .stream()
                                                                   .map(ApproverInputInfoDTO::fromApproverInputInfo)
                                                                   .collect(Collectors.toList()))
        .build();
  }
}
