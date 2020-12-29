package io.harness.cvng.core.services.impl;

import io.harness.cvng.core.services.api.DeleteEntityByProjectHandler;
import io.harness.persistence.HPersistence;
import io.harness.persistence.PersistentEntity;

import com.google.inject.Inject;

public class DefaultDeleteEntityByProjectHandler<T extends PersistentEntity>
    implements DeleteEntityByProjectHandler<T> {
  @Inject private HPersistence hPersistence;
  @Override
  public void deleteByProjectIdentifier(
      Class<T> clazz, String accountId, String orgIdentifier, String projectIdentifier) {
    hPersistence.delete(hPersistence.createQuery(clazz)
                            .filter("accountId", accountId)
                            .filter("orgIdentifier", orgIdentifier)
                            .filter("projectIdentifier", projectIdentifier));
  }
}
