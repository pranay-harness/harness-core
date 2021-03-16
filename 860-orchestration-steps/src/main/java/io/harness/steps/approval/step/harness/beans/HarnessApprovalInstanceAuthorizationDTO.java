package io.harness.steps.approval.step.harness.beans;

import io.swagger.annotations.ApiModel;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@ApiModel("HarnessApprovalInstanceAuthorization")
public class HarnessApprovalInstanceAuthorizationDTO {
  boolean authorized;
}
