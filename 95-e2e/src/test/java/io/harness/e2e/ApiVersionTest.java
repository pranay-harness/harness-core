package io.harness.e2e;

import io.harness.category.element.E2ETests;
import io.harness.testframework.framework.Setup;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ApiVersionTest extends AbstractE2ETest {
  @Test
  @Category(E2ETests.class)
  public void shouldApiReady() {
    Setup.portal().when().get("/version").then().statusCode(200);
  }
}
