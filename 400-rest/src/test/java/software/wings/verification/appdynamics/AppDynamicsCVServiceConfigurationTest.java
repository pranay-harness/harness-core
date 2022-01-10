/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.verification.appdynamics;

import static io.harness.rule.OwnerRule.SOWMYA;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.sm.StateType;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Slf4j
public class AppDynamicsCVServiceConfigurationTest extends WingsBaseTest {
  private static final String configName = "configName";
  private static final String accountId = "accountId";
  private static final String connectorId = "connectorId";
  private static final String envId = "envId";
  private static final String serviceId = "serviceId";
  private static final StateType stateType = StateType.APP_DYNAMICS;
  private static final String appDynamicsApplicationId = "appDApplicationId";
  private static final String tierId = "tierId";

  private AppDynamicsCVServiceConfiguration createAPMConfig() {
    AppDynamicsCVServiceConfiguration config = new AppDynamicsCVServiceConfiguration();
    config.setName(configName);
    config.setAccountId(accountId);
    config.setConnectorId(connectorId);
    config.setEnvId(envId);
    config.setServiceId(serviceId);
    config.setStateType(stateType);
    config.setEnabled24x7(true);

    config.setAppDynamicsApplicationId(appDynamicsApplicationId);
    config.setTierId(tierId);

    return config;
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testCloneAppDConfig() {
    AppDynamicsCVServiceConfiguration config = createAPMConfig();

    AppDynamicsCVServiceConfiguration clonedConfig = (AppDynamicsCVServiceConfiguration) config.deepCopy();

    assertThat(clonedConfig.getName()).isEqualTo(configName);
    assertThat(clonedConfig.getAccountId()).isEqualTo(accountId);
    assertThat(clonedConfig.getConnectorId()).isEqualTo(connectorId);
    assertThat(clonedConfig.getEnvId()).isEqualTo(envId);
    assertThat(clonedConfig.getServiceId()).isEqualTo(serviceId);
    assertThat(clonedConfig.getStateType()).isEqualTo(stateType);
    assertThat(clonedConfig.isEnabled24x7()).isTrue();

    assertThat(clonedConfig.getAppDynamicsApplicationId()).isEqualTo(appDynamicsApplicationId);
    assertThat(clonedConfig.getTierId()).isEqualTo(tierId);
  }
}
