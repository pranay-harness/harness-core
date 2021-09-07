package io.harness.ng.core.entitysetupusage.dto;

import io.harness.ng.core.EntityDetail;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class EntitySetupUsageDTO {
  String accountIdentifier;
  EntityDetail referredEntity;
  @NotNull EntityDetail referredByEntity;
  SetupUsageDetail detail;
  Long createdAt;
}
