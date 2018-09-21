package io.harness.threading;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;

import io.harness.exception.PullTimeoutException;
import org.junit.Test;

import java.time.Duration;

public class PullerTest {
  @Test
  public void testPuller() {
    assertThat(catchThrowable(() -> Puller.pullFor(Duration.ofMillis(10), () -> false)))
        .isInstanceOf(PullTimeoutException.class);
  }
}
