package io.harness.audit.beans;

import static io.harness.annotations.dev.HarnessTeam.PL;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

@OwnedBy(PL)
@Data
@Builder
@JsonInclude(NON_NULL)
@FieldNameConstants(innerTypeName = "AuthenticationInfoKeys")
public class AuthenticationInfo {
  @NotNull @Valid Principal principal;
  Map<String, String> labels;
}
