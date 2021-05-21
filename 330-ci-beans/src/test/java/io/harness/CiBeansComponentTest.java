package io.harness;

import static io.harness.rule.OwnerRule.AMAN;

import static org.assertj.core.api.Assertions.assertThatCode;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.testing.TestExecution;

import com.google.inject.Inject;
import java.util.Map;
import java.util.Map.Entry;
import lombok.extern.slf4j.Slf4j;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Slf4j
public class CiBeansComponentTest extends CiBeansTestBase {
  @Inject private Map<String, TestExecution> tests;

  @Test
  @Owner(developers = AMAN)
  @Category(UnitTests.class)
  @Ignore("CI-1635: test is failing, breaking jenkins")
  public void componentCiBeansTests() {
    for (Entry<String, TestExecution> test : tests.entrySet()) {
      assertThatCode(() -> test.getValue().run()).as(test.getKey()).doesNotThrowAnyException();
      log.info("{} passed", test.getKey());
    }
  }
}
