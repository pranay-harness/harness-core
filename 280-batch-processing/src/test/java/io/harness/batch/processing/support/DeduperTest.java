package io.harness.batch.processing.support;

import static io.harness.rule.OwnerRule.AVMOHAN;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.batch.processing.support.Deduper.Timestamped;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.time.Instant;

public class DeduperTest extends CategoryTest {
  private Instant t0 = Instant.now();
  private Deduper<String> deduper;

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldRejectIfWithinThresholdEqual() throws Exception {
    deduper = new Deduper<>(singletonList(Timestamped.of(t0.toEpochMilli(), "abc")));
    assertThat(deduper.checkEvent(Timestamped.of(t0.plusSeconds(8).toEpochMilli(), "abc"))).isFalse();
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldAcceptIfWithinThresholdUnequal() throws Exception {
    deduper = new Deduper<>(singletonList(Timestamped.of(t0.toEpochMilli(), "abc")));
    assertThat(deduper.checkEvent(Timestamped.of(t0.plusSeconds(8).toEpochMilli(), "def"))).isTrue();
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldAcceptIfNotWithinThreshold() throws Exception {
    deduper = new Deduper<>(singletonList(Timestamped.of(t0.toEpochMilli(), "abc")));
    assertThat(deduper.checkEvent(Timestamped.of(t0.plusSeconds(70).toEpochMilli(), "abc"))).isTrue();
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldRejectIfTimestampNotAfterMaxSeen() throws Exception {
    deduper = new Deduper<>(singletonList(Timestamped.of(t0.toEpochMilli(), "abc")));
    assertThat(deduper.checkEvent(Timestamped.of(t0.minusMillis(100).toEpochMilli(), "def"))).isFalse();
  }
}
