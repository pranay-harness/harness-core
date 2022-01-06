/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.distribution.barrier;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.distribution.barrier.Barrier.State.DOWN;
import static io.harness.distribution.barrier.Barrier.State.ENDURE;
import static io.harness.distribution.barrier.Barrier.State.STANDING;
import static io.harness.distribution.barrier.Barrier.State.TIMED_OUT;
import static io.harness.govern.Switch.unhandled;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import lombok.Builder;
import lombok.Value;

/*
 * Distributed Barrier is designed to provide a very well known inproc pattern of waiting until every thread hits a
 * certain point and then unblocks all of them together, but in distributed environment. This obviously creates set of
 * challenges that do not face the inproc scenario.
 *
 *
 * Before we get in details lets put a small glossary:
 * * down condition - condition that determines when the barrier is pushed down
 * * forcer - an agent that applies force on the barrier.
 * * barrier - system that blocks the progress of forcers until a down condition is met
 * * barrier registry - a storage where the barrier can be persisted in a distributed way
 * * force proctor - a system that interacts with the real live forcers to determine their state
 *
 * The Barrier is a Combining Tree Polling Barrier that can be used in distributed environment with
 * unreliable forcers. This means that it will allow to provide the necessary unblocking even if
 * forcers are unavailable to push their change of state.
 *
 * The combining tree allows for reducing the number of request to every individual forcers and also
 * eliminates the need of every single one to be executed. Only the top level forcer is critical for the
 * barrier behavior.
 */

@Value
@Builder
public class Barrier {
  private BarrierId id;
  private Forcer forcer;

  public enum State {
    // Standing when the barrier is standing because not all of the forcers are inline.
    STANDING,

    // Down when all the forcers lined up to push the barrier.
    DOWN,

    // The barrier endure when at least one of the forcers abandoned arriving to the barrier.
    ENDURE,

    // WF 2.0, Timed out when one of the barriers expired
    TIMED_OUT
  }
  @Builder.Default private State state = STANDING;

  public static Barrier create(BarrierId id, Forcer forcer, BarrierRegistry registry)
      throws UnableToSaveBarrierException {
    registry.save(id, forcer);

    return builder().id(id).forcer(forcer).build();
  }

  public static Barrier load(BarrierId id, BarrierRegistry registry) throws UnableToLoadBarrierException {
    return registry.load(id);
  }

  private State calculateState(ForceProctor proctor) {
    // Check the forcers breadth first
    Deque<Forcer> deque = new ArrayDeque<>();
    deque.add(forcer);

    Instant startTime = Instant.now();

    // By default we assume that all forcers already succeeded
    State result = DOWN;

    int forcerCount = 0;

    while (!deque.isEmpty()) {
      Forcer firstForcer = deque.removeFirst();
      if (firstForcer.getId() == null || firstForcer.getId().getValue() == null) {
        result = STANDING;
        continue;
      }

      forcerCount++;

      final Forcer.State forcerState = proctor.getForcerState(firstForcer.getId(), firstForcer.getMetadata());
      switch (forcerState) {
        case ABSENT:
          // If the forcer is absent, the barrier stands, but there is no reason to check the children
          result = STANDING;
          break;

        case APPROACHING:
          final List<Forcer> children = firstForcer.getChildren();

          // Running parent suggests that there might be children that are in progress, but some of them
          // might failed. We need to check the children about that.
          if (isEmpty(children)) {
            // If the forcer is still approaching the barrier and it has no children the barrier stands.
            result = STANDING;
          } else {
            children.forEach(deque::addLast);
          }
          break;

        case ARRIVED:
          // If the parent arrived, assume that all children arrived too.
          break;

        case ABANDONED:
          // If any of the forcers failed, there is nothing else to check - the barrier outlasts the forcers.
          return ENDURE;

        case TIMED_OUT:
          return TIMED_OUT;
        default:
          unhandled(forcerState);
      }
    }

    return result;
  }

  /*
   * Push down method will make an attempt to push down the barrier. It will check the forcers if they are
   * still processing
   */
  public State pushDown(ForceProctor proctor) {
    if (state != STANDING) {
      return state;
    }
    return calculateState(proctor);
  }

  public static String getDuration(Instant startTime) {
    Duration duration = Duration.between(startTime, Instant.now());
    return String.format("%ds %dns", duration.getSeconds(), duration.getNano());
  }
}
