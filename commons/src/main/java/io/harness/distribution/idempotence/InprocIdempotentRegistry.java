package io.harness.distribution.idempotence;

import static io.harness.distribution.idempotence.IdempotentRegistry.State.DONE;
import static io.harness.distribution.idempotence.IdempotentRegistry.State.NEW;
import static io.harness.distribution.idempotence.IdempotentRegistry.State.RUNNING;
import static io.harness.govern.Switch.unhandled;
import static java.util.Collections.synchronizedMap;

import io.harness.distribution.idempotence.Record.InternalState;
import io.harness.exception.UnexpectedException;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.collections.map.LRUMap;

import java.time.Duration;
import java.util.Map;

@Value
@Builder
class Record<T> {
  enum InternalState {
    /*
     * Tentative currently running.
     */
    TENTATIVE,
    /*
     * Tentative currently running from another thread.
     */
    TENTATIVE_ALREADY,
    /*
     * Finished indicates it is already done.
     */
    FINISHED,
  }

  private InternalState state;
  private T result;
}

/*
 * InprocIdempotentRegistry implements IdempotentRegistry with in-process synchronization primitive.
 */
public class InprocIdempotentRegistry<T> implements IdempotentRegistry<T> {
  @SuppressWarnings("unchecked") private Map<IdempotentId, Record<T>> map = synchronizedMap(new LRUMap(1000));

  @Override
  public IdempotentLock create(IdempotentId id) throws UnableToRegisterIdempotentOperationException {
    return IdempotentLock.create(id, this);
  }

  @Override
  public IdempotentLock create(IdempotentId id, Duration timeout, Duration pollingInterval)
      throws UnableToRegisterIdempotentOperationException {
    return IdempotentLock.create(id, this, timeout, pollingInterval);
  }

  @Override
  public Response register(IdempotentId id) throws UnableToRegisterIdempotentOperationException {
    final Record<T> record = map.compute(id, (k, v) -> {
      if (v == null) {
        return Record.<T>builder().state(InternalState.TENTATIVE).build();
      }
      switch (v.getState()) {
        case TENTATIVE:
        case TENTATIVE_ALREADY:
          return Record.<T>builder().state(InternalState.TENTATIVE_ALREADY).build();
        case FINISHED:
          return v;
        default:
          unhandled(v);
      }

      throw new UnexpectedException();
    });

    switch (record.getState()) {
      case TENTATIVE:
        return Response.builder().state(NEW).build();
      case TENTATIVE_ALREADY:
        return Response.builder().state(RUNNING).build();
      case FINISHED:
        return Response.builder().state(DONE).result(record.getResult()).build();
      default:
        unhandled(record.getState());
    }
    throw new UnexpectedException();
  }

  @Override
  public void unregister(IdempotentId id) {
    map.computeIfPresent(id, (k, v) -> {
      switch (v.getState()) {
        case TENTATIVE:
        case TENTATIVE_ALREADY:
          return null;
        case FINISHED:
          return v;
        default:
          unhandled(v);
      }
      throw new UnexpectedException();
    });
  }

  @Override
  public void finish(IdempotentId id, T result) {
    map.put(id, Record.<T>builder().state(InternalState.FINISHED).result(result).build());
  }
}
