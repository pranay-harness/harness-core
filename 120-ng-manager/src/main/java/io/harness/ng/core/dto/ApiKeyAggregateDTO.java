/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.dto;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(HarnessTeam.PL)
@Schema(name = "ApiKeyAggregate", description = "This has API Key details and metadata.")
public class ApiKeyAggregateDTO {
  @NotNull private ApiKeyDTO apiKey;
  @NotNull @Schema(description = "This is the time at which API Key was created.") private Long createdAt;
  @NotNull @Schema(description = "This is the time at which API Key was last modified.") private Long lastModifiedAt;
  @Schema(description = "The number of tokens within an API Key.") private int tokensCount;
}
