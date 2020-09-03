package io.harness.functional;

import static io.harness.rule.OwnerRule.SRINIVAS;

import io.harness.category.element.FunctionalTests;
import io.harness.rule.Owner;
import io.harness.testframework.framework.Setup;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ApiVersionTest extends AbstractFunctionalTest {
  @Test
  @Owner(developers = SRINIVAS)
  @Category(FunctionalTests.class)
  public void shouldApiReady() {
    Setup.portal().when().get("/version").then().statusCode(200);
  }
}
