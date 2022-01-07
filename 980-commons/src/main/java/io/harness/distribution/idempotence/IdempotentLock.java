/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.distribution.idempotence;

import static io.harness.govern.Switch.unhandled;

import static java.lang.String.format;
import static java.time.Duration.ofDays;
import static java.time.Duration.ofMillis;
import static java.time.Duration.ofMinutes;

import io.harness.threading.Morpheus;

import java.time.Duration;
import java.util.Optional;
import lombok.Builder;

/*
 * IdempotentLock allows for using try-resource java feature to lock non-idempotent operation and
 * convert it to idempotent.
 */

@Builder
public class IdempotentLock<T extends IdempotentResult> implements AutoCloseable {
  public static Duration defaultPollingInterval = ofMillis(100);
  public static Duration defaultLockTimeout = ofMinutes(3);
  public static Duration defaultTTL = ofDays(7);

  private IdempotentId id;
  private IdempotentRegistry registry;
  private Optional<T> resultData;

  public static IdempotentLock create(IdempotentId id, IdempotentRegistry registry) {
    return create(id, registry, defaultLockTimeout, defaultPollingInterval, defaultTTL);
  }

  public static IdempotentLock create(
      IdempotentId id, IdempotentRegistry registry, Duration lockTimeout, Duration pollingInterval, Duration ttl) {
    long systemTimeMillis = System.currentTimeMillis();

    do {
      IdempotentRegistry.Response response = registry.register(id, ttl);
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
    } while (System.currentTimeMillis() - systemTimeMillis < lockTimeout.toMillis());

    throw new UnableToRegisterIdempotentOperationException(
        format("Acquiring idempotent lock for operation %s timed out after %d seconds", id.getValue(),
            lockTimeout.getSeconds()));
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
  @Override
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
