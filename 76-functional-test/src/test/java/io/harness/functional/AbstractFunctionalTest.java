package io.harness.functional;

import static org.apache.commons.codec.binary.Base64.encodeBase64String;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.FunctionalTests;
import io.harness.framework.Setup;
import io.harness.rest.RestResponse;
import io.harness.rule.FunctionalTestRule;
import io.harness.rule.LifecycleRule;
import io.restassured.RestAssured;
import lombok.Getter;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Account;
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
    Setup.portal();
    RestAssured.useRelaxedHTTPSValidation();
  }

  //  @Inject private AccountGenerator accountGenerator;
  @Inject private DelegateExecutor delegateExecutor;
  //  @Inject OwnerManager ownerManager;
  @Inject private AccountSetupService accountSetupService;

  @Getter Account account;

  @Before
  public void testSetup() throws IOException {
    //    final Seed seed = new Seed(0);
    //    Owners owners = ownerManager.create();

    //    account = accountGenerator.ensurePredefined(seed, owners, GENERIC_TEST);
    account = accountSetupService.ensureAccount();

    delegateExecutor.ensureDelegate(account);

    String basicAuthValue =
        "Basic " + encodeBase64String(String.format("%s:%s", "admin@harness.io", "admin").getBytes());

    GenericType<RestResponse<User>> genericType = new GenericType<RestResponse<User>>() {

    };
    RestResponse<User> userRestResponse =
        Setup.portal().header("Authorization", basicAuthValue).get("/users/login").as(genericType.getType());

    assertThat(userRestResponse).isNotNull();
    User user = userRestResponse.getResource();
    assertThat(user).isNotNull();
    bearerToken = user.getToken();
  }

  protected void resetCache() {
    RestResponse<User> userRestResponse = Setup.portal()
                                              .auth()
                                              .oauth2(bearerToken)
                                              .queryParam("accountId", account.getUuid())
                                              .put("/users/reset-cache")
                                              .as(new GenericType<RestResponse<User>>() {}.getType());
    assertThat(userRestResponse).isNotNull();
  }
}
