/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
