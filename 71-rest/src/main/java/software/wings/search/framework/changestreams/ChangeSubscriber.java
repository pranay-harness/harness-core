package software.wings.search.framework.changestreams;

import io.harness.persistence.PersistentEntity;

@FunctionalInterface
public interface ChangeSubscriber<T extends PersistentEntity> {
  void onChange(ChangeEvent<T> changeEvent);
}