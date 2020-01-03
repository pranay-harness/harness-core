package io.harness.persistence;

import static io.harness.rule.OwnerRule.GEORGE;
import static java.lang.System.currentTimeMillis;
import static java.time.Duration.ofMinutes;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.PersistenceTest;
import io.harness.category.element.UnitTests;
import io.harness.iterator.PersistentIterable;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.rule.Owner;
import lombok.Builder;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.time.Duration;

public class MongoPersistenceIteratorTest extends PersistenceTest {
  private static Duration targetInterval = ofMinutes(1);
  private static Duration maximumDelayForCheck = ofMinutes(2);

  @Builder
  static class TestPersistentIterable implements PersistentIterable {
    private Long nextIteration;

    @Override
    public Long obtainNextIteration(String fieldName) {
      return nextIteration;
    }

    @Override
    public String getUuid() {
      return null;
    }
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldCalculateSleepDurationAsTargetInterval() {
    MongoPersistenceIterator iterator =
        MongoPersistenceIterator.<TestPersistentIterable>builder().targetInterval(targetInterval).build();
    assertThat(iterator.calculateSleepDuration(null)).isEqualTo(targetInterval);
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldCalculateSleepDurationAsMaximumDelayForCheck() {
    MongoPersistenceIterator iterator = MongoPersistenceIterator.<TestPersistentIterable>builder()
                                            .targetInterval(targetInterval)
                                            .maximumDelayForCheck(maximumDelayForCheck)
                                            .build();
    assertThat(iterator.calculateSleepDuration(null)).isEqualTo(maximumDelayForCheck);
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldCalculateSleepDurationAsZero() {
    MongoPersistenceIterator iterator = MongoPersistenceIterator.<TestPersistentIterable>builder()
                                            .targetInterval(targetInterval)
                                            .maximumDelayForCheck(maximumDelayForCheck)
                                            .build();

    TestPersistentIterable testPersistentIterable = TestPersistentIterable.builder().build();
    assertThat(iterator.calculateSleepDuration(testPersistentIterable)).isEqualTo(Duration.ZERO);
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldCalculateSleepDurationAsNextIteration() {
    MongoPersistenceIterator iterator = MongoPersistenceIterator.<TestPersistentIterable>builder()
                                            .targetInterval(targetInterval)
                                            .maximumDelayForCheck(maximumDelayForCheck)
                                            .build();

    TestPersistentIterable testPersistentIterable =
        TestPersistentIterable.builder().nextIteration(currentTimeMillis() + 10000).build();
    assertThat(iterator.calculateSleepDuration(testPersistentIterable).toMillis()).isLessThanOrEqualTo(10000);
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldCalculateSleepDurationLimitedAsNextIteration() {
    MongoPersistenceIterator iterator =
        MongoPersistenceIterator.<TestPersistentIterable>builder().targetInterval(targetInterval).build();

    TestPersistentIterable testPersistentIterable =
        TestPersistentIterable.builder().nextIteration(currentTimeMillis() + targetInterval.toMillis()).build();
    assertThat(iterator.calculateSleepDuration(testPersistentIterable).toMillis())
        .isLessThanOrEqualTo(targetInterval.toMillis());
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldCalculateSleepDurationLimitedAsMaximumDelayForCheck() {
    MongoPersistenceIterator iterator = MongoPersistenceIterator.<TestPersistentIterable>builder()
                                            .targetInterval(targetInterval)
                                            .maximumDelayForCheck(maximumDelayForCheck)
                                            .build();

    TestPersistentIterable testPersistentIterable =
        TestPersistentIterable.builder()
            .nextIteration(currentTimeMillis() + maximumDelayForCheck.toMillis() + 10000)
            .build();
    assertThat(iterator.calculateSleepDuration(testPersistentIterable)).isEqualTo(maximumDelayForCheck);
  }
}
