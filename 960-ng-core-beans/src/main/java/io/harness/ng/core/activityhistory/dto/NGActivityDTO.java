/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.core.activityhistory.dto;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.EntityDetail;
import io.harness.ng.core.activityhistory.NGActivityStatus;
import io.harness.ng.core.activityhistory.NGActivityType;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.annotations.ApiModel;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.NotBlank;

@Data
@NoArgsConstructor
@OwnedBy(DX)
@FieldDefaults(level = AccessLevel.PRIVATE)
@ApiModel("Activity")
public class NGActivityDTO {
  @NotBlank String accountIdentifier;
  EntityDetail referredEntity;
  @NotNull NGActivityType type;
  @NotNull NGActivityStatus activityStatus;
  @JsonTypeInfo(
      use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXTERNAL_PROPERTY, visible = true)
  @JsonSubTypes({
    @JsonSubTypes.Type(value = ConnectivityCheckActivityDetailDTO.class, name = "CONNECTIVITY_CHECK")
    , @JsonSubTypes.Type(value = EntityUsageActivityDetailDTO.class, name = "ENTITY_USAGE")
  })
  ActivityDetail detail;
  @NotNull long activityTime;
  @NotBlank String description;

  @Builder
  public NGActivityDTO(String accountIdentifier, EntityDetail referredEntity, NGActivityType type,
      NGActivityStatus activityStatus, ActivityDetail detail, long activityTime, String description) {
    this.accountIdentifier = accountIdentifier;
    this.referredEntity = referredEntity;
    this.type = type;
    this.activityStatus = activityStatus;
    this.detail = detail;
    this.description = description;
    this.activityTime = activityTime;
  }
}
