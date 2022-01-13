/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.verification;

import static io.harness.rule.OwnerRule.SOWMYA;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.sm.StateType;
import software.wings.verification.prometheus.PrometheusCVServiceConfiguration;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Slf4j
public class CVConfigurationTest extends WingsBaseTest {
  private static final String configName = "configName";
  private static final String accountId = "accountId";
  private static final String connectorId = "connectorId";
  private static final String envId = "envId";
  private static final String serviceId = "serviceId";
  private static final StateType stateType = StateType.PROMETHEUS;

  private PrometheusCVServiceConfiguration createPrometheusConfig() {
    PrometheusCVServiceConfiguration config = new PrometheusCVServiceConfiguration();
    config.setName(configName);
    config.setAccountId(accountId);
    config.setConnectorId(connectorId);
    config.setEnvId(envId);
    config.setServiceId(serviceId);
    config.setStateType(stateType);
    config.setEnabled24x7(true);
    return config;
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testCloneForUpdate() {
    PrometheusCVServiceConfiguration clonedConfig = new PrometheusCVServiceConfiguration();
    PrometheusCVServiceConfiguration config = createPrometheusConfig();
    config.copy(clonedConfig);

    assertThat(clonedConfig.getName()).isEqualTo(configName);
    assertThat(clonedConfig.getAccountId()).isEqualTo(accountId);
    assertThat(clonedConfig.getConnectorId()).isEqualTo(connectorId);
    assertThat(clonedConfig.getEnvId()).isEqualTo(envId);
    assertThat(clonedConfig.getServiceId()).isEqualTo(serviceId);
    assertThat(clonedConfig.getStateType()).isEqualTo(stateType);
    assertThat(clonedConfig.isEnabled24x7()).isTrue();
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testDeepClone() {
    CVConfiguration config = new CVConfiguration();
    assertThatThrownBy(config::deepCopy).isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testSetAlertThreshold_validThresholdValue() {
    CVConfiguration cvConfiguration = new CVConfiguration();

    cvConfiguration.setAlertThreshold(0);
    assertThat(cvConfiguration.getAlertThreshold()).isEqualTo(0);

    cvConfiguration.setAlertThreshold(0.1);
    assertThat(cvConfiguration.getAlertThreshold()).isEqualTo(0.1);

    cvConfiguration.setAlertThreshold(0.5);
    assertThat(cvConfiguration.getAlertThreshold()).isEqualTo(0.5);

    cvConfiguration.setAlertThreshold(0.99);
    assertThat(cvConfiguration.getAlertThreshold()).isEqualTo(0.99);

    cvConfiguration.setAlertThreshold(1);
    assertThat(cvConfiguration.getAlertThreshold()).isEqualTo(1);
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testSetAlertThreshold_invalidThresholdValue() {
    CVConfiguration cvConfiguration = new CVConfiguration();

    assertThatThrownBy(() -> cvConfiguration.setAlertThreshold(-0.01)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> cvConfiguration.setAlertThreshold(1.01)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testSetNumOfOccurrencesForAlert_validNumOccurrencesAlerts() {
    CVConfiguration cvConfiguration = new CVConfiguration();

    cvConfiguration.setNumOfOccurrencesForAlert(1);
    assertThat(cvConfiguration.getNumOfOccurrencesForAlert()).isEqualTo(1);

    cvConfiguration.setNumOfOccurrencesForAlert(2);
    assertThat(cvConfiguration.getNumOfOccurrencesForAlert()).isEqualTo(2);

    cvConfiguration.setNumOfOccurrencesForAlert(3);
    assertThat(cvConfiguration.getNumOfOccurrencesForAlert()).isEqualTo(3);

    cvConfiguration.setNumOfOccurrencesForAlert(4);
    assertThat(cvConfiguration.getNumOfOccurrencesForAlert()).isEqualTo(4);
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testSetNumOfOccurrencesForAlert_invalidNumOccurrencesAlerts() {
    CVConfiguration cvConfiguration = new CVConfiguration();

    assertThatThrownBy(() -> cvConfiguration.setNumOfOccurrencesForAlert(6))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> cvConfiguration.setNumOfOccurrencesForAlert(0))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> cvConfiguration.setNumOfOccurrencesForAlert(-1))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
