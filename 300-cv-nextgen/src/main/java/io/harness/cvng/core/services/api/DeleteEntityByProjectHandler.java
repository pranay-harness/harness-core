package io.harness.cvng.core.services.api;

import io.harness.persistence.PersistentEntity;

public interface DeleteEntityByProjectHandler<T extends PersistentEntity> {
  void deleteByProjectIdentifier(Class<T> clazz, String accountId, String orgIdentifier, String projectIdentifier);
}
