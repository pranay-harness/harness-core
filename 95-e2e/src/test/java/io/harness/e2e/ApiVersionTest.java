package io.harness.e2e;

import static io.harness.rule.OwnerRule.NATARAJA;

import io.harness.category.element.E2ETests;
import io.harness.rule.Owner;
import io.harness.testframework.framework.Setup;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ApiVersionTest extends AbstractE2ETest {
  @Test
  @Owner(developers = NATARAJA)
  @Category(E2ETests.class)
  public void shouldApiReady() {
    Setup.portal().when().get("/version").then().statusCode(200);
  }
}
