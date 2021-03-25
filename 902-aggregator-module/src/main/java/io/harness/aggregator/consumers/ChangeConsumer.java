package io.harness.aggregator.consumers;

import io.harness.accesscontrol.AccessControlEntity;
import io.harness.aggregator.OpType;

public interface ChangeConsumer<T extends AccessControlEntity> {
  long consumeUpdateEvent(String id, T persistentEntity);

  long consumeDeleteEvent(String id);

  long consumeCreateEvent(String id, T accessControlEntity);

  default void consumeEvent(OpType opType, String id, T accessControlEntity) {
    switch (opType) {
      case SNAPSHOT:
      case CREATE:
        consumeCreateEvent(id, accessControlEntity);
        break;
      case UPDATE:
        consumeUpdateEvent(id, accessControlEntity);
        break;
      case DELETE:
        consumeDeleteEvent(id);
        break;
      default:
        throw new UnsupportedOperationException("Operation type " + opType + " not supported");
    }
  }
}
