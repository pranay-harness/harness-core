/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.iterator;

import static io.harness.rule.OwnerRule.SANJA;

import static java.time.Duration.ofMinutes;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.PersistenceTestBase;
import io.harness.category.element.UnitTests;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.rule.Owner;
import io.harness.threading.Morpheus;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Slf4j
public class PersistentFibonacciIteratorTest extends PersistenceTestBase {
  class TestHandler implements MongoPersistenceIterator.Handler<TestFibonacciIterableEntity> {
    @Override
    public void handle(TestFibonacciIterableEntity entity) {
      Morpheus.sleep(ofSeconds(1));
      log.info("Handle {}", entity.getUuid());
    }
  }

  @Test
  @Owner(developers = SANJA)
  @Category(UnitTests.class)
  public void testRecalculateTimestampsWhenEmpty() {
    TestFibonacciIterableEntity cronIterableEntity =
        TestFibonacciIterableEntity.builder().nextIterations(new ArrayList<>()).build();

    Long now = System.currentTimeMillis();
    List<Long> timestamps = cronIterableEntity.recalculateNextIterations("", true, now);

    assertThat(timestamps)
        .containsExactly(now + ofMinutes(1).toMillis(), now + ofMinutes(2).toMillis(), now + ofMinutes(4).toMillis(),
            now + ofMinutes(7).toMillis(), now + ofMinutes(12).toMillis(), now + ofMinutes(20).toMillis(),
            now + ofMinutes(33).toMillis(), now + ofMinutes(54).toMillis(), now + ofMinutes(88).toMillis(),
            now + ofMinutes(143).toMillis());
  }

  @Test
  @Owner(developers = SANJA)
  @Category(UnitTests.class)
  public void testRecalculateTimestampsWhenLessThanInventory() {
    Long now = System.currentTimeMillis();
    ArrayList<Long> nextIterations = new ArrayList<>();
    nextIterations.add(now + ofMinutes(10).toMillis());
    TestFibonacciIterableEntity cronIterableEntity =
        TestFibonacciIterableEntity.builder().nextIterations(nextIterations).build();

    List<Long> timestamps = cronIterableEntity.recalculateNextIterations("", true, now);

    assertThat(timestamps)
        .containsExactly(now + ofMinutes(10).toMillis(), now + ofMinutes(70).toMillis(),
            now + ofMinutes(130).toMillis(), now + ofMinutes(190).toMillis(), now + ofMinutes(250).toMillis(),
            now + ofMinutes(310).toMillis(), now + ofMinutes(370).toMillis(), now + ofMinutes(430).toMillis(),
            now + ofMinutes(490).toMillis(), now + ofMinutes(550).toMillis());
  }

  @Test
  @Owner(developers = SANJA)
  @Category(UnitTests.class)
  public void testSkippingMissedIterations() {
    Long now = System.currentTimeMillis();
    ArrayList<Long> nextIterations = new ArrayList<>();
    nextIterations.add(now - ofMinutes(30).toMillis());
    nextIterations.add(now - ofMinutes(10).toMillis());

    TestFibonacciIterableEntity cronIterableEntity =
        TestFibonacciIterableEntity.builder().nextIterations(nextIterations).build();

    List<Long> timestamps = cronIterableEntity.recalculateNextIterations("", true, now);

    assertThat(timestamps)
        .containsExactly(now + ofMinutes(50).toMillis(), now + ofMinutes(110).toMillis(),
            now + ofMinutes(170).toMillis(), now + ofMinutes(230).toMillis(), now + ofMinutes(290).toMillis(),
            now + ofMinutes(350).toMillis(), now + ofMinutes(410).toMillis(), now + ofMinutes(470).toMillis(),
            now + ofMinutes(530).toMillis(), now + ofMinutes(590).toMillis());
  }

  @Test
  @Owner(developers = SANJA)
  @Category(UnitTests.class)
  public void testSkippingMissedIterationsWithCurrentTime() {
    Long now = System.currentTimeMillis();
    ArrayList<Long> nextIterations = new ArrayList<>();
    nextIterations.add(now - ofMinutes(30).toMillis());
    nextIterations.add(now - ofMinutes(10).toMillis());
    nextIterations.add(now);

    TestFibonacciIterableEntity cronIterableEntity =
        TestFibonacciIterableEntity.builder().nextIterations(nextIterations).build();

    List<Long> timestamps = cronIterableEntity.recalculateNextIterations("", true, now);

    assertThat(timestamps)
        .containsExactly(now + ofMinutes(60).toMillis(), now + ofMinutes(120).toMillis(),
            now + ofMinutes(180).toMillis(), now + ofMinutes(240).toMillis(), now + ofMinutes(300).toMillis(),
            now + ofMinutes(360).toMillis(), now + ofMinutes(420).toMillis(), now + ofMinutes(480).toMillis(),
            now + ofMinutes(540).toMillis(), now + ofMinutes(600).toMillis());
  }

  @Test
  @Owner(developers = SANJA)
  @Category(UnitTests.class)
  public void testSkippingMissedIterationsFarInThePast() {
    Long now = System.currentTimeMillis();
    ArrayList<Long> nextIterations = new ArrayList<>();
    nextIterations.add(now - ofMinutes(650).toMillis());
    nextIterations.add(now - ofMinutes(590).toMillis());

    TestFibonacciIterableEntity cronIterableEntity =
        TestFibonacciIterableEntity.builder().nextIterations(nextIterations).build();

    List<Long> timestamps = cronIterableEntity.recalculateNextIterations("", true, now);

    assertThat(timestamps)
        .containsExactly(now + ofMinutes(10).toMillis(), now + ofMinutes(70).toMillis(),
            now + ofMinutes(130).toMillis(), now + ofMinutes(190).toMillis(), now + ofMinutes(250).toMillis(),
            now + ofMinutes(310).toMillis(), now + ofMinutes(370).toMillis(), now + ofMinutes(430).toMillis(),
            now + ofMinutes(490).toMillis(), now + ofMinutes(550).toMillis());
  }

  @Test
  @Owner(developers = SANJA)
  @Category(UnitTests.class)
  public void testAllIterationsGoodShouldNotModify() {
    Long now = System.currentTimeMillis();
    ArrayList<Long> nextIterations = new ArrayList<>();
    nextIterations.add(now + ofMinutes(5).toMillis());
    nextIterations.add(now + ofMinutes(8).toMillis());
    nextIterations.add(now + ofMinutes(13).toMillis());

    TestFibonacciIterableEntity cronIterableEntity =
        TestFibonacciIterableEntity.builder().nextIterations(nextIterations).build();

    List<Long> timestamps = cronIterableEntity.recalculateNextIterations("", true, now);

    assertThat(timestamps).isNull();
  }
}
