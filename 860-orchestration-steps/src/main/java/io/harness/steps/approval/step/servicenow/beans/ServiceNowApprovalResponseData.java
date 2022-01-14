package io.harness.steps.approval.step.servicenow.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.tasks.ResponseData;

import lombok.Builder;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder
public class ServiceNowApprovalResponseData implements ResponseData {
  String instanceId;
}
