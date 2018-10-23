package io.harness.distribution.idempotent;

import static io.harness.threading.Morpheus.sleep;
import static java.time.Duration.ofMillis;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.distribution.idempotence.IdempotentId;
import io.harness.distribution.idempotence.IdempotentLock;
import io.harness.distribution.idempotence.IdempotentRegistry;
import io.harness.distribution.idempotence.IdempotentRegistry.State;
import io.harness.distribution.idempotence.InprocIdempotentRegistry;
import io.harness.distribution.idempotence.UnableToRegisterIdempotentOperationException;
import io.harness.rule.RepeatRule.Repeat;
import io.harness.threading.Concurrent;
import org.junit.Ignore;
import org.junit.Test;

import java.security.SecureRandom;
import java.util.ArrayList;

public class IdempotentTest {
  IdempotentId id = new IdempotentId("foo");

  private static IdempotentRegistry.Response<Boolean> newResponse =
      IdempotentRegistry.Response.<Boolean>builder().state(State.NEW).build();
  private static IdempotentRegistry.Response<Boolean> doneResponse =
      IdempotentRegistry.Response.<Boolean>builder().state(State.DONE).result(Boolean.TRUE).build();

  @Test
  public void testNewIdempotentFailed() {
    final IdempotentRegistry mockIdempotentRegistry = mock(IdempotentRegistry.class);

    when(mockIdempotentRegistry.register(any(), any())).thenReturn(newResponse);

    try (IdempotentLock idempotent = IdempotentLock.create(id, mockIdempotentRegistry)) {
      assertNotNull(idempotent);
    }

    verify(mockIdempotentRegistry).unregister(id);
  }

  @Test
  public void testNewIdempotentSucceeded() {
    final IdempotentRegistry mockIdempotentRegistry = mock(IdempotentRegistry.class);

    when(mockIdempotentRegistry.register(any(), any())).thenReturn(newResponse);

    try (IdempotentLock idempotent = IdempotentLock.create(id, mockIdempotentRegistry)) {
      assertNotNull(idempotent);
      idempotent.succeeded(Boolean.TRUE);
    }

    verify(mockIdempotentRegistry).finish(id, Boolean.TRUE);
  }

  @Test
  public void testFinishedIdempotent() {
    final IdempotentRegistry mockIdempotentRegistry = mock(IdempotentRegistry.class);

    when(mockIdempotentRegistry.register(any(), any())).thenReturn(doneResponse);

    try (IdempotentLock<Boolean> idempotent = IdempotentLock.create(id, mockIdempotentRegistry)) {
      assertNotNull(idempotent);
      assertTrue(idempotent.alreadyExecuted());
      assertTrue(idempotent.getResult());
    }
  }

  @Test
  @Ignore // TODO: find more reliable way to test this
  public void testIdempotentAfterTtl() {
    final IdempotentRegistry<Boolean> idempotentRegistry = new InprocIdempotentRegistry<>();
    try (IdempotentLock<Boolean> idempotent = idempotentRegistry.create(id, ofMillis(1), ofMillis(1), ofMillis(500))) {
      assertNotNull(idempotent);
      assertFalse(idempotent.alreadyExecuted());
      idempotent.succeeded(true);
    }
    try (IdempotentLock<Boolean> idempotent = idempotentRegistry.create(id)) {
      assertNotNull(idempotent);
      assertTrue(idempotent.alreadyExecuted());
    }
    sleep(ofMillis(510));
    try (IdempotentLock<Boolean> idempotent = idempotentRegistry.create(id)) {
      assertNotNull(idempotent);
      assertFalse(idempotent.alreadyExecuted());
    }
  }

  public void concurrencyTest(IdempotentRegistry idempotentRegistry) {
    final ArrayList<Integer> integers = new ArrayList<>();
    SecureRandom random = new SecureRandom();

    Concurrent.test(10, i -> {
      // We need at least one thread to execute positive scenario, else the test will fail
      if (i == 0 || random.nextBoolean()) {
        try (IdempotentLock idempotent = IdempotentLock.create(id, idempotentRegistry)) {
          if (idempotent.alreadyExecuted()) {
            return;
          }
          integers.add(1);
          idempotent.succeeded(Boolean.TRUE);
        } catch (UnableToRegisterIdempotentOperationException e) {
          // do nothing
        }
      } else {
        try (IdempotentLock idempotent = IdempotentLock.create(id, idempotentRegistry)) {
        } catch (UnableToRegisterIdempotentOperationException e) {
          // do nothing
        }
      }
    });

    assertEquals(1, integers.size());
  }

  @Test
  @Repeat(times = 10, successes = 10)
  public void testInprocRegistryConcurrency() throws InterruptedException {
    final IdempotentRegistry idempotentRegistry = new InprocIdempotentRegistry();
    concurrencyTest(idempotentRegistry);
  }
}