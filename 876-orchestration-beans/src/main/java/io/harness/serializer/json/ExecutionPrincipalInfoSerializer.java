/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.serializer.json;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.plan.ExecutionPrincipalInfo;

@OwnedBy(PIPELINE)
public class ExecutionPrincipalInfoSerializer extends ProtoJsonSerializer<ExecutionPrincipalInfo> {
  public ExecutionPrincipalInfoSerializer() {
    super(ExecutionPrincipalInfo.class);
  }
}
