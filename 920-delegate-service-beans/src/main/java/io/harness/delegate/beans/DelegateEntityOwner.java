/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;

@FieldNameConstants(innerTypeName = "DelegateEntityOwnerKeys")
@Data
@Builder
@OwnedBy(HarnessTeam.DEL)
public class DelegateEntityOwner {
  @NotEmpty private String identifier;
}
