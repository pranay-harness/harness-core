/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ng.core.dto;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.OrganizationConstants;
import io.harness.annotations.dev.OwnedBy;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@OwnedBy(PL)
@Data
@NoArgsConstructor
@Schema(description = "This has details of the Organization along with its metadata in Harness.")
public class OrganizationResponse {
  @NotNull private OrganizationDTO organization;
  @Schema(description = OrganizationConstants.CREATED_AT) private Long createdAt;
  @Schema(description = OrganizationConstants.LAST_MODIFIED_AT) private Long lastModifiedAt;
  @Schema(description = OrganizationConstants.HARNESS_MANAGED) private boolean harnessManaged;

  @Builder
  public OrganizationResponse(
      OrganizationDTO organization, Long createdAt, Long lastModifiedAt, boolean harnessManaged) {
    this.organization = organization;
    this.createdAt = createdAt;
    this.lastModifiedAt = lastModifiedAt;
    this.harnessManaged = harnessManaged;
  }
}
