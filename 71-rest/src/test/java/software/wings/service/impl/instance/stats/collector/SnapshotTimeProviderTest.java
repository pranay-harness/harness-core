package software.wings.service.impl.instance.stats.collector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class SnapshotTimeProviderTest extends CategoryTest {
  @Test
  @Category(UnitTests.class)
  public void testProvider() {
    Instant lastTs = Instant.now().minus(21, ChronoUnit.MINUTES);

    SnapshotTimeProvider provider = new SnapshotTimeProvider(lastTs, 10);

    assertThat(provider.hasNext()).isTrue();
    Instant updated = lastTs.plus(10, ChronoUnit.MINUTES);
    assertEquals(provider.next(), updated);

    assertThat(provider.hasNext()).isTrue();
    updated = updated.plus(10, ChronoUnit.MINUTES);
    assertEquals(provider.next(), updated);

    assertFalse(provider.hasNext());
    assertThat(provider.next()).isNull();
  }
}