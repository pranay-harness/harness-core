package io.harness.connector;

import static io.harness.rule.OwnerRule.ABHINAV;

import static org.assertj.core.api.Assertions.assertThatCode;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.testing.TestExecution;

import com.google.inject.Inject;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Slf4j
public class ConnectorComponentTest extends ConnectorsTestBase {
  @Inject private Map<String, TestExecution> tests;

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  @Ignore("TODO: Prashant this require registration from Orch Beans but it in no way depends on orch beans")
  public void componentConnectorTests() {
    for (Map.Entry<String, TestExecution> test : tests.entrySet()) {
      assertThatCode(() -> test.getValue().run()).as(test.getKey()).doesNotThrowAnyException();
      log.info("{} passed", test.getKey());
    }
  }
}
