package io.harness.functional;

import static io.restassured.RestAssured.given;

import io.harness.category.element.FunctionalTests;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ApiVersionTest extends AbstractFunctionalTest {
  @Test
  @Category(FunctionalTests.class)
  public void shouldApiReady() {
    given().when().get("/version").then().statusCode(200);
  }
}
