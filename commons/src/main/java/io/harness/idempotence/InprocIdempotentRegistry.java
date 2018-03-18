package io.harness.idempotence;

import static io.harness.govern.Switch.unhandled;
import static io.harness.idempotence.IdempotentRegistry.State.DONE;
import static io.harness.idempotence.IdempotentRegistry.State.NEW;
import static io.harness.idempotence.IdempotentRegistry.State.RUNNING;
import static io.harness.idempotence.InprocIdempotentRegistry.InternalState.FINISHED;
import static io.harness.idempotence.InprocIdempotentRegistry.InternalState.TENTATIVE;
import static io.harness.idempotence.InprocIdempotentRegistry.InternalState.TENTATIVE_ALREADY;
import static java.util.Collections.synchronizedMap;

import io.harness.exception.UnableToRegisterIdempotentOperationException;
import io.harness.exception.UnexpectedException;
import org.apache.commons.collections.map.LRUMap;

import java.util.Map;

/*
 * InprocIdempotentRegistry implements IdempotentRegistry with in-process synchronization primitive.
 */
public class InprocIdempotentRegistry implements IdempotentRegistry {
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

  private Map<IdempotentId, InternalState> map = synchronizedMap(new LRUMap(1000));

  @Override
  public State register(IdempotentId id) throws UnableToRegisterIdempotentOperationException {
    final InternalState internalState = map.compute(id, (k, v) -> {
      if (v == null) {
        return TENTATIVE;
      }
      switch (v) {
        case TENTATIVE:
        case TENTATIVE_ALREADY:
          return TENTATIVE_ALREADY;
        case FINISHED:
          return FINISHED;
        default:
          unhandled(v);
      }

      throw new UnexpectedException();
    });

    switch (internalState) {
      case TENTATIVE:
        return NEW;
      case TENTATIVE_ALREADY:
        return RUNNING;
      case FINISHED:
        return DONE;
      default:
        unhandled(internalState);
    }
    throw new UnexpectedException();
  }

  @Override
  public void unregister(IdempotentId id) {
    map.computeIfPresent(id, (k, v) -> {
      switch (v) {
        case TENTATIVE:
        case TENTATIVE_ALREADY:
          return null;
        case FINISHED:
          return FINISHED;
        default:
          unhandled(v);
      }
      throw new UnexpectedException();
    });
  }

  @Override
  public void finish(IdempotentId id) {
    map.put(id, FINISHED);
  }
}
