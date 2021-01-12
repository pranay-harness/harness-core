package io.harness;

import static io.harness.rule.OwnerRule.ANKUSH;

import static org.assertj.core.api.Assertions.assertThatCode;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.testing.TestExecution;

import com.google.inject.Inject;
import java.util.Map;
import java.util.Map.Entry;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Slf4j
public class NotificationServiceComponentTest extends NotificationServiceTestBase {
  @Inject private Map<String, TestExecution> tests;

  @Test
  @Owner(developers = ANKUSH, intermittent = true)
  @Category(UnitTests.class)
  public void componentNotificationServiceTests() {
    for (Entry<String, TestExecution> test : tests.entrySet()) {
      assertThatCode(() -> test.getValue().run()).as(test.getKey()).doesNotThrowAnyException();
      log.info("{} passed", test.getKey());
    }
  }
}
