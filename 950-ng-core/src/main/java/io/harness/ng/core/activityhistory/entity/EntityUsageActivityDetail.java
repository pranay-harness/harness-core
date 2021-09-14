/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.core.activityhistory.entity;

import io.harness.connector.ConnectivityStatus;
import io.harness.ng.core.EntityDetail;
import io.harness.ng.core.dto.ErrorDetail;

import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.Persistent;
import org.springframework.data.annotation.TypeAlias;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity(value = "entityActivity", noClassnameStored = true)
@Persistent
@TypeAlias("io.harness.ng.core.activity.EntityUsageActivityDetail")
public class EntityUsageActivityDetail extends NGActivity {
  @NotBlank String referredByEntityFQN;
  @NotBlank String referredByEntityType;
  @NotNull EntityDetail referredByEntity;
  @NotEmpty String activityStatusMessage;
  List<ErrorDetail> errors;
  String errorSummary;
  ConnectivityStatus status;
}
