/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans.instance.dashboard;

import software.wings.beans.infrastructure.instance.SyncStatus;

import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * @author rktummala on 08/13/17
 */
@Data
@Builder
public class InstanceStatsByEnvironment {
  private EnvironmentSummary environmentSummary;
  private List<InstanceStatsByArtifact> instanceStatsByArtifactList;
  private List<SyncStatus> infraMappingSyncStatusList;
  private boolean hasSyncIssues;
}
