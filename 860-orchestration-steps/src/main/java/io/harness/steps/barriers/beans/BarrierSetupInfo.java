/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.steps.barriers.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.Set;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;

@OwnedBy(HarnessTeam.PIPELINE)
@Data
@Builder
@FieldNameConstants(innerTypeName = "BarrierSetupInfoKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BarrierSetupInfo {
  String name;
  String identifier;
  Set<StageDetail> stages;
}
