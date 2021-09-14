/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.mongo.changestreams;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.persistence.PersistentEntity;

import com.mongodb.DBObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

/**
 * The changeTask bean in which all the changes
 * received from ChangeTrackingTask are converted to.
 *
 * @author utkarsh
 */

@OwnedBy(PL)
@Value
@Builder
@AllArgsConstructor
@Slf4j
public class ChangeEvent<T extends PersistentEntity> {
  @NonNull private String token;
  @NonNull private ChangeType changeType;
  @NonNull private Class<T> entityType;
  @NonNull private String uuid;
  private T fullDocument;
  private DBObject changes;

  public boolean isChangeFor(Class<? extends PersistentEntity> entityClass) {
    return this.entityType.isAssignableFrom(entityClass);
  }
}
