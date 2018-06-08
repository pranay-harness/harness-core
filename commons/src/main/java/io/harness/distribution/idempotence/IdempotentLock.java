package io.harness.distribution.idempotence;

import static io.harness.govern.Switch.unhandled;
import static java.lang.String.format;
import static java.time.Duration.ofMillis;
import static java.time.Duration.ofMinutes;

import io.harness.threading.Morpheus;
import lombok.Builder;

import java.time.Duration;
import java.util.Optional;

/*
 * IdempotentLock allows for using try-resource java feature to lock non-idempotent operation and
 * convert it to idempotent.
 */

@Builder
public class IdempotentLock<T> implements AutoCloseable {
  private static Duration defaultPollingInterval = ofMillis(100);
  private static Duration defaultTimeout = ofMinutes(3);
  private IdempotentId id;
  private IdempotentRegistry registry;
  private Optional<T> resultData;

  public static IdempotentLock create(IdempotentId id, IdempotentRegistry registry)
      throws UnableToRegisterIdempotentOperationException {
    return create(id, registry, defaultTimeout, defaultPollingInterval);
  }

  public static IdempotentLock create(IdempotentId id, IdempotentRegistry registry, Duration timeout,
      Duration pollingInterval) throws UnableToRegisterIdempotentOperationException {
    long systemTimeMillis = System.currentTimeMillis();

    do {
      IdempotentRegistry.Response response = registry.register(id);
      switch (response.getState()) {
        case NEW:
          return builder().id(id).registry(registry).resultData(Optional.empty()).build();
        case RUNNING:
          Morpheus.sleep(pollingInterval);
          continue;
        case DONE:
          return builder().id(id).resultData(Optional.of(response.getResult())).build();
        default:
          unhandled(response.getState());
      }
    } while (System.currentTimeMillis() - systemTimeMillis < timeout.toMillis());

    throw new UnableToRegisterIdempotentOperationException(format(
        "Acquiring idempotent lock for operation %s timed out after %d seconds", id.getValue(), timeout.getSeconds()));
  }

  public boolean alreadyExecuted() {
    return resultData.isPresent();
  }

  public T getResult() {
    return resultData.orElseGet(null);
  }

  /*
   * Sets the operation as succeeded.
   */
  public void succeeded(T data) {
    resultData = Optional.of(data);
  }

  /*
   * Close will register the id as finished if the operation was successful and will unregister it if it was not.
   */
  public void close() {
    if (registry == null) {
      return;
    }

    if (resultData.isPresent()) {
      registry.finish(id, resultData.get());
    } else {
      registry.unregister(id);
    }
  }
}
