/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.globalcontex;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.context.GlobalContextData;

import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(PL)
@Value
@Builder
@TypeAlias("PurgeGlobalContextData")
public class PurgeGlobalContextData implements GlobalContextData {
  public static final String PURGE_OP = "PURGE_OP";

  @Override
  public String getKey() {
    return PURGE_OP;
  }
}
