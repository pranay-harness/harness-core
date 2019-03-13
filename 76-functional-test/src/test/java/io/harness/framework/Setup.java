package io.harness.framework;

import static io.restassured.RestAssured.given;
import static org.apache.commons.codec.binary.Base64.encodeBase64String;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.rest.RestResponse;
import io.harness.scm.ScmSecret;
import io.harness.scm.SecretName;
import io.restassured.specification.RequestSpecification;
import software.wings.beans.User;

import javax.ws.rs.core.GenericType;

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
    String basicAuthValue = "Basic " + encodeBase64String(String.format("%s:%s", email, password).getBytes());
    GenericType<RestResponse<User>> genericType = new GenericType<RestResponse<User>>() {};
    RestResponse<User> userRestResponse =
        Setup.portal().header("Authorization", basicAuthValue).get("/users/login").as(genericType.getType());
    assertThat(userRestResponse).isNotNull();
    User user = userRestResponse.getResource();
    assertThat(user).isNotNull();
    return user.getToken();
  }

  public static int signOut(String userId, String bearerToken) {
    return Setup.portal()
        .header("Authorization", "Bearer " + bearerToken)
        .post("/users/" + userId + "/logout")
        .getStatusCode();
  }
}
