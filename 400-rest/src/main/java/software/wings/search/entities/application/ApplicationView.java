/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.search.entities.application;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EmbeddedUser;

import software.wings.beans.EntityType;
import software.wings.search.entities.related.audit.RelatedAuditView;
import software.wings.search.framework.EntityBaseView;
import software.wings.search.framework.EntityInfo;

import java.util.List;
import java.util.Set;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

@OwnedBy(PL)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@FieldNameConstants(innerTypeName = "ApplicationViewKeys")
public class ApplicationView extends EntityBaseView {
  private Set<EntityInfo> services;
  private Set<EntityInfo> environments;
  private Set<EntityInfo> workflows;
  private Set<EntityInfo> pipelines;
  private List<RelatedAuditView> audits;
  private List<Long> auditTimestamps;

  ApplicationView(String uuid, String name, String description, String accountId, long createdAt, long lastUpdatedAt,
      EntityType entityType, EmbeddedUser createdBy, EmbeddedUser lastUpdatedBy) {
    super(uuid, name, description, accountId, createdAt, lastUpdatedAt, entityType, createdBy, lastUpdatedBy);
  }
}
