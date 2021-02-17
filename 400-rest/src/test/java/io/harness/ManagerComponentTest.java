package io.harness;

import static io.harness.rule.OwnerRule.GEORGE;

import static org.assertj.core.api.Assertions.assertThatCode;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.testing.TestExecution;

import software.wings.WingsBaseTest;

import com.google.inject.Inject;
import java.util.Map;
import java.util.Map.Entry;
import lombok.extern.slf4j.Slf4j;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Slf4j
@Ignore("TODO: This test is failing in bazel. Changes are required from the owner to make it work in bazel")
public class ManagerComponentTest extends WingsBaseTest {
  @Inject private Map<String, TestExecution> tests;

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void componentManagerTests() {
    for (Entry<String, TestExecution> test : tests.entrySet()) {
      assertThatCode(() -> test.getValue().run()).as(test.getKey()).doesNotThrowAnyException();
      log.info("{} passed", test.getKey());
    }
  }
}
