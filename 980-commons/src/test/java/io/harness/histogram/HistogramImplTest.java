/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.histogram;

import static io.harness.histogram.HistogramImpl.MAX_CHECKPOINT_WEIGHT;
import static io.harness.rule.OwnerRule.AVMOHAN;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.within;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import com.google.common.collect.ImmutableMap;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import lombok.val;
import org.joor.Reflect;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class HistogramImplTest extends CategoryTest {
  static final double valueEpsilon = 1e-15;
  private static final double weightEpsilon = 1e-15;
  private static final Instant anyTime = Instant.ofEpochMilli(0);
  static final HistogramOptions testHistogramOptions = new LinearHistogramOptions(10.0, 1.0, weightEpsilon);

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void testPercentilesEmptyHistogram() throws Exception {
    val h = new HistogramImpl(testHistogramOptions);
    for (double p = -0.5; p <= 1.5; p += 0.5) {
      assertThat(h.getPercentile(p)).isEqualTo(0.0);
    }
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void testPercentiles() throws Exception {
    val h = new HistogramImpl(testHistogramOptions);
    for (int i = 1; i <= 4; i++) {
      h.addSample(i, i, anyTime);
    }
    assertThat(h.getPercentile(0.0)).isCloseTo(2, within(valueEpsilon));
    assertThat(h.getPercentile(0.1)).isCloseTo(2, within(valueEpsilon));
    assertThat(h.getPercentile(0.2)).isCloseTo(3, within(valueEpsilon));
    assertThat(h.getPercentile(0.3)).isCloseTo(3, within(valueEpsilon));
    assertThat(h.getPercentile(0.4)).isCloseTo(4, within(valueEpsilon));
    assertThat(h.getPercentile(0.5)).isCloseTo(4, within(valueEpsilon));
    assertThat(h.getPercentile(0.6)).isCloseTo(4, within(valueEpsilon));
    assertThat(h.getPercentile(0.7)).isCloseTo(5, within(valueEpsilon));
    assertThat(h.getPercentile(0.8)).isCloseTo(5, within(valueEpsilon));
    assertThat(h.getPercentile(0.9)).isCloseTo(5, within(valueEpsilon));
    assertThat(h.getPercentile(1.0)).isCloseTo(5, within(valueEpsilon));
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void testPercentileOutOfBounds() throws Exception {
    val options = new LinearHistogramOptions(1.0, 0.1, weightEpsilon);
    val h = new HistogramImpl(options);
    h.addSample(0.1, 0.1, anyTime);
    h.addSample(0.2, 0.2, anyTime);

    assertThat(h.getPercentile(-0.1)).isCloseTo(0.2, within(valueEpsilon));
    assertThat(h.getPercentile(1.1)).isCloseTo(0.3, within(valueEpsilon));

    h.addSample(0.0, 0.1, anyTime);
    h.addSample(1.0, 0.2, anyTime);
    assertThat(h.getPercentile(-0.1)).isCloseTo(0.1, within(valueEpsilon));
    assertThat(h.getPercentile(1.1)).isCloseTo(1.0, within(valueEpsilon));
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void testEmptyHistogram() throws Exception {
    val options = new LinearHistogramOptions(1.0, 0.1, weightEpsilon);
    val h = new HistogramImpl(options);
    assertThat(h.isEmpty()).isTrue();

    h.addSample(0.1, weightEpsilon * 2.5, anyTime); // Sample weight = epsilon * 2.5
    assertThat(h.isEmpty()).isFalse();
    h.subtractSample(0.1, weightEpsilon, anyTime); // Sample weight = epsilon * 1.5
    assertThat(h.isEmpty()).isFalse();
    h.subtractSample(0.1, weightEpsilon, anyTime); // Sample weight = epsilon * 0.5
    assertThat(h.isEmpty()).isTrue();
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void testNonEmptyOnEpsilonAddition() throws Exception {
    val options = new LinearHistogramOptions(1.0, 0.1, weightEpsilon);
    val h = new HistogramImpl(options);
    assertThat(h.isEmpty()).isTrue();

    h.addSample(9.9, weightEpsilon * 3, anyTime);
    assertThat(h.isEmpty()).isFalse();
    h.addSample(0.1, weightEpsilon * 0.3, anyTime);
    assertThat(h.isEmpty()).isFalse();
    h.addSample(999.9, weightEpsilon * 0.3, anyTime);
    assertThat(h.isEmpty()).isFalse();
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void testHistogramMerge() throws Exception {
    val h1 = new HistogramImpl(testHistogramOptions);
    h1.addSample(1, 1, anyTime);
    h1.addSample(2, 1, anyTime);

    val h2 = new HistogramImpl(testHistogramOptions);
    h2.addSample(2, 1, anyTime);
    h2.addSample(3, 1, anyTime);

    val expected = new HistogramImpl(testHistogramOptions);
    expected.addSample(1, 1, anyTime);
    expected.addSample(2, 1, anyTime);
    expected.addSample(2, 1, anyTime);
    expected.addSample(3, 1, anyTime);

    h1.merge(h2);
    assertThat(h1).isEqualTo(expected);
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void testHistogramSaveToCheckpointEmpty() throws Exception {
    val h = new HistogramImpl(testHistogramOptions);
    val s = h.saveToCheckpoint();
    assertThat(s.getTotalWeight()).isEqualTo(0);
    assertThat(s.getBucketWeights()).isEmpty();
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void testHistogramSaveToCheckpoint() throws Exception {
    val h = new HistogramImpl(testHistogramOptions);
    h.addSample(1, 1, anyTime);
    val s = h.saveToCheckpoint();
    val bucket = testHistogramOptions.findBucket(1);
    assertThat(s.getTotalWeight()).isEqualTo(1.0);
    assertThat(s.getBucketWeights()).hasSize(1);
    assertThat(s.getBucketWeights()).containsKey(bucket);
    assertThat(s.getBucketWeights().get(bucket)).isEqualTo(MAX_CHECKPOINT_WEIGHT);
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void testHistogramSaveToCheckpointDropsRelativelySmallValues() throws Exception {
    val h = new HistogramImpl(testHistogramOptions);
    double v1 = 1.0;
    double w1 = 1.0;
    double v2 = 2.0;
    double w2 = 100000.0;

    h.addSample(v1, w1, anyTime);
    h.addSample(v2, w2, anyTime);

    val bucket1 = testHistogramOptions.findBucket(v1);
    val bucket2 = testHistogramOptions.findBucket(v2);

    assertThat(bucket1)
        .withFailMessage("For this test %s and %s have to be stored in different buckets", v1, v2)
        .isNotEqualTo(bucket2);
    assertThat(w1 < (w2 / MAX_CHECKPOINT_WEIGHT) / 2)
        .withFailMessage("w1 to be omitted has to be less than (0.5*w2)/MaxCheckPointWeight")
        .isTrue();

    val s = h.saveToCheckpoint();
    assertThat(s.getTotalWeight()).isEqualTo(100001.0);
    assertThat(s.getBucketWeights()).hasSize(1);
    assertThat(s.getBucketWeights()).containsKey(bucket2);
    assertThat(s.getBucketWeights().get(bucket2)).isEqualTo(MAX_CHECKPOINT_WEIGHT);
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void testHistogramSaveToCheckpointForMultipleValues() throws Exception {
    val h = new HistogramImpl(testHistogramOptions);
    List<Double> values = Arrays.asList(1.0, 2.0, 3.0);
    List<Double> weights = Arrays.asList(1.0, 10000.0, 50.0);
    for (int i = 0; i < 3; i++) {
      h.addSample(values.get(i), weights.get(i), anyTime);
    }

    List<Integer> buckets = values.stream().map(testHistogramOptions::findBucket).collect(Collectors.toList());
    assertThat(new HashSet<>(buckets))
        .withFailMessage("For this test values %s have to be stored in different buckets", values)
        .hasSize(3);

    val s = h.saveToCheckpoint();
    assertThat(s.getTotalWeight()).isEqualTo(10051.0);
    assertThat(s.getBucketWeights()).hasSize(3);
    assertThat(buckets.stream().map(b -> s.getBucketWeights().get(b)).collect(Collectors.toList()))
        .withFailMessage("For this test values %s has to be stored in different buckets", values)
        .containsExactly(1, 10000, 50);
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void testHistogramLoadFromCheckpoint() throws Exception {
    val checkpoint = HistogramCheckpoint.builder().totalWeight(6.0).bucketWeights(ImmutableMap.of(0, 1, 1, 2)).build();
    val h = new HistogramImpl(testHistogramOptions);
    h.loadFromCheckPoint(checkpoint);
    double totalWeight = Reflect.on(h).get("totalWeight");
    assertThat(totalWeight).isEqualTo(6.0);
    double[] bucketWeights = Reflect.on(h).get("bucketWeight");
    assertThat(bucketWeights[0]).isEqualTo(2.0);
    assertThat(bucketWeights[1]).isEqualTo(4.0);
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void testHistogramLoadFromCheckpointReturnsErrorOnNegativeBucket() throws Exception {
    val checkpoint = HistogramCheckpoint.builder().totalWeight(1.0).bucketWeights(ImmutableMap.of(-1, 1)).build();
    val h = new HistogramImpl(testHistogramOptions);
    assertThatExceptionOfType(IndexOutOfBoundsException.class)
        .isThrownBy(() -> h.loadFromCheckPoint(checkpoint))
        .withMessageContaining("Checkpoint has invalid bucket index");
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void testHistogramLoadFromCheckpointReturnsErrorOnInvalidBucket() throws Exception {
    val checkpoint = HistogramCheckpoint.builder().totalWeight(1.0).bucketWeights(ImmutableMap.of(99, 1)).build();
    val h = new HistogramImpl(testHistogramOptions);
    assertThatExceptionOfType(IndexOutOfBoundsException.class)
        .isThrownBy(() -> h.loadFromCheckPoint(checkpoint))
        .withMessageContaining("Checkpoint has invalid bucket index");
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void testHistogramLoadFromCheckpointReturnsErrorOnNegativeTotalWeight() throws Exception {
    val checkpoint = HistogramCheckpoint.builder().totalWeight(-1.0).bucketWeights(ImmutableMap.of()).build();
    val h = new HistogramImpl(testHistogramOptions);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> h.loadFromCheckPoint(checkpoint))
        .withMessage("Cannot load checkpoint with negative weight -1.0");
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void testHistogramLoadFromCheckpointReturnsErrorOnNullInput() throws Exception {
    val h = new HistogramImpl(testHistogramOptions);
    assertThatNullPointerException()
        .isThrownBy(() -> h.loadFromCheckPoint(null))
        .withMessage("Cannot load from empty checkpoint");
  }
}
