package io.harness.steps.approval.step.harness.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.steps.approval.step.beans.ApprovalInstanceDetailsDTO;

import io.swagger.annotations.ApiModel;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@OwnedBy(CDC)
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@ApiModel("HarnessApprovalInstanceDetails")
@Schema(name = "HarnessApprovalInstanceDetails", description = "This contains details of Harness Approval Instance")
public class HarnessApprovalInstanceDetailsDTO implements ApprovalInstanceDetailsDTO {
  @NotNull String approvalMessage;
  boolean includePipelineExecutionHistory;
  @NotNull ApproversDTO approvers;
  List<HarnessApprovalActivity> approvalActivities;
  List<ApproverInputInfoDTO> approverInputs;
}
