/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.steps.approval.step.harness.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(CDC)
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ApproverInputInfoDTO {
  @NotEmpty String name;
  String defaultValue;

  public static ApproverInputInfoDTO fromApproverInputInfo(ApproverInputInfo approverInput) {
    if (approverInput == null) {
      return null;
    }

    return ApproverInputInfoDTO.builder()
        .name(approverInput.getName())
        .defaultValue((String) approverInput.getDefaultValue().fetchFinalValue())
        .build();
  }
}
