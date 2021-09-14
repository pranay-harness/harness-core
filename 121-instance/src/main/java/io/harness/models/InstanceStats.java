/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.models;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
@OwnedBy(HarnessTeam.DX)
public class InstanceStats {
  private String accountId;
  private String orgId;
  private String projectId;
  private String serviceId;
  private String envId;
  private String cloudProviderId;
  private String artifactId;
  private Timestamp reportedAt;
}
