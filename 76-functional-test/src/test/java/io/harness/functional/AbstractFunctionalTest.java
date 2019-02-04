package io.harness.functional;

import static io.harness.generator.AccountGenerator.Accounts.GENERIC_TEST;
import static io.restassured.RestAssured.given;
import static org.apache.commons.codec.binary.Base64.encodeBase64String;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.FunctionalTests;
import io.harness.generator.AccountGenerator;
import io.harness.generator.OwnerManager;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.Randomizer.Seed;
import io.harness.rule.FunctionalTestRule;
import io.harness.rule.LifecycleRule;
import io.restassured.RestAssured;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Account;
import software.wings.beans.RestResponse;
import software.wings.beans.User;

import java.io.IOException;
import javax.ws.rs.core.GenericType;

public abstract class AbstractFunctionalTest implements FunctionalTests {
  private static final Logger logger = LoggerFactory.getLogger(AbstractFunctionalTest.class);

  protected static String bearerToken;
  @Rule public LifecycleRule lifecycleRule = new LifecycleRule();
  @Rule public FunctionalTestRule rule = new FunctionalTestRule(lifecycleRule.getClosingFactory());

  protected static boolean failedAlready;

  private static Exception previous = new Exception();

  @BeforeClass
  public static void setup() {
    String port = System.getProperty("server.port", "9090");
    RestAssured.port = Integer.valueOf(9090);
    RestAssured.basePath = System.getProperty("server.base", "/api");
    RestAssured.baseURI = System.getProperty("server.host", "https://localhost");

    //    RestAssured.authentication = basic("admin@harness.io","admin");
    RestAssured.useRelaxedHTTPSValidation();
  }

  @Inject private AccountGenerator accountGenerator;
  @Inject private DelegateExecutor delegateExecutor;
  @Inject OwnerManager ownerManager;

  Account account;

  @Before
  public void testSetup() throws IOException {
    final Seed seed = new Seed(0);
    Owners owners = ownerManager.create();

    account = accountGenerator.ensurePredefined(seed, owners, GENERIC_TEST);

    delegateExecutor.ensureDelegate();

    String basicAuthValue =
        "Basic " + encodeBase64String(String.format("%s:%s", "admin@harness.io", "admin").getBytes());

    GenericType<RestResponse<User>> genericType = new GenericType<RestResponse<User>>() {

    };
    RestResponse<User> userRestResponse =
        given().header("Authorization", basicAuthValue).get("/users/login").as(genericType.getType());

    assertThat(userRestResponse).isNotNull();
    User user = userRestResponse.getResource();
    assertThat(user).isNotNull();
    bearerToken = user.getToken();
  }

  protected void resetCache() {
    RestResponse<User> userRestResponse = given()
                                              .auth()
                                              .oauth2(bearerToken)
                                              .queryParam("accountId", account.getUuid())
                                              .put("/users/reset-cache")
                                              .as(new GenericType<RestResponse<User>>() {}.getType());
    assertThat(userRestResponse).isNotNull();
  }
}
