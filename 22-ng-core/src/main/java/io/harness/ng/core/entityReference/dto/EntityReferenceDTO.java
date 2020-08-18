package io.harness.ng.core.entityReference.dto;

import io.harness.ng.core.entityReference.ReferenceEntityType;
import lombok.Builder;
import lombok.Value;
import org.hibernate.validator.constraints.NotBlank;

import javax.validation.constraints.NotNull;

@Value
@Builder
public class EntityReferenceDTO {
  String accountIdentifier;
  @NotBlank String referredEntityFQN;
  String referredEntityName;
  @NotNull ReferenceEntityType referredEntityType;
  @NotBlank String referredByEntityFQN;
  @NotNull ReferenceEntityType referredByEntityType;
  String referredByEntityName;
  // todo @deepak: Add the support for setup usage
  Long createdAt;
}
