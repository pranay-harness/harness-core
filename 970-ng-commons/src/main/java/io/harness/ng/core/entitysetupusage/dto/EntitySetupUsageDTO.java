/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.entitysetupusage.dto;

import io.harness.ng.core.EntityDetail;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@Schema(name = "EntitySetupUsage", description = "This is the view of the Entity Setup Usage defined in Harness")
public class EntitySetupUsageDTO {
  String accountIdentifier;
  EntityDetail referredEntity;
  @NotNull EntityDetail referredByEntity;
  SetupUsageDetail detail;
  Long createdAt;
}
