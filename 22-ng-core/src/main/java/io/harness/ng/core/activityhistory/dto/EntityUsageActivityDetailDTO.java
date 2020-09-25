package io.harness.ng.core.activityhistory.dto;

import io.harness.EntityType;
import io.harness.encryption.Scope;
import io.swagger.annotations.ApiModel;
import lombok.Builder;
import lombok.Value;
import org.hibernate.validator.constraints.NotBlank;

import javax.validation.constraints.NotNull;

@Value
@Builder
@ApiModel("EntityUsageActivityDetail")
public class EntityUsageActivityDetailDTO implements ActivityDetail {
  String referredByEntityOrgIdentifier;
  String referredByEntityProjectIdentifier;
  @NotBlank String referredByEntityIdentifier;
  @NotNull Scope referredByEntityScope;
  @NotBlank EntityType referredByEntityType;
}
