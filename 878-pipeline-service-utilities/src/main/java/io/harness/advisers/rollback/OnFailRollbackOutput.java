/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.advisers.rollback;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.sdk.core.data.ExecutionSweepingOutput;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("onFailRollbackOutput")
@JsonTypeName("onFailRollbackOutput")
@OwnedBy(HarnessTeam.CDC)
@RecasterAlias("io.harness.advisers.rollback.OnFailRollbackOutput")
public class OnFailRollbackOutput implements ExecutionSweepingOutput {
  String nextNodeId;
}
