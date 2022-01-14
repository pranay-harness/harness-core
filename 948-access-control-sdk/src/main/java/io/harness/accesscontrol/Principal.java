package io.harness.accesscontrol;

import io.harness.accesscontrol.principals.PrincipalType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(HarnessTeam.PL)
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@FieldNameConstants(innerTypeName = "PrincipalKeys")
@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel(value = "Principal")
public class Principal {
  @NotEmpty String principalIdentifier;
  @NotNull PrincipalType principalType;

  public static Principal of(PrincipalType principalType, String principalIdentifier) {
    return Principal.builder().principalIdentifier(principalIdentifier).principalType(principalType).build();
  }
}
