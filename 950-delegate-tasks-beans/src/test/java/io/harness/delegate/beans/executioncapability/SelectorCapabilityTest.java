/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
