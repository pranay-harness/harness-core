package io.harness.ng.core.entitysetupusage.dto;

import io.harness.ng.core.EntityDetail;

import lombok.Builder;
import lombok.Value;
import org.hibernate.validator.constraints.NotBlank;

@Value
@Builder
public class EntitySetupUsageDTO {
  String accountIdentifier;
  @NotBlank EntityDetail referredEntity;
  @NotBlank EntityDetail referredByEntity;
  // todo @deepak: Add the support for setup usage
  Long createdAt;
}
