package software.wings.search.framework;

import io.harness.persistence.PersistentEntity;

public interface SearchEntity<T extends PersistentEntity> {
  String getType();
  String getVersion();
  String getConfigurationPath();
  Class<T> getSourceEntityClass();
  ChangeHandler getChangeHandler();
  EntityBaseView getView(T object);
}
