/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
