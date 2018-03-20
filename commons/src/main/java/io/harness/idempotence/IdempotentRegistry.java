package io.harness.idempotence;

import io.harness.exception.UnableToRegisterIdempotentOperationException;
import lombok.Builder;
import lombok.Value;

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
  Response register(IdempotentId id) throws UnableToRegisterIdempotentOperationException;

  /*
   * Marks the idempotent operation as successfully done.
   */
  void finish(IdempotentId id, T result);

  /*
   * Unregister the idempotent operation.
   */
  void unregister(IdempotentId id);
}
