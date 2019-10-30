package io.harness.event;

import static io.harness.persistence.HQuery.excludeAuthority;
import static org.apache.cxf.ws.addressing.ContextUtils.generateUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static software.wings.beans.AccountStatus.ACTIVE;
import static software.wings.beans.AccountType.COMMUNITY;
import static software.wings.beans.AccountType.PAID;
import static software.wings.common.VerificationConstants.CV_META_DATA;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.event.model.GenericEvent;
import io.harness.event.usagemetrics.UsageMetricsService;
import io.harness.metrics.HarnessMetricRegistry;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.Account.Builder;
import software.wings.beans.LicenseInfo;
import software.wings.dl.WingsPersistence;
import software.wings.sm.StateType;
import software.wings.verification.CVConfiguration;
import software.wings.verification.datadog.DatadogCVServiceConfiguration;
import software.wings.verification.newrelic.NewRelicCVServiceConfiguration;

import java.util.Arrays;
import java.util.List;

public class UsageMetricsServiceTest extends WingsBaseTest {
  @Mock private HarnessMetricRegistry harnessMetricRegistry;

  @Inject UsageMetricsService usageMetricsService;

  @Inject WingsPersistence wingsPersistence;

  private String envId;
  private String serviceId;
  private String appId;
  private String connectorId;
  private String accountId;
  private String accountIdCommunity;

  @Before
  public void setup() throws IllegalAccessException {
    accountId = generateUUID();
    accountIdCommunity = generateUUID();
    envId = generateUUID();
    serviceId = generateUUID();
    appId = generateUUID();
    connectorId = generateUUID();
    MockitoAnnotations.initMocks(this);
    FieldUtils.writeField(usageMetricsService, "harnessMetricRegistry", harnessMetricRegistry, true);
  }

  private void setBasicInfo(CVConfiguration cvServiceConfiguration, StateType stateType, String name) {
    cvServiceConfiguration.setStateType(stateType);
    cvServiceConfiguration.setAccountId(accountId);
    cvServiceConfiguration.setServiceId(serviceId);
    cvServiceConfiguration.setConnectorId(connectorId);
    cvServiceConfiguration.setEnvId(envId);
    cvServiceConfiguration.setAppId(appId);
    cvServiceConfiguration.setEnabled24x7(true);
    cvServiceConfiguration.setName(name);
  }

  @Test
  @Category(UnitTests.class)
  public void testEmitCVMetricsHappyCase() {
    Account account = Builder.anAccount()
                          .withAccountName("test")
                          .withLicenseInfo(LicenseInfo.builder().accountStatus(ACTIVE).accountType(PAID).build())
                          .withUuid(accountId)
                          .build();
    wingsPersistence.save(account);

    NewRelicCVServiceConfiguration nrConfig = NewRelicCVServiceConfiguration.builder().applicationId("appId").build();
    setBasicInfo(nrConfig, StateType.NEW_RELIC, "nrConfig");
    DatadogCVServiceConfiguration ddConfig =
        DatadogCVServiceConfiguration.builder().datadogServiceName("testService").build();
    setBasicInfo(ddConfig, StateType.DATA_DOG, "ddConfig");

    wingsPersistence.save(Arrays.asList(nrConfig, ddConfig));

    doNothing().when(harnessMetricRegistry).recordGaugeValue(any(), any(String[].class), anyInt());
    usageMetricsService.checkUsageMetrics();
    ArgumentCaptor<String> taskCaptorName = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String[]> taskCaptorParams = ArgumentCaptor.forClass(String[].class);
    ArgumentCaptor<Double> taskCaptorValue = ArgumentCaptor.forClass(Double.class);

    verify(harnessMetricRegistry, times(2))
        .recordGaugeValue(taskCaptorName.capture(), taskCaptorParams.capture(), taskCaptorValue.capture());

    String[] params = taskCaptorParams.getValue();

    assertThat(CV_META_DATA).isEqualTo(taskCaptorName.getValue());
    assertThat(params.length).isEqualTo(3);
    assertThat(taskCaptorValue.getValue().intValue()).isEqualTo(1);

    List<GenericEvent> events = wingsPersistence.createQuery(GenericEvent.class, excludeAuthority).asList();
    assertThat(events).isEmpty();
  }

  @Test
  @Category(UnitTests.class)
  public void testEmitCVMetricsOnePaidOneCommunity() {
    Account account = Builder.anAccount()
                          .withAccountName("test")
                          .withLicenseInfo(LicenseInfo.builder().accountStatus(ACTIVE).accountType(PAID).build())
                          .withUuid(accountId)
                          .build();
    Account comnunityAccount =
        Builder.anAccount()
            .withAccountName("test")
            .withLicenseInfo(LicenseInfo.builder().accountStatus(ACTIVE).accountType(COMMUNITY).build())
            .withUuid(accountIdCommunity)
            .build();
    wingsPersistence.save(Arrays.asList(account, comnunityAccount));

    NewRelicCVServiceConfiguration nrConfig = NewRelicCVServiceConfiguration.builder().applicationId("appId").build();
    setBasicInfo(nrConfig, StateType.NEW_RELIC, "nrConfig");
    DatadogCVServiceConfiguration ddConfig =
        DatadogCVServiceConfiguration.builder().datadogServiceName("testService").build();
    setBasicInfo(ddConfig, StateType.DATA_DOG, "ddConfig");

    wingsPersistence.save(Arrays.asList(nrConfig, ddConfig));

    nrConfig.setAccountId(comnunityAccount.getUuid());
    nrConfig.setUuid("communityId");
    wingsPersistence.save(nrConfig);

    doNothing().when(harnessMetricRegistry).recordGaugeValue(any(), any(String[].class), anyInt());
    usageMetricsService.checkUsageMetrics();
    ArgumentCaptor<String> taskCaptorName = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String[]> taskCaptorParams = ArgumentCaptor.forClass(String[].class);
    ArgumentCaptor<Double> taskCaptorValue = ArgumentCaptor.forClass(Double.class);

    // validate that this is called only for 2 configs and not 3 since the 3rd one is for a community account
    verify(harnessMetricRegistry, times(2))
        .recordGaugeValue(taskCaptorName.capture(), taskCaptorParams.capture(), taskCaptorValue.capture());
  }
}
