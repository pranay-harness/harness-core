package io.harness.pms.approval.beans;

import io.swagger.annotations.ApiModel;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@ApiModel("ApprovalInstanceAuthorization")
public class ApprovalInstanceAuthorizationDTO {
  boolean authorized;
}
