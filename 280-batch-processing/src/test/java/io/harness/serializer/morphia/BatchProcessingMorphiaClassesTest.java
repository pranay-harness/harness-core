/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.serializer.morphia;

import static io.harness.rule.OwnerRule.GEORGE;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;

public class BatchProcessingMorphiaClassesTest extends CategoryTest {
  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testBatchProcessingClassesModule() {
    new BatchProcessingMorphiaRegistrar().testClassesModule();
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testEventSearchAndList() {
    // MorphiaModule.getInstance().testAutomaticSearch();
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testBatchProcessingImplementationClassesModule() {
    new BatchProcessingMorphiaRegistrar().testImplementationClassesModule();
  }
}
