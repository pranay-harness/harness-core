package io.harness.distribution.idempotent;

import static io.harness.rule.OwnerRule.GEORGE;
import static io.harness.threading.Morpheus.sleep;
import static java.time.Duration.ofMillis;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.distribution.idempotence.IdempotentId;
import io.harness.distribution.idempotence.IdempotentLock;
import io.harness.distribution.idempotence.IdempotentRegistry;
import io.harness.distribution.idempotence.IdempotentRegistry.State;
import io.harness.distribution.idempotence.IdempotentResult;
import io.harness.distribution.idempotence.InprocIdempotentRegistry;
import io.harness.distribution.idempotence.UnableToRegisterIdempotentOperationException;
import io.harness.rule.OwnerRule.Owner;
import io.harness.rule.RepeatRule.Repeat;
import io.harness.threading.Concurrent;
import lombok.Builder;
import lombok.Value;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.security.SecureRandom;
import java.util.ArrayList;

public class IdempotentTest extends CategoryTest {
  IdempotentId id = new IdempotentId("foo");

  @Value
  @Builder
  private static class BooleanIdempotentResult implements IdempotentResult {
    private Boolean value;
  }

  private static BooleanIdempotentResult TRUE = BooleanIdempotentResult.builder().value(Boolean.TRUE).build();

  private static IdempotentRegistry.Response<BooleanIdempotentResult> newResponse =
      IdempotentRegistry.Response.<BooleanIdempotentResult>builder().state(State.NEW).build();
  private static IdempotentRegistry.Response<BooleanIdempotentResult> doneResponse =
      IdempotentRegistry.Response.<BooleanIdempotentResult>builder().state(State.DONE).result(TRUE).build();

  @Test
  @Category(UnitTests.class)
  public void testNewIdempotentFailed() {
    final IdempotentRegistry mockIdempotentRegistry = mock(IdempotentRegistry.class);

    when(mockIdempotentRegistry.register(any(), any())).thenReturn(newResponse);

    try (IdempotentLock idempotent = IdempotentLock.create(id, mockIdempotentRegistry)) {
      assertNotNull(idempotent);
    }

    verify(mockIdempotentRegistry).unregister(id);
  }

  @Test
  @Category(UnitTests.class)
  public void testNewIdempotentSucceeded() {
    final IdempotentRegistry mockIdempotentRegistry = mock(IdempotentRegistry.class);

    when(mockIdempotentRegistry.register(any(), any())).thenReturn(newResponse);

    try (IdempotentLock idempotent = IdempotentLock.create(id, mockIdempotentRegistry)) {
      assertNotNull(idempotent);
      idempotent.succeeded(TRUE);
    }

    verify(mockIdempotentRegistry).finish(id, TRUE);
  }

  @Test
  @Category(UnitTests.class)
  public void testFinishedIdempotent() {
    final IdempotentRegistry mockIdempotentRegistry = mock(IdempotentRegistry.class);

    when(mockIdempotentRegistry.register(any(), any())).thenReturn(doneResponse);

    try (IdempotentLock<BooleanIdempotentResult> idempotent = IdempotentLock.create(id, mockIdempotentRegistry)) {
      assertNotNull(idempotent);
      assertThat(idempotent.alreadyExecuted()).isTrue();
      assertThat(idempotent.getResult().getValue()).isTrue();
    }
  }

  @Test
  @Owner(emails = GEORGE)
  @Category(UnitTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void testIdempotentAfterTtl() {
    final IdempotentRegistry<BooleanIdempotentResult> idempotentRegistry = new InprocIdempotentRegistry<>();
    try (IdempotentLock<BooleanIdempotentResult> idempotent =
             idempotentRegistry.create(id, ofMillis(1), ofMillis(1), ofMillis(500))) {
      assertNotNull(idempotent);
      assertThat(idempotent.alreadyExecuted()).isFalse();
      idempotent.succeeded(TRUE);
    }
    try (IdempotentLock<BooleanIdempotentResult> idempotent = idempotentRegistry.create(id)) {
      assertNotNull(idempotent);
      assertThat(idempotent.alreadyExecuted()).isTrue();
    }
    sleep(ofMillis(510));
    try (IdempotentLock<BooleanIdempotentResult> idempotent = idempotentRegistry.create(id)) {
      assertNotNull(idempotent);
      assertThat(idempotent.alreadyExecuted()).isFalse();
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
          idempotent.succeeded(TRUE);
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
  @Category(UnitTests.class)
  public void testInprocRegistryConcurrency() throws InterruptedException {
    final IdempotentRegistry idempotentRegistry = new InprocIdempotentRegistry();
    concurrencyTest(idempotentRegistry);
  }
}