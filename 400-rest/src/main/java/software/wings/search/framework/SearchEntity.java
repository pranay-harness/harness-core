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

import java.util.List;

/**
 * Search Entity interface each searchEntity
 * has to implement.
 *
 * @author utkarsh
 */
@OwnedBy(PL)
public interface SearchEntity<T extends PersistentEntity> {
  String getType();
  String getVersion();
  String getConfigurationPath();
  Class<T> getSourceEntityClass();
  List<Class<? extends PersistentEntity>> getSubscriptionEntities();
  ChangeHandler getChangeHandler();
  ElasticsearchRequestHandler getElasticsearchRequestHandler();
  EntityBaseView getView(T object);
}
