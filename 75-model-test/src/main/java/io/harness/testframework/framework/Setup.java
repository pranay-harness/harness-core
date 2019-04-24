package io.harness.testframework.framework;

import static io.restassured.RestAssured.given;
import static org.apache.commons.codec.binary.Base64.encodeBase64String;
import static org.junit.Assert.assertNotNull;

import io.harness.rest.RestResponse;
import io.harness.scm.ScmSecret;
import io.harness.scm.SecretName;
import io.restassured.specification.RequestSpecification;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.User;

import java.nio.charset.StandardCharsets;
import javax.ws.rs.core.GenericType;

@Slf4j
public class Setup {
  private static RequestSpecProvider rqProvider = new RequestSpecProvider();

  private static ScmSecret instScmSecret = new ScmSecret();

  public static RequestSpecification portal() {
    return given().spec(rqProvider.useDefaultSpec());
  }

  public static RequestSpecification email() {
    return given().spec(rqProvider.useEmailSpec());
  }

  public static RequestSpecification mailinator() {
    String secret = instScmSecret.decryptToString(new SecretName("mailinator_paid_api_key"));
    return given().spec(rqProvider.useMailinatorSpec(secret));
  }

  public static String getAuthToken(String email, String password) {
    String basicAuthValue =
        "Basic " + encodeBase64String(String.format("%s:%s", email, password).getBytes(StandardCharsets.UTF_8));
    GenericType<RestResponse<User>> genericType = new GenericType<RestResponse<User>>() {};
    RestResponse<User> userRestResponse =
        Setup.portal().header("Authorization", basicAuthValue).get("/users/login").as(genericType.getType());
    assertNotNull(userRestResponse);
    User user = userRestResponse.getResource();
    assertNotNull(user);
    return user.getToken();
  }

  public static int signOut(String userId, String bearerToken) {
    return Setup.portal()
        .header("Authorization", "Bearer " + bearerToken)
        .post("/users/" + userId + "/logout")
        .getStatusCode();
  }
}
