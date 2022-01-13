/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.beans.executioncapability;

import static io.harness.rule.OwnerRule.VUK;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class SelectorCapabilityTest extends CategoryTest {
  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void testFetchCapabilityBasis() {
    Set<String> selectors = Stream.of("a", "b", "c").collect(Collectors.toSet());

    SelectorCapability selectorCapability = SelectorCapability.builder().selectors(selectors).build();

    String fetchCapabilityBasis = selectorCapability.fetchCapabilityBasis();

    assertThat(fetchCapabilityBasis).isNotNull();
    assertThat(fetchCapabilityBasis).isEqualTo("a, b, c");
  }
}
