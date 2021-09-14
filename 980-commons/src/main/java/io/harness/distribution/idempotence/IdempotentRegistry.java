/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.distribution.idempotence;

import java.time.Duration;
import lombok.Builder;
import lombok.Value;

public interface IdempotentRegistry<T extends IdempotentResult> {
  enum State {
    /*
     * New indicates that this idempotent operation was not observed before.
     */
    NEW,
    /*
     * Running indicates that there is currently running operation with this idempotent id.
     */
    RUNNING,
    /*
     * Done indicates that there was successful operation that already finished for the idempotent id.
     */
    DONE
  }

  @Value
  @Builder
  class Response<T extends IdempotentResult> {
    private State state;
    private T result;
  }

  /*
   * Register the idempotent operation if possible/needed and returns the current state.
   */
  Response register(IdempotentId id, Duration ttl);

  /*
   * Marks the idempotent operation as successfully done.
   */
  <T extends IdempotentResult> void finish(IdempotentId id, T result);

  /*
   * Unregister the idempotent operation.
   */
  void unregister(IdempotentId id);

  /*
   * Creates idempotent lock object for particular id
   */
  IdempotentLock create(IdempotentId id);

  /*
   * Creates idempotent lock object for particular id and timeout
   */
  IdempotentLock create(IdempotentId id, Duration lockTimeout, Duration pollingInterval, Duration ttl);
}
