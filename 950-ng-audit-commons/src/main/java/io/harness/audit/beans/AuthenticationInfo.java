package io.harness.audit.beans;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.common.beans.KeyValuePair;

import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import lombok.experimental.FieldNameConstants;

@OwnedBy(PL)
@Data
@Builder
@FieldNameConstants(innerTypeName = "AuthenticationInfoKeys")
public class AuthenticationInfo {
  @NotNull @Valid Principal principal;
  @Singular List<KeyValuePair> labels;
}
