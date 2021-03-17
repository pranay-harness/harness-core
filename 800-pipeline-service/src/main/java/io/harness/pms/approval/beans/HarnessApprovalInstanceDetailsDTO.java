package io.harness.pms.approval.beans;

import io.swagger.annotations.ApiModel;
import java.util.List;
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
}
