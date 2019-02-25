package io.harness.functional;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.CategoryTest;
import io.harness.category.element.FunctionalTests;
import io.harness.framework.Retry;
import io.harness.framework.Setup;
import io.harness.framework.matchers.SettingsAttributeMatcher;
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
import software.wings.beans.SettingAttribute;
import software.wings.beans.User;
import software.wings.helpers.ext.external.comm.handlers.EmailHandler;
import software.wings.service.intfc.SettingsService;

import java.io.IOException;
import javax.ws.rs.core.GenericType;

public abstract class AbstractFunctionalTest extends CategoryTest implements FunctionalTests {
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
  @Inject private EmailHandler emailHandler;
  @Inject private SettingsService settingsService;

  @Getter Account account;

  @Before
  public void testSetup() throws IOException {
    final int MAX_RETRIES = 5;
    final int DELAY_IN_MS = 3000;
    final Retry<Object> retry = new Retry<>(MAX_RETRIES, DELAY_IN_MS);
    account = accountSetupService.ensureAccount();

    delegateExecutor.ensureDelegate(account);

    bearerToken = Setup.getAuthToken("admin@harness.io", "admin");
    retry.executeWithRetry(() -> updateAndGetSettingAttribute(), new SettingsAttributeMatcher<>(), true);
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

  private SettingAttribute updateAndGetSettingAttribute() {
    SettingAttribute settingAttribute = Setup.getEmailConfig(account.getUuid());

    if (settingsService.getByName(
            settingAttribute.getAccountId(), settingAttribute.getAppId(), settingAttribute.getName())
        == null) {
      return settingsService.save(settingAttribute);
    }
    return settingsService.getByName(
        settingAttribute.getAccountId(), settingAttribute.getAppId(), settingAttribute.getName());
  }
}
