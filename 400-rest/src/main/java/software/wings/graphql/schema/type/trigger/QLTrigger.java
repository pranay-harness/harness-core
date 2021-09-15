/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.graphql.schema.type.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.QLObject;
import software.wings.graphql.schema.type.QLUser;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@OwnedBy(CDC)
@Value
@Builder
@Scope(ResourceType.APPLICATION)
@FieldNameConstants(innerTypeName = "QLTriggerKeys")
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLTrigger implements QLObject {
  private String id;
  private String name;
  private String description;
  private QLTriggerCondition condition;
  private QLTriggerAction action;
  private Long createdAt;
  private QLUser createdBy;
  private Boolean excludeHostsWithSameArtifact;
}
