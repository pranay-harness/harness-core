package io.harness.functional;

import static io.restassured.RestAssured.given;
import static org.apache.commons.codec.binary.Base64.encodeBase64String;

import io.harness.category.element.FunctionalTests;
import io.harness.rule.FunctionalTestRule;
import io.restassured.RestAssured;
import org.junit.BeforeClass;
import org.junit.Rule;
import software.wings.beans.RestResponse;
import software.wings.beans.User;

import javax.ws.rs.core.GenericType;

public abstract class AbstractFunctionalTest implements FunctionalTests {
  protected static String bearerToken;
  @Rule public FunctionalTestRule rule = new FunctionalTestRule();

  @BeforeClass
  public static void setup() {
    String port = System.getProperty("server.port");
    if (port == null) {
      RestAssured.port = Integer.valueOf(9090);
    } else {
      RestAssured.port = Integer.valueOf(port);
    }

    String basePath = System.getProperty("server.base");
    if (basePath == null) {
      basePath = "/api";
    }
    RestAssured.basePath = basePath;

    String baseHost = System.getProperty("server.host");
    if (baseHost == null) {
      baseHost = "https://localhost";
    }
    RestAssured.baseURI = baseHost;

    //    RestAssured.authentication = basic("admin@harness.io","admin");
    RestAssured.useRelaxedHTTPSValidation();

    // Verify if api is ready
    given().when().get("/version").then().statusCode(200);

    String basicAuthValue =
        "Basic " + encodeBase64String(String.format("%s:%s", "admin@harness.io", "admin").getBytes());

    GenericType<RestResponse<User>> genericType = new GenericType<RestResponse<User>>() {

    };
    RestResponse<User> userRestResponse =
        given().header("Authorization", basicAuthValue).get("/users/login").as(genericType.getType());

    User user = userRestResponse.getResource();
    bearerToken = user.getToken();
  }
}
