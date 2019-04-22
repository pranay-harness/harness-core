package io.harness.functional;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.CategoryTest;
import io.harness.category.element.FunctionalTests;
import io.harness.framework.DelegateExecutor;
import io.harness.framework.Setup;
import io.harness.rest.RestResponse;
import io.harness.rule.FunctionalTestRule;
import io.harness.rule.LifecycleRule;
import io.harness.utils.FileUtils;
import io.restassured.RestAssured;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import software.wings.beans.Account;
import software.wings.beans.User;

import java.io.IOException;
import javax.ws.rs.core.GenericType;

@Slf4j
public abstract class AbstractFunctionalTest extends CategoryTest implements FunctionalTests {
  protected static String bearerToken;
  @Rule public LifecycleRule lifecycleRule = new LifecycleRule();
  @Rule public FunctionalTestRule rule = new FunctionalTestRule(lifecycleRule.getClosingFactory());

  @BeforeClass
  public static void setup() {
    Setup.portal();
    RestAssured.useRelaxedHTTPSValidation();
  }

  //  @Inject private AccountGenerator accountGenerator;
  @Inject private DelegateExecutor delegateExecutor;
  //  @Inject OwnerManager ownerManager;
  @Inject private AccountSetupService accountSetupService;
  @Getter static Account account;

  @Before
  public void testSetup() throws IOException {
    account = accountSetupService.ensureAccount();
    delegateExecutor.ensureDelegate(account);
    bearerToken = Setup.getAuthToken("admin@harness.io", "admin");
    logger.info("Basic setup completed");
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

  @AfterClass
  public static void cleanup() {
    FileUtils.deleteModifiedConfig();
    logger.info("All tests exit");
  }
}
