package io.harness.aggregator.consumers;

import io.harness.accesscontrol.AccessControlEntity;
import io.harness.aggregator.OpType;

public interface ChangeConsumer {
  void consumeUpdateEvent(String id, AccessControlEntity persistentEntity);

  void consumeDeleteEvent(String id);

  void consumeCreateEvent(String id, AccessControlEntity accessControlEntity);

  default void consumeEvent(OpType opType, String id, AccessControlEntity accessControlEntity) {
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
