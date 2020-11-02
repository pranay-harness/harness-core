package io.harness;

import static io.harness.rule.OwnerRule.RAGHU;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.testing.TestExecution;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Map;
import java.util.Map.Entry;

@Slf4j
public class VerificationComponentTest extends VerificationBaseTest {
  @Inject private Map<String, TestExecution> tests;

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void componentVerificationTests() {
    for (Entry<String, TestExecution> test : tests.entrySet()) {
      assertThatCode(() -> test.getValue().run()).as(test.getKey()).doesNotThrowAnyException();
      log.info("{} passed", test.getKey());
    }
  }
}
