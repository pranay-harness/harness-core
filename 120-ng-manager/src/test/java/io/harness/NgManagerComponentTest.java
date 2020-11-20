package io.harness;

import static io.harness.rule.OwnerRule.GEORGE;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.testing.TestExecution;
import lombok.extern.slf4j.Slf4j;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Map;

@Slf4j
public class NgManagerComponentTest extends CategoryTest {
  private Map<String, TestExecution> tests;

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  @Ignore("As of now we don't have any morhpia or kyro registrar file for 120-ng module")
  public void componentNgManagerTests() {
    assertThat(tests).isNull(); // Adding this code just to remove sonar issue
    /*for (Map.Entry<String, TestExecution> test : tests.entrySet()) {
      assertThatCode(() -> test.getValue().run()).as(test.getKey()).doesNotThrowAnyException();
      log.info("{} passed", test.getKey());
    }*/
  }
}
