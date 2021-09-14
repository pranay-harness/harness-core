/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.beans.execution;

import static io.harness.beans.execution.ExecutionSource.Type.MANUAL;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@JsonTypeName("Manual")
@TypeAlias("MANUAL")
@OwnedBy(HarnessTeam.CI)
public class ManualExecutionSource implements ExecutionSource {
  private String branch;
  private String tag;
  private String prNumber;
  private String commitSha;

  @Override
  public Type getType() {
    return MANUAL;
  }
}
