/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.changestreamsframework;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.persistence.PersistentEntity;

import com.mongodb.DBObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Value
@Builder
@AllArgsConstructor
@Slf4j
@OwnedBy(HarnessTeam.CE)
public class ChangeEvent<T extends PersistentEntity> {
  private String token;
  private Class<T> entityType;
  @NonNull private ChangeType changeType;
  @NonNull private String uuid;
  private DBObject fullDocument;
  private DBObject changes;

  public boolean isChangeFor(Class<? extends PersistentEntity> entityClass) {
    return this.entityType.isAssignableFrom(entityClass);
  }
}
