/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.threading;

import static io.harness.rule.OwnerRule.GEORGE;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.exception.CyclicBarrierException;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ConcurrentTest extends CategoryTest {
  @Test(expected = CyclicBarrierException.class)
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testExceptionPropagation() {
    Concurrent.test(1, i -> { throw new CyclicBarrierException(null); });
  }
}
