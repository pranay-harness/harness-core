package io.harness.audit.beans;

import io.harness.ng.core.common.beans.KeyValuePair;
import io.harness.security.dto.Principal;

import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import lombok.experimental.FieldNameConstants;

@Data
@Builder
@FieldNameConstants(innerTypeName = "AuthenticationInfoKeys")
public class AuthenticationInfo {
  @NotNull Principal principal;
  @Singular List<KeyValuePair> labels;
}
