package io.harness.functional;

import static io.restassured.RestAssured.given;

import org.junit.Ignore;
import org.junit.Test;

public class ApiVersionTest extends AbstractFunctionalTest {
  @Test
  @Ignore
  public void shouldApiReady() {
    given().when().get("/version").then().statusCode(200);
  }
}
