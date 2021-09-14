/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pcf;

import static io.harness.rule.OwnerRule.ADWAIT;
import static io.harness.rule.OwnerRule.ARVIND;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;

public class PcfUtilsTest extends CategoryTest {
  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testGetRevisionFromServiceName() {
    assertThat(PcfUtils.getRevisionFromServiceName(null)).isEqualTo(-1);
    assertThat(PcfUtils.getRevisionFromServiceName("App")).isEqualTo(-1);
    assertThat(PcfUtils.getRevisionFromServiceName("App__INACTIVE")).isEqualTo(-1);
    assertThat(PcfUtils.getRevisionFromServiceName("App__12")).isEqualTo(12);
  }

  @Test
  @Owner(developers = ARVIND)
  @Category(UnitTests.class)
  public void testEncodeColor() {
    assertThat(PcfUtils.encodeColor("APP")).contains("APP");
    assertThat(PcfUtils.encodeColor(null)).isEqualTo(EMPTY);
  }
}
