package io.harness.ng.core.entitysetupusage.dto;

import io.harness.ng.core.EntityDetail;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@Schema(name = "EntitySetupUsage", description = "This is the view of the Entity Setup Usage defined in Harness")
public class EntitySetupUsageDTO {
  String accountIdentifier;
  EntityDetail referredEntity;
  @NotNull EntityDetail referredByEntity;
  SetupUsageDetail detail;
  Long createdAt;
}
