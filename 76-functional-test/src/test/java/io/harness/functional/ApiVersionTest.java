package io.harness.functional;

import io.harness.category.element.FunctionalTests;
import io.harness.framework.Setup;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ApiVersionTest extends AbstractFunctionalTest {
  @Test
  @Category(FunctionalTests.class)
  public void shouldApiReady() {
    Setup.portal().when().get("/version").then().statusCode(200);
  }
}
