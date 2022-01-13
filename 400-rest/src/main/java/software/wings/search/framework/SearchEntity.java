/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
