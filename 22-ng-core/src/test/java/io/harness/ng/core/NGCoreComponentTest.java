package io.harness.ng.core;

import static io.harness.rule.OwnerRule.ARCHIT;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.testing.TestExecution;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Map;

@Slf4j
public class NGCoreComponentTest extends NGCoreTestBase {
  @Inject private Map<String, TestExecution> tests;

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void componentNGCoreTests() {
    for (Map.Entry<String, TestExecution> test : tests.entrySet()) {
      assertThatCode(() -> test.getValue().run()).as(test.getKey()).doesNotThrowAnyException();
    }
  }
}
