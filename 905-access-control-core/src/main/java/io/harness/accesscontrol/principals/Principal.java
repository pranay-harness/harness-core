package io.harness.accesscontrol.principals;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;

@Value
@Builder
@EqualsAndHashCode
public class Principal {
  @NotNull PrincipalType principalType;
  @NotEmpty String principalIdentifier;
}
