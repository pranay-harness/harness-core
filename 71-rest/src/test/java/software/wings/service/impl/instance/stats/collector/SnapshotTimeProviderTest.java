package software.wings.service.impl.instance.stats.collector;

import static io.harness.rule.OwnerRule.UJJAWAL;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class SnapshotTimeProviderTest extends CategoryTest {
  @Test
  @Owner(developers = UJJAWAL)
  @Category(UnitTests.class)
  public void testProvider() {
    Instant lastTs = Instant.now().minus(21, ChronoUnit.MINUTES);

    SnapshotTimeProvider provider = new SnapshotTimeProvider(lastTs, 10);

    assertThat(provider.hasNext()).isTrue();
    Instant updated = lastTs.plus(10, ChronoUnit.MINUTES);
    assertThat(updated).isEqualTo(provider.next());

    assertThat(provider.hasNext()).isTrue();
    updated = updated.plus(10, ChronoUnit.MINUTES);
    assertThat(updated).isEqualTo(provider.next());

    assertThat(provider.hasNext()).isFalse();
    assertThat(provider.next()).isNull();
  }
}