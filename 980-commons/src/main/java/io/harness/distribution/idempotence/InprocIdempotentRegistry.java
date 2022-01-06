/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.distribution.idempotence;

import static io.harness.distribution.idempotence.IdempotentRegistry.State.DONE;
import static io.harness.distribution.idempotence.IdempotentRegistry.State.NEW;
import static io.harness.distribution.idempotence.IdempotentRegistry.State.RUNNING;
import static io.harness.govern.Switch.unhandled;

import static java.util.Collections.synchronizedMap;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.distribution.idempotence.Record.InternalState;
import io.harness.exception.UnexpectedException;

import java.time.Duration;
import java.util.Map;
import org.apache.commons.collections.map.LRUMap;

/*
 * InprocIdempotentRegistry implements IdempotentRegistry with in-process synchronization primitive.
 */
@OwnedBy(HarnessTeam.CDC)
public class InprocIdempotentRegistry<T extends IdempotentResult> implements IdempotentRegistry<T> {
  @SuppressWarnings("unchecked") private Map<IdempotentId, Object> map = synchronizedMap(new LRUMap(1000));

  @Override
  public IdempotentLock create(IdempotentId id) {
    return IdempotentLock.create(id, this);
  }

  @Override
  public IdempotentLock create(IdempotentId id, Duration timeout, Duration pollingInterval, Duration ttl) {
    return IdempotentLock.create(id, this, timeout, pollingInterval, ttl);
  }

  @Override
  public Response register(IdempotentId id, Duration ttl) {
    final Record<T> record = (Record<T>) map.compute(id, (k, v) -> {
      Record<T> r = (Record<T>) v;

      final long now = System.currentTimeMillis();
      if (r == null || r.getValidUntil() < now) {
        return Record.<T>builder().state(InternalState.TENTATIVE).validUntil(now + ttl.toMillis()).build();
      }
      switch (r.getState()) {
        case TENTATIVE:
        case TENTATIVE_ALREADY:
          return Record.<T>builder().state(InternalState.TENTATIVE_ALREADY).validUntil(now + ttl.toMillis()).build();
        case FINISHED:
          return r;
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
      Record<T> r = (Record<T>) v;

      switch (r.getState()) {
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
  public <T extends IdempotentResult> void finish(IdempotentId id, T result) {
    map.compute(id, (k, v) -> {
      Record<T> r = (Record<T>) v;
      final Record<T> record =
          Record.<T>builder().state(InternalState.FINISHED).result(result).validUntil(r.getValidUntil()).build();
      return record;
    });
  }
}
