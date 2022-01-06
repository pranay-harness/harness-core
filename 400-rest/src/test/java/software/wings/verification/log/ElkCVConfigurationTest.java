/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.verification.log;

import static io.harness.rule.OwnerRule.KAMAL;
import static io.harness.rule.OwnerRule.SOWMYA;

import static software.wings.common.VerificationConstants.CV_24x7_STATE_EXECUTION;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.ElkConfig;
import software.wings.beans.SettingAttribute;
import software.wings.service.impl.elk.ElkDataCollectionInfoV2;
import software.wings.service.impl.elk.ElkQueryType;
import software.wings.sm.StateType;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mockito;

@Slf4j
public class ElkCVConfigurationTest extends WingsBaseTest {
  private static final String configName = "configName";
  private static final String accountId = "accountId";
  private static final String connectorId = "connectorId";
  private static final String envId = "envId";
  private static final String serviceId = "serviceId";
  private static final StateType stateType = StateType.ELK;

  private static final ElkQueryType queryType = ElkQueryType.MATCH;
  private static final String index = "index";
  private static final String hostnameField = "hostnameField";
  private static final String messageField = "messageField";
  private static final String query = "query";
  private static final String timestampField = "timestampField";
  private static final String timestampFormat = "timestampFormat";

  private ElkCVConfiguration createElkConfig() {
    ElkCVConfiguration config = new ElkCVConfiguration();
    config.setName(configName);
    config.setAccountId(accountId);
    config.setConnectorId(connectorId);
    config.setEnvId(envId);
    config.setServiceId(serviceId);
    config.setStateType(stateType);
    config.setEnabled24x7(true);

    config.setIndex(index);
    config.setHostnameField(hostnameField);
    config.setMessageField(messageField);
    config.setQuery(query);
    config.setTimestampFormat(timestampFormat);
    config.setTimestampField(timestampField);

    return config;
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testCloneElkConfig() {
    ElkCVConfiguration config = createElkConfig();

    ElkCVConfiguration clonedConfig = (ElkCVConfiguration) config.deepCopy();

    assertThat(clonedConfig.getName()).isEqualTo(configName);
    assertThat(clonedConfig.getAccountId()).isEqualTo(accountId);
    assertThat(clonedConfig.getConnectorId()).isEqualTo(connectorId);
    assertThat(clonedConfig.getEnvId()).isEqualTo(envId);
    assertThat(clonedConfig.getServiceId()).isEqualTo(serviceId);
    assertThat(clonedConfig.getStateType()).isEqualTo(stateType);
    assertThat(clonedConfig.isEnabled24x7()).isTrue();

    assertThat(clonedConfig.getIndex()).isEqualTo(index);
    assertThat(clonedConfig.getHostnameField()).isEqualTo(hostnameField);
    assertThat(clonedConfig.getMessageField()).isEqualTo(messageField);
    assertThat(clonedConfig.getQuery()).isEqualTo(query);
    assertThat(clonedConfig.getTimestampField()).isEqualTo(timestampField);
    assertThat(clonedConfig.getTimestampFormat()).isEqualTo(timestampFormat);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testElkDataCollectionInfoCreation() {
    ElkCVConfiguration elkCVConfig = createElkConfig();
    ElkConfig elkConfig = ElkConfig.builder().elkUrl("test").build();
    SettingAttribute settingAttribute = Mockito.mock(SettingAttribute.class);
    when(settingAttribute.getValue()).thenReturn(elkConfig);
    ElkDataCollectionInfoV2 dataCollectionInfo = (ElkDataCollectionInfoV2) elkCVConfig.toDataCollectionInfo();
    assertThat(dataCollectionInfo.getAccountId()).isEqualTo(elkCVConfig.getAccountId());
    assertThat(dataCollectionInfo.getCvConfigId()).isEqualTo(elkCVConfig.getUuid());
    assertThat(dataCollectionInfo.getStateExecutionId())
        .isEqualTo(CV_24x7_STATE_EXECUTION + "-" + elkCVConfig.getUuid());
    assertThat(dataCollectionInfo.getStartTime()).isNull();
    assertThat(dataCollectionInfo.getEndTime()).isNull();
    assertThat(dataCollectionInfo.getQuery()).isEqualTo(query);
    assertThat(dataCollectionInfo.getHostnameField()).isEqualTo(hostnameField);
    assertThat(dataCollectionInfo.getIndices()).isEqualTo(index);
    assertThat(dataCollectionInfo.getTimestampFieldFormat()).isEqualTo(timestampFormat);
    assertThat(dataCollectionInfo.getTimestampField()).isEqualTo(timestampField);
    assertThat(dataCollectionInfo.getMessageField()).isEqualTo(messageField);
  }
}
