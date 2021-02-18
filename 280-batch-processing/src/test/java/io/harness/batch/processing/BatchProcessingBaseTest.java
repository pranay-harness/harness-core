package io.harness.batch.processing;

import static io.harness.rule.OwnerRule.SHUBHANSHU;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.LifecycleRule;
import io.harness.rule.Owner;

import lombok.extern.slf4j.Slf4j;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@Slf4j
public abstract class BatchProcessingBaseTest extends CategoryTest {
  @Rule public LifecycleRule lifecycleRule = new LifecycleRule();
  @Rule public BatchProcessingRule batchProcessingRule = new BatchProcessingRule(lifecycleRule.getClosingFactory());

  // Rules are processed first fields, then methods and we need Mockito to inject after guice hence this should
  // be a method rule.
  @Rule
  public MockitoRule mockitoRule() {
    return MockitoJUnit.rule();
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  @Ignore("Placeholder")
  public void batchProcessingTest() {}
}
