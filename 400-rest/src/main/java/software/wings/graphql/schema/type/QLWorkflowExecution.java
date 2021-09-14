/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.graphql.schema.type;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.aggregation.deployment.QLDeploymentTag;
import software.wings.graphql.schema.type.artifact.QLArtifact;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;

import java.util.List;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@OwnedBy(CDC)
@Value
@Builder
@FieldNameConstants(innerTypeName = "QLWorkflowExecutionKeys")
@Scope(ResourceType.APPLICATION)
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLWorkflowExecution implements QLExecution {
  private String id;
  private String workflowId;
  private Long createdAt;
  private Long startedAt;
  private Long endedAt;
  private QLExecutionStatus status;
  private List<QLArtifact> artifacts;
  private List<QLArtifact> rollbackArtifacts;
  private QLCause cause;
  private String notes;
  private String appId;
  private List<QLDeploymentTag> tags;
  private String failureDetails;
}
