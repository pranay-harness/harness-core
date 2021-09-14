/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.search.entities.deployment;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EmbeddedUser;
import io.harness.beans.ExecutionStatus;

import software.wings.beans.EntityType;
import software.wings.search.framework.EntityBaseView;
import software.wings.search.framework.EntityInfo;

import java.util.Set;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

@OwnedBy(PL)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@FieldNameConstants(innerTypeName = "DeploymentViewKeys")
public class DeploymentView extends EntityBaseView {
  private String appId;
  private String appName;
  private ExecutionStatus status;
  private Set<EntityInfo> services;
  private Set<EntityInfo> workflows;
  private Set<EntityInfo> environments;
  private String workflowId;
  private String workflowName;
  private String pipelineId;
  private String pipelineName;
  private boolean workflowInPipeline;

  DeploymentView(String uuid, String name, long createdAt, EmbeddedUser createdBy, String appId, ExecutionStatus status,
      String appName, EntityType type, String accountId) {
    super(uuid, name, null, accountId, createdAt, 0, type, createdBy, null);
    this.status = status;
    this.appId = appId;
    this.appName = appName;
  }
}
