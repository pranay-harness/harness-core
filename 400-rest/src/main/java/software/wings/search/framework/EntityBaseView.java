/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.search.framework;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EmbeddedUser;

import software.wings.beans.EntityType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

/**
 * The base entity view which can be
 * used by search entities.
 *
 * @author utkarsh
 */
@OwnedBy(PL)
@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants(innerTypeName = "EntityBaseViewKeys")
public class EntityBaseView {
  private String id;
  private String name;
  private String description;
  private String accountId;
  private long createdAt;
  private long lastUpdatedAt;
  private EntityType type;
  private EmbeddedUser createdBy;
  private EmbeddedUser lastUpdatedBy;
}
