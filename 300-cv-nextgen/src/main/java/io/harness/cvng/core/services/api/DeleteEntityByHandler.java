package io.harness.cvng.core.services.api;

import io.harness.persistence.PersistentEntity;

public interface DeleteEntityByHandler<T extends PersistentEntity> {
  void deleteByProjectIdentifier(Class<T> clazz, String accountId, String orgIdentifier, String projectIdentifier);

  void deleteByOrgIdentifier(Class<T> clazz, String accountId, String orgIdentifier);

  void deleteByAccountIdentifier(Class<T> clazz, String accountId);
}
