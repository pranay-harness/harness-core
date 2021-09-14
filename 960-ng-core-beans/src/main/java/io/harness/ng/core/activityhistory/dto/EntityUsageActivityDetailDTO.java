/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.core.activityhistory.dto;

import io.harness.connector.ConnectivityStatus;
import io.harness.ng.core.EntityDetail;
import io.harness.ng.core.dto.ErrorDetail;

import io.swagger.annotations.ApiModel;
import java.util.List;
import lombok.Builder;
import lombok.Value;
import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.NotEmpty;

@Value
@Builder
@ApiModel("EntityUsageActivityDetail")
public class EntityUsageActivityDetailDTO implements ActivityDetail {
  @NotBlank EntityDetail referredByEntity;
  @NotEmpty String activityStatusMessage;
  List<ErrorDetail> errors;
  String errorSummary;
  ConnectivityStatus status;
}
