/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans.instance.dashboard.service;

import software.wings.beans.appmanifest.ManifestSummary;
import software.wings.beans.instance.dashboard.ArtifactSummary;
import software.wings.beans.instance.dashboard.EntitySummary;

import java.util.Date;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * @author rktummala on 08/14/17
 */
@Data
@Builder
public class DeploymentHistory {
  private ArtifactSummary artifact;
  private ManifestSummary manifest;
  private Date deployedAt;
  private String status;
  private EntitySummary triggeredBy;
  private EntitySummary pipeline;
  private EntitySummary workflow;
  private long instanceCount;
  private List<EntitySummary> inframappings;
  private List<EntitySummary> envs;
  private boolean rolledBack;
}
