package io.harness.ng.core.activityhistory.dto;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.EntityDetail;
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
  EntityDetail referredEntity;
  @NotNull NGActivityType type;
  @NotNull NGActivityStatus activityStatus;
  ActivityDetail detail;
  @NotNull long activityTime;
  @NotBlank String description;
  String errorMessage;
}
