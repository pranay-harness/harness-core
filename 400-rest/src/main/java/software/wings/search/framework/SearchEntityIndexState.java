/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.search.framework;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.persistence.PersistentEntity;

import lombok.Value;
import lombok.experimental.FieldNameConstants;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

/**
 * The current version
 * of a search entity.
 *
 * @author utkarsh
 */
@OwnedBy(PL)
@Value
@Entity(value = "searchEntitiesIndexState", noClassnameStored = true)
@FieldNameConstants(innerTypeName = "SearchEntityIndexStateKeys")
@Slf4j
public class SearchEntityIndexState implements PersistentEntity {
  @Id private String entityClass;
  private String syncVersion;
  private String indexName;
  private boolean recreateIndex;

  private SearchEntity getSearchEntity() {
    try {
      return (SearchEntity) Class.forName(entityClass).newInstance();
    } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
      log.error("Could not create new instance", e);
    }
    return null;
  }

  boolean shouldBulkSync() {
    SearchEntity searchEntity = getSearchEntity();
    if (searchEntity != null) {
      return !(searchEntity.getVersion().equals(syncVersion)) || recreateIndex;
    }
    return true;
  }
}
