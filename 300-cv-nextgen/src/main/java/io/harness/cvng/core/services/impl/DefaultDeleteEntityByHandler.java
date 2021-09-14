/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.core.services.impl;

import io.harness.cvng.core.services.api.DeleteEntityByHandler;
import io.harness.persistence.HPersistence;
import io.harness.persistence.PersistentEntity;

import com.google.inject.Inject;

public class DefaultDeleteEntityByHandler<T extends PersistentEntity> implements DeleteEntityByHandler<T> {
  @Inject private HPersistence hPersistence;
  @Override
  public void deleteByProjectIdentifier(
      Class<T> clazz, String accountId, String orgIdentifier, String projectIdentifier) {
    hPersistence.delete(hPersistence.createQuery(clazz)
                            .filter("accountId", accountId)
                            .filter("orgIdentifier", orgIdentifier)
                            .filter("projectIdentifier", projectIdentifier));
  }

  @Override
  public void deleteByOrgIdentifier(Class<T> clazz, String accountId, String orgIdentifier) {
    hPersistence.delete(
        hPersistence.createQuery(clazz).filter("accountId", accountId).filter("orgIdentifier", orgIdentifier));
  }

  @Override
  public void deleteByAccountIdentifier(Class<T> clazz, String accountId) {
    hPersistence.delete(hPersistence.createQuery(clazz).filter("accountId", accountId));
  }
}
