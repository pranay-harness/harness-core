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

public class LinearHistogramOptionsTest extends CategoryTest {
  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void testLinearHistogramOptions() throws Exception {
    HistogramOptions o = new LinearHistogramOptions(5.0, 0.3, 0.001);
    assertThat(o.getEpsilon()).isEqualTo(0.001);
    assertThat(o.getNumBuckets()).isEqualTo(18);

    assertThat(o.getBucketStart(0)).isEqualTo(0.0);
    assertThat(o.getBucketStart(17)).isEqualTo(5.1);

    assertThat(o.findBucket(-1.0)).isEqualTo(0);
    assertThat(o.findBucket(0.0)).isEqualTo(0);
    assertThat(o.findBucket(1.3)).isEqualTo(4);
    assertThat(o.findBucket(100.0)).isEqualTo(17);
  }
}
