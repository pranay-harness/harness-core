package io.harness.managerclient;

import static io.harness.rule.OwnerRule.HARSH;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.executionplan.CIExecutionTest;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.IOException;

public class ManagerClientFactoryTest extends CIExecutionTest {
  private String BASE_URL = "https://localhost:9090/api/";

  @Test
  @Owner(developers = HARSH, intermittent = true)
  @Category(UnitTests.class)
  public void shouldCreateManagerClientFactory() throws IOException {
    ManagerClientFactory managerClientFactory = new ManagerClientFactory(BASE_URL, null);
    assertThat(managerClientFactory.get()).isNotNull();
  }
}