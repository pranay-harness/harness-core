package io.harness.ng.core.activityhistory.dto;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.EntityType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.encryption.Scope;
import io.harness.ng.core.activityhistory.NGActivityStatus;
import io.harness.ng.core.activityhistory.NGActivityType;
import io.swagger.annotations.ApiModel;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.NotBlank;

import javax.validation.constraints.NotNull;

@Value
@Builder
@OwnedBy(DX)
@FieldDefaults(level = AccessLevel.PRIVATE)
@ApiModel("Activity")
public class NGActivityDTO {
  @NotBlank String accountIdentifier;
  String referredEntityOrgIdentifier;
  String referredEntityProjectIdentifier;
  @NotBlank String referredEntityIdentifier;
  @NotNull Scope referredEntityScope;
  @NotNull EntityType referredEntityType;
  @NotNull NGActivityType type;
  @NotNull NGActivityStatus activityStatus;
  ActivityDetail detail;
  @NotNull long activityTime;
  @NotBlank String description;
  String errorMessage;
}