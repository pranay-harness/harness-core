package software.wings.search.framework.changestreams;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.persistence.PersistentEntity;

@OwnedBy(PL)
@FunctionalInterface
public interface ChangeSubscriber<T extends PersistentEntity> {
  void onChange(ChangeEvent<T> changeEvent);
}
