package io.harness.audit.beans;

import io.harness.security.dto.Principal;

import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

@Data
@Builder
@FieldNameConstants(innerTypeName = "AuthenticationInfoKeys")
public class AuthenticationInfo {
  @NotNull Principal principal;
  Map<String, String> labels;
}
