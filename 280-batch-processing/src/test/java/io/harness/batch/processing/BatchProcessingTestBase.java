/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing;

import io.harness.CategoryTest;
import io.harness.rule.LifecycleRule;

import lombok.extern.slf4j.Slf4j;
import org.junit.Rule;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@Slf4j
public abstract class BatchProcessingTestBase extends CategoryTest {
  @Rule public LifecycleRule lifecycleRule = new LifecycleRule();
  @Rule public BatchProcessingRule batchProcessingRule = new BatchProcessingRule(lifecycleRule.getClosingFactory());

  // Rules are processed first fields, then methods and we need Mockito to inject after guice hence this should
  // be a method rule.
  @Rule
  public MockitoRule mockitoRule() {
    return MockitoJUnit.rule();
  }
}
