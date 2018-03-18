package io.harness.idempotence;

import static io.harness.govern.Switch.unhandled;
import static java.time.Duration.ofMillis;

import io.harness.exception.UnableToRegisterIdempotentOperationException;
import io.harness.idempotence.IdempotentRegistry.State;
import io.harness.threading.Morpheus;
import lombok.Builder;

import java.time.Duration;

/*
 * IdempotentLock allows for using try-resource java feature to lock non-idempotent operation and
 * convert it to idempotent.
 */

@Builder
public class IdempotentLock implements AutoCloseable {
  private static Duration pullingInterval = ofMillis(100);
  private IdempotentId id;
  private IdempotentRegistry registry;
  private boolean succeeded;

  public static IdempotentLock create(IdempotentId id, IdempotentRegistry registry)
      throws UnableToRegisterIdempotentOperationException {
    for (;;) {
      final State status = registry.register(id);
      switch (status) {
        case NEW:
          return builder().id(id).registry(registry).build();
        case RUNNING:
          Morpheus.sleep(pullingInterval);
          continue;
        case DONE:
          return null;
        default:
          unhandled(status);
      }
    }
  }

  /*
   * Sets the operation as succeeded.
   */
  public void succeeded() {
    succeeded = true;
  }

  /*
   * Close will register the id as finished if the operation was successful and will unregister it if it was not.
   */
  public void close() {
    if (succeeded) {
      registry.finish(id);
    } else {
      registry.unregister(id);
    }
  }
}
