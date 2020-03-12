package io.harness.event.client.impl.tailer;

import static io.harness.rule.OwnerRule.AVMOHAN;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.time.FakeClock;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

public class SamplerTest extends CategoryTest {
  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldDisallowNegativeInterval() throws Exception {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new Sampler(Duration.ofMinutes(-1)))
        .withMessage("Sampling interval should be non-negative");
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldRunWhenDurationElapsed() throws Exception {
    FakeClock clock = new FakeClock();
    Sampler sampler = new Sampler(Duration.ofSeconds(1), clock);
    clock.advanceBy(2, ChronoUnit.SECONDS);
    sampler.updateTime();
    Runnable runnable = mock(Runnable.class);
    sampler.sampled(runnable);
    verify(runnable).run();
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldNotRunWhenDurationNotElapsed() throws Exception {
    FakeClock clock = new FakeClock();
    Sampler sampler = new Sampler(Duration.ofSeconds(20), clock);
    clock.advanceBy(19, ChronoUnit.SECONDS);
    sampler.updateTime();
    Runnable runnable = mock(Runnable.class);
    sampler.sampled(runnable);
    verifyZeroInteractions(runnable);
  }
}
