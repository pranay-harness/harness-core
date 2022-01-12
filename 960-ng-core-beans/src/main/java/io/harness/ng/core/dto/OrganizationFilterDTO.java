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
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@OwnedBy(PL)
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(name = "OrganizationFilter",
    description =
        "This has the Organization filter details. This is used to filter Organizations according to the details specified.")
public class OrganizationFilterDTO {
  @Schema(description = OrganizationConstants.SEARCH_TERM) String searchTerm;
  @Schema(description = OrganizationConstants.IDENTIFIER_LIST) List<String> identifiers;
  @Schema(description = OrganizationConstants.IGNORE_CASE) boolean ignoreCase;
}
