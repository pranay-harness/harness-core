package io.harness;

import static io.harness.rule.OwnerRule.GEORGE;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.runners.GuiceRunner;
import io.harness.runners.ModuleProvider;
import io.harness.testing.TestExecution;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import java.util.Map;
import java.util.Map.Entry;

@RunWith(GuiceRunner.class)
@ModuleProvider(OrchestrationModuleListProvider.class)
@Slf4j
public class OrchestrationComponentTest extends OrchestrationTest {
  @Inject private Map<String, TestExecution> tests;

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void componentOrchestrationTests() {
    for (Entry<String, TestExecution> test : tests.entrySet()) {
      assertThatCode(() -> test.getValue().run()).as(test.getKey()).doesNotThrowAnyException();
      logger.info("{} passed", test.getKey());
    }
  }
}