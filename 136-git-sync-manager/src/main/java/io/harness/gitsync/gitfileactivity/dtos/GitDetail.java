/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.gitsync.gitfileactivity.dtos;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.EntityType;

import lombok.Builder;
import lombok.Data;
import org.mongodb.morphia.annotations.Transient;

@Data
@Builder
@OwnedBy(DX)
public class GitDetail {
  private String entityName;
  private EntityType entityType;
  private String repositoryUrl;
  private String branchName;
  private String yamlGitConfigId;
  private String gitConnectorId;
  private String appId;
  private String gitCommitId;
  @Transient String connectorName;
}
