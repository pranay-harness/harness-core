/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.beans;

import static io.harness.annotations.dev.HarnessTeam.PL;

import static lombok.AccessLevel.PRIVATE;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(PL)
@Data
@Builder
@FieldDefaults(level = PRIVATE, makeFinal = true)
@FieldNameConstants(innerTypeName = "ScopeKeys")
public class Scope {
  @NotEmpty String accountIdentifier;
  String orgIdentifier;
  String projectIdentifier;

  public static Scope of(String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    return Scope.builder()
        .accountIdentifier(accountIdentifier)
        .orgIdentifier(orgIdentifier)
        .projectIdentifier(projectIdentifier)
        .build();
  }
}
