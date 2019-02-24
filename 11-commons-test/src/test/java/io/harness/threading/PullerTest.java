package io.harness.threading;

import static java.time.Duration.ofMillis;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;

import io.harness.exception.PullTimeoutException;
import org.junit.Test;

public class PullerTest {
  @Test
  public void testPuller() {
    assertThat(catchThrowable(() -> Puller.pullFor(ofMillis(10), ofMillis(1), () -> false)))
        .isInstanceOf(PullTimeoutException.class);
  }
}
