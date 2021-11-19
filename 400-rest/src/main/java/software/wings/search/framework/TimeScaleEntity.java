package software.wings.search.framework;

import io.harness.persistence.PersistentEntity;

import java.util.Set;

public interface TimeScaleEntity<T extends PersistentEntity> {
  Class<T> getSourceEntityClass();
  ChangeHandler getChangeHandler();
  boolean toProcessChangeEvent(Set<String> accountIds, PersistentEntity entity);
  boolean runMigration(String accountId);
}
