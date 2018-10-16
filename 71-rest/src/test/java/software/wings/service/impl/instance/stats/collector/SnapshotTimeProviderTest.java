package software.wings.service.impl.instance.stats.collector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class SnapshotTimeProviderTest {
  @Test
  public void testProvider() {
    Instant lastTs = Instant.now().minus(21, ChronoUnit.MINUTES);

    SnapshotTimeProvider provider = new SnapshotTimeProvider(lastTs, 10);

    assertTrue(provider.hasNext());
    Instant updated = lastTs.plus(10, ChronoUnit.MINUTES);
    assertEquals(provider.next(), updated);

    assertTrue(provider.hasNext());
    updated = updated.plus(10, ChronoUnit.MINUTES);
    assertEquals(provider.next(), updated);

    assertFalse(provider.hasNext());
    assertNull(provider.next());
  }
}