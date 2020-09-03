package io.harness.functional.harnesscli;

import static io.harness.rule.OwnerRule.SHUBHANSHU;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.CliFunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.generator.ApplicationGenerator;
import io.harness.generator.OwnerManager;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.Randomizer.Seed;
import io.harness.rule.Owner;
import io.harness.testframework.restutils.ApplicationRestUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.Application;

import java.util.Iterator;
import java.util.List;

@Slf4j
public class GetApplicationsFunctionalTest extends AbstractFunctionalTest {
  @Inject private OwnerManager ownerManager;
  @Inject private ApplicationGenerator applicationGenerator;
  @Inject private ApplicationRestUtils applicationRestUtils;

  private Application testApplication;

  private final Seed seed = new Seed(0);
  private Owners owners;

  @Before
  public void setUp() {
    owners = ownerManager.create();
    harnesscliHelper.loginToCLI();
  }

  @Inject HarnesscliHelper harnesscliHelper;
  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(CliFunctionalTests.class)
  public void getApplicationsTest() {
    // Running harness get applications before creating a new application
    String command = String.format("harness get applications");
    logger.info("Running harness get applications");
    List<String> cliOutput = null;
    try {
      cliOutput = harnesscliHelper.executeCLICommand(command);
    } catch (Exception IOException) {
      logger.info("Could not read output of terminal command");
      assertThat(false).isTrue();
    }
    assertThat(cliOutput).isNotNull();
    int outputSize = cliOutput.size();

    // Creating new application
    String appName = "Test application harnessCli - " + System.currentTimeMillis();
    Application newApplication = new Application();
    newApplication.setName(appName);
    testApplication = applicationRestUtils.createApplication(bearerToken, getAccount(), newApplication);
    assertThat(testApplication).isNotNull();

    // Running harness get applications after creating a new application
    logger.info("Running harness get applications after creating a new application");
    cliOutput = null;
    try {
      cliOutput = harnesscliHelper.executeCLICommand(command);
    } catch (Exception IOException) {
      logger.info("Could not read output of terminal command");
      assertThat(false).isTrue();
    }
    assertThat(cliOutput).isNotNull();
    int newOutputSize = cliOutput.size();
    assertThat(newOutputSize).isGreaterThanOrEqualTo(outputSize + 1);

    boolean newAppListed = false;
    Iterator<String> iterator = cliOutput.iterator();
    while (iterator.hasNext()) {
      if (iterator.next().contains(testApplication.getAppId())) {
        newAppListed = true;
        break;
      }
    }
    assertThat(newAppListed).isTrue();
    ApplicationRestUtils.deleteApplication(bearerToken, testApplication.getAppId(), getAccount().getUuid());
  }
}