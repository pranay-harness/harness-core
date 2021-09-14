/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.nexus.model;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;

@lombok.Data
@Builder
@OwnedBy(HarnessTeam.CDC)
public class Nexus3RequestData {
  private String repositoryName;
  private String node;
}
