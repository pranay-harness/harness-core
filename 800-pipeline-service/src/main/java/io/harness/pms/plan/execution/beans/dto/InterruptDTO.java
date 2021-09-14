/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.plan.execution.beans.dto;

import io.harness.pms.plan.execution.PlanExecutionInterruptType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel("pipelineExecutionInterrupt")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InterruptDTO {
  String id;
  @NonNull PlanExecutionInterruptType type;
  @NonNull String planExecutionId;
}
