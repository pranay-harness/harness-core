package software.wings.service.impl;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static org.apache.cxf.ws.addressing.ContextUtils.generateUUID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static software.wings.beans.Account.Builder.anAccount;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Environment.Builder.anEnvironment;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.AccountType;
import software.wings.beans.LicenseInfo;
import software.wings.beans.Service;
import software.wings.dl.WingsPersistence;
import software.wings.security.encryption.EncryptionUtils;
import software.wings.service.impl.analysis.AnalysisTolerance;
import software.wings.service.intfc.verification.CV24x7DashboardService;
import software.wings.service.intfc.verification.CVConfigurationService;
import software.wings.sm.StateType;
import software.wings.verification.datadog.DatadogCVServiceConfiguration;
import software.wings.verification.newrelic.NewRelicCVServiceConfiguration;

import java.nio.charset.Charset;
import java.util.Map;

/**
 * Created by Praveen
 */
public class CV24x7DashboardServiceTest extends WingsBaseTest {
  @Inject WingsPersistence wingsPersistence;
  @Inject CV24x7DashboardService cv24x7DashboardService;
  @Inject CVConfigurationService cvConfigurationService;

  private String accountId;
  private String appId;
  private String serviceId;
  private String envId;
  private String envName;
  private String connectorId;

  @Before
  public void setup() {
    Account account = anAccount().withAccountName(generateUUID()).build();

    account.setEncryptedLicenseInfo(EncryptionUtils.encrypt(
        LicenseUtils.convertToString(LicenseInfo.builder().accountType(AccountType.PAID).build())
            .getBytes(Charset.forName("UTF-8")),
        null));
    accountId = wingsPersistence.save(account);
    appId = wingsPersistence.save(anApplication().accountId(accountId).name(generateUUID()).build());
    envName = generateUuid();
    connectorId = generateUuid();
    serviceId = wingsPersistence.save(Service.builder().appId(appId).name(generateUuid()).build());
    envId = wingsPersistence.save(anEnvironment().appId(appId).name(envName).build());
  }

  private String createDDCVConfig() {
    DatadogCVServiceConfiguration cvServiceConfiguration =
        DatadogCVServiceConfiguration.builder().metrics("kubernetes.cpu.usage.total,docker.mem.rss").build();
    cvServiceConfiguration.setName(generateUUID());
    cvServiceConfiguration.setConnectorId(connectorId);
    cvServiceConfiguration.setEnvId(envId);
    cvServiceConfiguration.setServiceId(serviceId);
    cvServiceConfiguration.setEnabled24x7(true);
    cvServiceConfiguration.setAnalysisTolerance(AnalysisTolerance.LOW);
    return cvConfigurationService.saveConfiguration(accountId, appId, StateType.DATA_DOG, cvServiceConfiguration);
  }

  private String createNRConfig() {
    NewRelicCVServiceConfiguration cvServiceConfiguration =
        NewRelicCVServiceConfiguration.builder().applicationId(generateUUID()).build();
    cvServiceConfiguration.setName(generateUUID());
    cvServiceConfiguration.setConnectorId(connectorId);
    cvServiceConfiguration.setEnvId(envId);
    cvServiceConfiguration.setServiceId(serviceId);
    cvServiceConfiguration.setEnabled24x7(true);
    cvServiceConfiguration.setAnalysisTolerance(AnalysisTolerance.LOW);
    return cvConfigurationService.saveConfiguration(accountId, appId, StateType.NEW_RELIC, cvServiceConfiguration);
  }

  @Test
  @Category(UnitTests.class)
  public void testGetTagsForCvConfig() throws Exception {
    String cvConfigId = createDDCVConfig();

    // test behavior

    Map<String, Double> result = cv24x7DashboardService.getMetricTags(accountId, appId, cvConfigId, 0l, 0l);

    // assert
    assertEquals("There are 2 tags", 2, result.size());
    assertTrue("Docker is one of the tags", result.containsKey("Docker"));
    assertTrue("Kubernetes is one of the tags", result.containsKey("Kubernetes"));
  }

  @Test
  @Category(UnitTests.class)
  public void testGetTagsForCvConfigNoTags() throws Exception {
    String cvConfigId = createNRConfig();

    // test behavior
    Map<String, Double> result = cv24x7DashboardService.getMetricTags(accountId, appId, cvConfigId, 0l, 0l);

    // assert
    assertEquals("There are 0 tags", 0, result.size());
  }
}
