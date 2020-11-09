package io.harness.threading;

import static io.harness.rule.OwnerRule.GEORGE;
import static java.time.Duration.ofMillis;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.exception.PollTimeoutException;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class PullerTest extends CategoryTest {
  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testPuller() {
    assertThat(catchThrowable(() -> Poller.pollFor(ofMillis(10), ofMillis(1), () -> false)))
        .isInstanceOf(PollTimeoutException.class);
  }
}
