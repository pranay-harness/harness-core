/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Data;

/**
 * Created by sgurubelli on 11/20/17.
 */
@Data
@Builder
@OwnedBy(HarnessTeam.CDC)
public class BuildExecutionSummary {
  String artifactStreamId;
  String artifactSource;
  String revision;
  String buildUrl;
  String buildName;
  String metadata;
}
