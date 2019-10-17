package io.harness.functional.harnesscli;

import static io.harness.rule.OwnerRule.SHUBHANSHU;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.CliFunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.generator.ApplicationGenerator;
import io.harness.generator.ApplicationGenerator.Applications;
import io.harness.generator.OwnerManager;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.Randomizer.Seed;
import io.harness.generator.ServiceGenerator;
import io.harness.rule.OwnerRule.Owner;
import io.harness.testframework.restutils.ServiceRestUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.Application;
import software.wings.beans.Service;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@Slf4j
public class GetServicesFunctionalTest extends AbstractFunctionalTest {
  @Inject private OwnerManager ownerManager;
  @Inject private ApplicationGenerator applicationGenerator;
  @Inject private ServiceGenerator serviceGenerator;

  private Application application;
  private Service testService;
  private String defaultOutput = "No services to show.";

  private final Seed seed = new Seed(0);
  private Owners owners;

  @Before
  public void setUp() {
    owners = ownerManager.create();
    application = applicationGenerator.ensurePredefined(seed, owners, Applications.GENERIC_TEST);
    assertThat(application).isNotNull();
    harnesscliHelper.loginToCLI();
  }

  @Inject HarnesscliHelper harnesscliHelper;
  @Test
  @Owner(emails = SHUBHANSHU)
  @Category(CliFunctionalTests.class)
  public void getServicesTest() {
    // Running harness get services before creating a new service
    String appId = application.getAppId();
    String command = String.format("harness get services -a %s", appId);
    logger.info("Running harness get services");
    List<String> cliOutput = null;
    try {
      cliOutput = harnesscliHelper.executeCLICommand(command);
    } catch (Exception IOException) {
      logger.info("Could not read output of terminal command");
      assertThat(false).isTrue();
    }
    assertThat(cliOutput).isNotNull();
    int outputSize = cliOutput.size();

    // Creating new service
    String serviceName = "Test service harnessCli - " + System.currentTimeMillis();
    testService = new Service();
    testService.setName(serviceName);
    assertThat(testService).isNotNull();
    String testServiceId = ServiceRestUtils.createService(bearerToken, getAccount().getUuid(), appId, testService);
    assertThat(testServiceId).isNotNull();
    application.setServices(Collections.singletonList(testService));

    // Running harness get services after creating a new service
    logger.info("Running harness get services after creating a new service");
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

    boolean newServiceListed = false;
    Iterator<String> iterator = cliOutput.iterator();
    while (iterator.hasNext()) {
      if (iterator.next().contains(testServiceId)) {
        newServiceListed = true;
        break;
      }
    }
    assertThat(newServiceListed).isTrue();
    ServiceRestUtils.deleteService(bearerToken, appId, testServiceId);
  }

  @Test
  @Owner(emails = SHUBHANSHU)
  @Category(CliFunctionalTests.class)
  public void getServicesWithInvalidArgumentsTest() {
    // Running harness get services with invalid appId
    String command = String.format("harness get services -a %s", "INVALID_ID");
    logger.info("Running harness get services with invalid app ID");
    List<String> cliOutput = null;
    try {
      cliOutput = harnesscliHelper.getCLICommandError(command);
    } catch (Exception IOException) {
      logger.info("Could not read output of terminal command");
      assertThat(false).isTrue();
    }
    assertThat(cliOutput).isNotNull();
    assertThat(cliOutput.size()).isEqualTo(1);
    assertThat(cliOutput.get(0).equals(defaultOutput));
  }
}