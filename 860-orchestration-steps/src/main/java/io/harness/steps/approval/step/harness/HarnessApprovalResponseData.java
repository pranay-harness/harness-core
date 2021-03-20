package io.harness.steps.approval.step.harness;

import io.harness.tasks.ResponseData;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class HarnessApprovalResponseData implements ResponseData {
  String approvalInstanceId;
}
