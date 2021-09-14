/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans.instance.dashboard.service;

import software.wings.beans.appmanifest.ManifestSummary;
import software.wings.beans.instance.dashboard.ArtifactSummary;
import software.wings.beans.instance.dashboard.EntitySummary;

import java.util.Date;
import lombok.Builder;
import lombok.Data;

/**
 * @author rktummala on 08/14/17
 */
@Data
@Builder
public class CurrentActiveInstances {
  private EntitySummary environment;
  private long instanceCount;
  private ArtifactSummary artifact;
  private ManifestSummary manifest;
  private EntitySummary serviceInfra;
  private EntitySummary workflow;
  private Date deployedAt;
  private EntitySummary lastWorkflowExecution;
  private boolean onDemandRollbackAvailable;
}
