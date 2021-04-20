package io.harness.accesscontrol.principals;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(HarnessTeam.PL)
@Value
@Builder
@EqualsAndHashCode
public class Principal {
  @NotNull PrincipalType principalType;
  @NotEmpty String principalIdentifier;
}
