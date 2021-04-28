package io.harness.aggregator.consumers;

import io.harness.accesscontrol.AccessControlEntity;
import io.harness.aggregator.OpType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.PL)
public interface ChangeConsumer<T extends AccessControlEntity> {
  long consumeUpdateEvent(String id, T updatedEntity);

  long consumeDeleteEvent(String id);

  long consumeCreateEvent(String id, T createdEntity);

  default void consumeEvent(OpType opType, String id, T entity) {
    switch (opType) {
      case SNAPSHOT:
      case CREATE:
        consumeCreateEvent(id, entity);
        break;
      case UPDATE:
        consumeUpdateEvent(id, entity);
        break;
      case DELETE:
        consumeDeleteEvent(id);
        break;
      default:
        throw new UnsupportedOperationException("Operation type " + opType + " not supported");
    }
  }
}
