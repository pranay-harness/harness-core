/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.appdynamics;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.SOWMYA;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.AppDynamicsConfig;
import software.wings.sm.StateType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.Random;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class AppDynamicsDataCollectionInfoV2Test extends WingsBaseTest {
  private AppDynamicsConfig appDynamicsConfig;
  private long appDynamicsApplicationId;
  private long appDynamicsTierId;
  private String accountId;
  private AppDynamicsDataCollectionInfoV2 appDynamicsDataCollectionInfoV2;
  private String url = "http://appdURL.com/";

  @Before
  public void setUp() {
    accountId = generateUuid();
    Random random = new Random();
    appDynamicsApplicationId = random.nextLong();
    appDynamicsTierId = random.nextLong();
    appDynamicsConfig = AppDynamicsConfig.builder().accountId(accountId).controllerUrl(url).build();
    appDynamicsDataCollectionInfoV2 = AppDynamicsDataCollectionInfoV2.builder()
                                          .accountId(accountId)
                                          .appDynamicsConfig(appDynamicsConfig)
                                          .appDynamicsApplicationId(appDynamicsApplicationId)
                                          .appDynamicsTierId(appDynamicsTierId)
                                          .hosts(new HashSet<>())
                                          .hostsToGroupNameMap(new HashMap<>())
                                          .build();
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testBuilder() {
    assertThat(appDynamicsDataCollectionInfoV2.getAppDynamicsConfig()).isEqualTo(appDynamicsConfig);
    assertThat(appDynamicsDataCollectionInfoV2.getAccountId()).isEqualTo(accountId);
    assertThat(appDynamicsDataCollectionInfoV2.getAppDynamicsApplicationId()).isEqualTo(appDynamicsApplicationId);
    assertThat(appDynamicsDataCollectionInfoV2.getAppDynamicsTierId()).isEqualTo(appDynamicsTierId);
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testGetStateType() {
    assertThat(appDynamicsDataCollectionInfoV2.getStateType()).isEqualByComparingTo(StateType.APP_DYNAMICS);
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testGetUrlForValidation() {
    assertThat(appDynamicsDataCollectionInfoV2.getUrlForValidation()).isEqualTo(Optional.of(url));
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testDeepCopy() {
    AppDynamicsDataCollectionInfoV2 copiedConfig =
        (AppDynamicsDataCollectionInfoV2) appDynamicsDataCollectionInfoV2.deepCopy();
    assertThat(copiedConfig.getAppDynamicsConfig()).isEqualTo(appDynamicsConfig);
    assertThat(copiedConfig.getAccountId()).isEqualTo(accountId);
    assertThat(copiedConfig.getAppDynamicsApplicationId()).isEqualTo(appDynamicsApplicationId);
    assertThat(copiedConfig.getAppDynamicsTierId()).isEqualTo(appDynamicsTierId);
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testSetSettingValue() {
    String newAccount = generateUuid();
    appDynamicsConfig.setAccountId(newAccount);

    appDynamicsDataCollectionInfoV2.setAppDynamicsConfig(appDynamicsConfig);

    assertThat(appDynamicsDataCollectionInfoV2.getAppDynamicsConfig().getAccountId()).isEqualTo(newAccount);
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testValidateParams() {
    appDynamicsDataCollectionInfoV2.setAppDynamicsConfig(null);
    assertThatThrownBy(() -> appDynamicsDataCollectionInfoV2.validateParams()).isInstanceOf(NullPointerException.class);
  }
}
