/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.accesscontrol.clients;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(HarnessTeam.PL)
@JsonInclude(Include.NON_NULL)
public class ResourceScope {
  private String accountIdentifier;
  private String orgIdentifier;
  private String projectIdentifier;

  public static ResourceScope NONE = ResourceScope.of(null, null, null);

  public static ResourceScope of(String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    return ResourceScope.builder()
        .accountIdentifier(accountIdentifier)
        .orgIdentifier(orgIdentifier)
        .projectIdentifier(projectIdentifier)
        .build();
  }
}
