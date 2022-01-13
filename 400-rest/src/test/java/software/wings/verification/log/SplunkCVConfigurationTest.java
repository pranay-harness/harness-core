/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.verification.log;

import static io.harness.rule.OwnerRule.KAMAL;
import static io.harness.rule.OwnerRule.PRAVEEN;
import static io.harness.rule.OwnerRule.SOWMYA;

import static software.wings.common.VerificationConstants.CV_24x7_STATE_EXECUTION;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SplunkConfig;
import software.wings.service.impl.splunk.SplunkDataCollectionInfoV2;
import software.wings.sm.StateType;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mockito;

@Slf4j
public class SplunkCVConfigurationTest extends WingsBaseTest {
  private static final String configName = "configName";
  private static final String accountId = "accountId";
  private static final String connectorId = "connectorId";
  private static final String envId = "envId";
  private static final String serviceId = "serviceId";
  private static final StateType stateType = StateType.SPLUNKV2;

  private static final String hostnameField = "hostnameField";
  private static final String query = "query";

  private SplunkCVConfiguration createSplunkConfig() {
    SplunkCVConfiguration config = new SplunkCVConfiguration();
    config.setName(configName);
    config.setAccountId(accountId);
    config.setConnectorId(connectorId);
    config.setEnvId(envId);
    config.setServiceId(serviceId);
    config.setStateType(stateType);
    config.setEnabled24x7(true);

    config.setHostnameField(hostnameField);
    config.setAdvancedQuery(true);
    config.setQuery(query);
    config.setEnabled24x7(true);
    return config;
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testCloneSplunkConfig() {
    SplunkCVConfiguration config = createSplunkConfig();

    SplunkCVConfiguration clonedConfig = (SplunkCVConfiguration) config.deepCopy();

    assertThat(clonedConfig.getName()).isEqualTo(configName);
    assertThat(clonedConfig.getAccountId()).isEqualTo(accountId);
    assertThat(clonedConfig.getConnectorId()).isEqualTo(connectorId);
    assertThat(clonedConfig.getEnvId()).isEqualTo(envId);
    assertThat(clonedConfig.getServiceId()).isEqualTo(serviceId);
    assertThat(clonedConfig.getStateType()).isEqualTo(stateType);
    assertThat(clonedConfig.isEnabled24x7()).isTrue();

    assertThat(clonedConfig.getHostnameField()).isEqualTo(hostnameField);
    assertThat(clonedConfig.isAdvancedQuery()).isTrue();
    assertThat(clonedConfig.getQuery()).isEqualTo(query);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testSplunkDataCollectionInfoCreation() {
    SplunkCVConfiguration splunkCVConfiguration = createSplunkConfig();
    SplunkConfig splunkConfig = SplunkConfig.builder().splunkUrl("test").username("test").build();
    SettingAttribute settingAttribute = Mockito.mock(SettingAttribute.class);
    when(settingAttribute.getValue()).thenReturn(splunkConfig);
    SplunkDataCollectionInfoV2 dataCollectionInfo =
        (SplunkDataCollectionInfoV2) splunkCVConfiguration.toDataCollectionInfo();
    assertThat(dataCollectionInfo.getAccountId()).isEqualTo(splunkCVConfiguration.getAccountId());
    assertThat(dataCollectionInfo.getCvConfigId()).isEqualTo(splunkCVConfiguration.getUuid());
    assertThat(dataCollectionInfo.getStateExecutionId())
        .isEqualTo(CV_24x7_STATE_EXECUTION + "-" + splunkCVConfiguration.getUuid());
    assertThat(dataCollectionInfo.getStartTime()).isNull();
    assertThat(dataCollectionInfo.getEndTime()).isNull();
    assertThat(dataCollectionInfo.getQuery()).isEqualTo(query);
    assertThat(dataCollectionInfo.getHostnameField()).isEqualTo(hostnameField);
    assertThat(dataCollectionInfo.getEnvId()).isEqualTo(envId);
    assertThat(dataCollectionInfo.isAdvancedQuery()).isEqualTo(splunkCVConfiguration.isAdvancedQuery());
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testSplunkDataCollectionInfoCreation_advancedQuery() {
    SplunkCVConfiguration splunkCVConfiguration = createSplunkConfig();
    splunkCVConfiguration.setAdvancedQuery(true);
    SplunkConfig splunkConfig = SplunkConfig.builder().splunkUrl("test").username("test").build();
    SettingAttribute settingAttribute = Mockito.mock(SettingAttribute.class);
    when(settingAttribute.getValue()).thenReturn(splunkConfig);
    SplunkDataCollectionInfoV2 dataCollectionInfo =
        (SplunkDataCollectionInfoV2) splunkCVConfiguration.toDataCollectionInfo();
    assertThat(dataCollectionInfo.getAccountId()).isEqualTo(splunkCVConfiguration.getAccountId());
    assertThat(dataCollectionInfo.getCvConfigId()).isEqualTo(splunkCVConfiguration.getUuid());
    assertThat(dataCollectionInfo.getStateExecutionId())
        .isEqualTo(CV_24x7_STATE_EXECUTION + "-" + splunkCVConfiguration.getUuid());
    assertThat(dataCollectionInfo.getStartTime()).isNull();
    assertThat(dataCollectionInfo.getEndTime()).isNull();
    assertThat(dataCollectionInfo.getQuery()).isEqualTo(query);
    assertThat(dataCollectionInfo.getHostnameField()).isEqualTo(hostnameField);
    assertThat(dataCollectionInfo.isAdvancedQuery()).isTrue();
  }
}
