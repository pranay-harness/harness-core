package io.harness.framework;

import static io.restassured.RestAssured.given;

import io.restassured.specification.RequestSpecification;

public class Setup {
  private static RequestSpecProvider rqProvider = new RequestSpecProvider();

  public static RequestSpecification portal() {
    return given().spec(rqProvider.useDefaultSpec());
  }

  public static RequestSpecification email() {
    return given().spec(rqProvider.useEmailSpec());
  }
}
