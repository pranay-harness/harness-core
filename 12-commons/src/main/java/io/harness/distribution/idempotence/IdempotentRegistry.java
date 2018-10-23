package io.harness.distribution.idempotence;

import lombok.Builder;
import lombok.Value;

import java.time.Duration;

public interface IdempotentRegistry<T> {
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
  class Response<T> {
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
  void finish(IdempotentId id, T result);

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
