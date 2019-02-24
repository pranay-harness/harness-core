package io.harness.framework;

import static io.restassured.RestAssured.given;
import static org.apache.commons.codec.binary.Base64.encodeBase64String;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;

import io.harness.rest.RestResponse;
import io.restassured.specification.RequestSpecification;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.Category;
import software.wings.beans.User;
import software.wings.helpers.ext.mail.SmtpConfig;

import javax.ws.rs.core.GenericType;

public class Setup {
  private static RequestSpecProvider rqProvider = new RequestSpecProvider();

  public static RequestSpecification portal() {
    return given().spec(rqProvider.useDefaultSpec());
  }

  public static RequestSpecification email() {
    return given().spec(rqProvider.useEmailSpec());
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

  public static SettingAttribute getEmailConfig(String accountId) {
    SmtpConfig smtpConfig =
        SmtpConfig.builder()
            .host("smtp.sendgrid.net")
            .port(465)
            .username("apikey")
            .password("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".toCharArray())
            .useSSL(true)
            .fromAddress("automation@harness.io")
            .accountId(accountId)
            .build();

    return aSettingAttribute()
        .withCategory(Category.CONNECTOR)
        .withName("EMAIL")
        .withAccountId(accountId)
        .withValue(smtpConfig)
        .build();
  }
}
