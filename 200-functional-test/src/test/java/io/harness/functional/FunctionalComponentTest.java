package io.harness.functional;

import static io.harness.rule.OwnerRule.GEORGE;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.google.inject.Inject;

import io.harness.category.element.FunctionalTests;
import io.harness.rule.Owner;
import io.harness.testing.TestExecution;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Map;
import java.util.Map.Entry;

@Slf4j
public class FunctionalComponentTest extends AbstractFunctionalTest {
  @Inject private Map<String, TestExecution> tests;

  @Test
  @Owner(developers = GEORGE)
  @Category(FunctionalTests.class)
  public void componentFunctionalTests() {
    for (Entry<String, TestExecution> test : tests.entrySet()) {
      assertThatCode(() -> test.getValue().run()).as(test.getKey()).doesNotThrowAnyException();
      log.info("{} passed", test.getKey());
    }
  }
}
