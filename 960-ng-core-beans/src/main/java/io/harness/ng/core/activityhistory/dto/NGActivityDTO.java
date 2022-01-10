/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(name = "NGActivity", description = "This is the view of the NGActivity entity defined in Harness")
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
