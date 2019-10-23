package software.wings.beans.concurrency;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.beans.concurrency.ConcurrencyStrategy.UnitType;

public class ConcurrencyStrategyTest extends WingsBaseTest {
  @Test
  @Category(UnitTests.class)
  public void testIsEnabled() {
    ConcurrencyStrategy concurrencyStrategy = ConcurrencyStrategy.builder().build();
    assertThat(concurrencyStrategy.isEnabled()).isTrue();
  }

  @Test
  @Category(UnitTests.class)
  public void testNotEnabled() {
    ConcurrencyStrategy concurrencyStrategy = ConcurrencyStrategy.builder().unitType(UnitType.NONE).build();
    assertThat(concurrencyStrategy.isEnabled()).isFalse();
  }
}