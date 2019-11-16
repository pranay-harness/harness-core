package software.wings.verification;

import static io.harness.rule.OwnerRule.KAMAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static software.wings.common.VerificationConstants.CV_24x7_STATE_EXECUTION;
import static software.wings.sm.StateType.NEW_RELIC;
import static software.wings.sm.StateType.SPLUNKV2;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.OwnerRule.Owner;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.Mockito;
import software.wings.WingsBaseTest;
import software.wings.beans.NewRelicConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SplunkConfig;
import software.wings.service.impl.analysis.DataCollectionInfoV2;
import software.wings.service.impl.newrelic.NewRelicDataCollectionInfoV2;
import software.wings.service.impl.splunk.SplunkDataCollectionInfoV2;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.service.intfc.verification.DataCollectionInfoService;
import software.wings.verification.log.SplunkCVConfiguration;
import software.wings.verification.newrelic.NewRelicCVServiceConfiguration;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Random;
import java.util.UUID;

public class DataCollectionInfoServiceTest extends WingsBaseTest {
  @Mock private SettingsService settingsService;
  @Mock private SecretManager secretManager;
  @Inject private DataCollectionInfoService dataCollectionInfoService;

  @Before
  public void setupTests() throws IllegalAccessException {
    FieldUtils.writeField(dataCollectionInfoService, "settingsService", settingsService, true);
    FieldUtils.writeField(dataCollectionInfoService, "secretManager", secretManager, true);
  }
  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testSplunkDataCollectionInfoCreation() {
    SplunkCVConfiguration splunkCVConfiguration = createSplunkCVConfig();
    Instant startTime = Instant.now().minus(5, ChronoUnit.MINUTES);
    Instant endTime = Instant.now();
    SplunkConfig splunkConfig = SplunkConfig.builder().splunkUrl("test").username("test").build();
    SettingAttribute settingAttribute = Mockito.mock(SettingAttribute.class);
    when(settingAttribute.getValue()).thenReturn(splunkConfig);
    when(settingsService.get(splunkCVConfiguration.getConnectorId())).thenReturn(settingAttribute);
    DataCollectionInfoV2 dataCollectionInfo =
        dataCollectionInfoService.create(splunkCVConfiguration, startTime, endTime);
    assertThat(dataCollectionInfo.getAccountId()).isEqualTo(splunkCVConfiguration.getAccountId());
    assertThat(dataCollectionInfo.getCvConfigId()).isEqualTo(splunkCVConfiguration.getUuid());
    assertThat(dataCollectionInfo.getStateExecutionId())
        .isEqualTo(CV_24x7_STATE_EXECUTION + "-" + splunkCVConfiguration.getUuid());
    assertThat(dataCollectionInfo.getStartTime()).isEqualTo(startTime);
    assertThat(dataCollectionInfo.getEndTime()).isEqualTo(endTime);
    assertThat(dataCollectionInfo instanceof SplunkDataCollectionInfoV2).isTrue();
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testNewRelicDataCollectionInfoCreation() {
    NewRelicCVServiceConfiguration newRelicCVConfig = createNewRelicCVConfig();
    Instant startTime = Instant.now().minus(5, ChronoUnit.MINUTES);
    Instant endTime = Instant.now();
    NewRelicConfig newRelicConfig = NewRelicConfig.builder().newRelicUrl("test").apiKey("test".toCharArray()).build();
    SettingAttribute settingAttribute = Mockito.mock(SettingAttribute.class);
    when(settingAttribute.getValue()).thenReturn(newRelicConfig);
    when(settingsService.get(newRelicCVConfig.getConnectorId())).thenReturn(settingAttribute);
    DataCollectionInfoV2 dataCollectionInfo = dataCollectionInfoService.create(newRelicCVConfig, startTime, endTime);
    assertThat(dataCollectionInfo.getAccountId()).isEqualTo(newRelicCVConfig.getAccountId());
    assertThat(dataCollectionInfo.getCvConfigId()).isEqualTo(newRelicCVConfig.getUuid());
    assertThat(dataCollectionInfo.getStateExecutionId())
        .isEqualTo(CV_24x7_STATE_EXECUTION + "-" + newRelicCVConfig.getUuid());
    assertThat(dataCollectionInfo.getStartTime()).isEqualTo(startTime);
    assertThat(dataCollectionInfo.getEndTime()).isEqualTo(endTime);
    assertThat(dataCollectionInfo instanceof NewRelicDataCollectionInfoV2).isTrue();
  }

  private SplunkCVConfiguration createSplunkCVConfig() {
    SplunkCVConfiguration splunkCVConfiguration = new SplunkCVConfiguration();
    splunkCVConfiguration.setQuery("*exception*");
    splunkCVConfiguration.setName("Config 1");
    splunkCVConfiguration.setHostnameField("splunk_hostname");
    splunkCVConfiguration.setAdvancedQuery(false);
    splunkCVConfiguration.setAppId(UUID.randomUUID().toString());
    splunkCVConfiguration.setEnvId(UUID.randomUUID().toString());
    splunkCVConfiguration.setServiceId(UUID.randomUUID().toString());
    splunkCVConfiguration.setEnabled24x7(true);
    splunkCVConfiguration.setConnectorId(UUID.randomUUID().toString());
    splunkCVConfiguration.setBaselineStartMinute(100);
    splunkCVConfiguration.setBaselineEndMinute(200);
    splunkCVConfiguration.setAlertEnabled(false);
    splunkCVConfiguration.setAlertThreshold(0.1);
    splunkCVConfiguration.setStateType(SPLUNKV2);
    return splunkCVConfiguration;
  }

  private NewRelicCVServiceConfiguration createNewRelicCVConfig() {
    NewRelicCVServiceConfiguration newRelicCVServiceConfiguration = new NewRelicCVServiceConfiguration();
    newRelicCVServiceConfiguration.setApplicationId(String.valueOf(new Random().nextLong()));
    newRelicCVServiceConfiguration.setName("Config 1");
    newRelicCVServiceConfiguration.setAppId(UUID.randomUUID().toString());
    newRelicCVServiceConfiguration.setEnvId(UUID.randomUUID().toString());
    newRelicCVServiceConfiguration.setServiceId(UUID.randomUUID().toString());
    newRelicCVServiceConfiguration.setEnabled24x7(true);
    newRelicCVServiceConfiguration.setConnectorId(UUID.randomUUID().toString());
    newRelicCVServiceConfiguration.setAlertEnabled(false);
    newRelicCVServiceConfiguration.setAlertThreshold(0.1);
    newRelicCVServiceConfiguration.setStateType(NEW_RELIC);
    return newRelicCVServiceConfiguration;
  }
}
