/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.audit;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import software.wings.jersey.JsonViews;

import com.fasterxml.jackson.annotation.JsonView;
import lombok.Builder;
import lombok.Data;

@OwnedBy(PL)
@Data
@Builder
public class EntityAuditRecord {
  // Details of the entity being modified
  private String entityId;
  private String entityType;
  private String entityName;
  private String operationType;
  @JsonView(JsonViews.Internal.class) private String entityOldYamlRecordId;
  @JsonView(JsonViews.Internal.class) private String entityNewYamlRecordId;
  private String yamlPath;
  private String yamlError;
  private boolean failure;

  // Details of the affected application.
  // May be NULL for account level entities.
  // The application column is always there.
  // Hence maintained separately.
  private String appId;
  private String appName;

  // Details of the affected resource.
  // Mostly, this could be Service / Environment.
  // Added separately to make indexing and
  // UI aggregation easier.
  private String affectedResourceId;
  private String affectedResourceName;
  private String affectedResourceType;
  private String affectedResourceOperation;
  private long createdAt;
}
