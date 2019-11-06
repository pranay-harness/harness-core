package io.harness.waiter;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.OrchestrationTest;
import io.harness.category.element.UnitTests;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Slf4j
public class NotifierTest extends OrchestrationTest {
  @Test
  @Category(UnitTests.class)
  public void testNextSkip() {
    assertThat(Notifier.nextSkip(0, 0, 0, () -> 0)).isEqualTo(0);
    assertThat(Notifier.nextSkip(0, Notifier.PAGE_SIZE - 1, 0, () -> 3 * Notifier.PAGE_SIZE)).isEqualTo(0);
    assertThat(Notifier.nextSkip(0, Notifier.PAGE_SIZE, 0, () -> 3 * Notifier.PAGE_SIZE))
        .isEqualTo(2 * Notifier.PAGE_SIZE);
    assertThat(Notifier.nextSkip(0, Notifier.PAGE_SIZE, 0, () -> Notifier.PAGE_SIZE - 1)).isEqualTo(0);
    assertThat(Notifier.nextSkip(Notifier.PAGE_SIZE, 0, Notifier.PAGE_SIZE / 2, () -> 0))
        .isEqualTo(Notifier.PAGE_SIZE / 2);
    assertThat(Notifier.nextSkip(Notifier.PAGE_SIZE - 1, 0, Notifier.PAGE_SIZE, () -> 0)).isEqualTo(0);
  }
}
