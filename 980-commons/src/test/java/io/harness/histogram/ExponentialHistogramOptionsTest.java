/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.histogram;

import static io.harness.rule.OwnerRule.AVMOHAN;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ExponentialHistogramOptionsTest extends CategoryTest {
  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void testExponentialHistogramOptions() throws Exception {
    HistogramOptions o = new ExponentialHistogramOptions(500.0, 40.0, 1.5, 0.001);
    assertThat(o.getEpsilon()).isEqualTo(0.001);
    assertThat(o.getNumBuckets()).isEqualTo(6);

    assertThat(o.getBucketStart(0)).isEqualTo(0.0);
    assertThat(o.getBucketStart(1)).isEqualTo(40.0);
    assertThat(o.getBucketStart(2)).isEqualTo(100.0);
    assertThat(o.getBucketStart(3)).isEqualTo(190.0);
    assertThat(o.getBucketStart(4)).isEqualTo(325.0);
    assertThat(o.getBucketStart(5)).isEqualTo(527.5);

    assertThat(o.findBucket(-1.0)).isEqualTo(0);
    assertThat(o.findBucket(39.9)).isEqualTo(0);
    assertThat(o.findBucket(40.0)).isEqualTo(1);
    assertThat(o.findBucket(100.0)).isEqualTo(2);
    assertThat(o.findBucket(900.0)).isEqualTo(5);
  }
}
