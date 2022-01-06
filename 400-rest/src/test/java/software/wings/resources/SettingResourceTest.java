/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.resources;

import static io.harness.beans.FeatureName.CUSTOM_MAX_PAGE_SIZE;
import static io.harness.rule.OwnerRule.ANSHUL;
import static io.harness.rule.OwnerRule.TMACARI;

import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.HARNESS_BAMBOO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.category.element.UnitTests;
import io.harness.ff.FeatureFlagService;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.BambooConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.Builder;
import software.wings.beans.SettingAttribute.SettingCategory;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.UsageRestrictionsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.utils.WingsTestConstants;

import java.util.ArrayList;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class SettingResourceTest extends WingsBaseTest {
  @Mock private SecretManager secretManager;
  @Mock private SettingsService settingsService;
  @Mock private UsageRestrictionsService usageRestrictionsService;
  @Mock private FeatureFlagService featureFlagService;

  @InjectMocks private SettingResource settingResource;

  // Tests testSaveSettingAttribute and testUpdateSettingAttribute are there to make sure that no one removes the code
  // setting the decrypted field in SettingValue to true if it comes from rest layer.
  private SettingAttribute settingAttribute;

  @Before
  public void setUp() throws IllegalAccessException {
    BambooConfig bambooConfig = BambooConfig.builder()
                                    .accountId(ACCOUNT_ID)
                                    .bambooUrl(WingsTestConstants.JENKINS_URL)
                                    .username(WingsTestConstants.USER_NAME)
                                    .build();

    settingAttribute = Builder.aSettingAttribute()
                           .withAccountId(ACCOUNT_ID)
                           .withAppId(APP_ID)
                           .withName(HARNESS_BAMBOO + System.currentTimeMillis())
                           .withCategory(SettingCategory.CLOUD_PROVIDER)
                           .withValue(bambooConfig)
                           .build();

    FieldUtils.writeField(settingResource, "settingsService", settingsService, true);
    FieldUtils.writeField(settingResource, "usageRestrictionsService", usageRestrictionsService, true);
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  @Ignore("Will fix this test, currently marking it ignored")
  public void testSaveSettingAttribute() {
    settingResource.save(APP_ID, ACCOUNT_ID, settingAttribute);

    ArgumentCaptor<SettingAttribute> argumentCaptor = ArgumentCaptor.forClass(SettingAttribute.class);
    verify(settingsService).save(argumentCaptor.capture());

    settingAttribute = argumentCaptor.getValue();
    assertThat(settingAttribute.getValue().isDecrypted()).isEqualTo(true);
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  @Ignore("Will fix this test, currently marking it ignored")
  public void testUpdateSettingAttribute() {
    settingResource.update(APP_ID, ACCOUNT_ID, settingAttribute);

    ArgumentCaptor<SettingAttribute> argumentCaptor = ArgumentCaptor.forClass(SettingAttribute.class);
    verify(settingsService).update(argumentCaptor.capture());

    settingAttribute = argumentCaptor.getValue();
    assertThat(settingAttribute.getValue().isDecrypted()).isEqualTo(true);
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testCustomMaxPageSizeIsEnabled() {
    PageRequest<SettingAttribute> pageRequest = new PageRequest<>();
    doReturn(true).when(featureFlagService).isEnabled(CUSTOM_MAX_PAGE_SIZE, ACCOUNT_ID);
    doReturn(new PageResponse()).when(settingsService).list(pageRequest, APP_ID, ENV_ID);
    settingResource.list(
        APP_ID, APP_ID, ENV_ID, ACCOUNT_ID, new ArrayList<>(), false, false, null, 10000, null, null, pageRequest);
    assertThat(pageRequest.getLimit()).isEqualTo("1200");
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testCustomMaxPageSizeIsEnabledButLimitSetInRequest() {
    PageRequest<SettingAttribute> pageRequest = new PageRequest<>();
    pageRequest.setLimit("50");
    doReturn(true).when(featureFlagService).isEnabled(CUSTOM_MAX_PAGE_SIZE, ACCOUNT_ID);
    doReturn(new PageResponse()).when(settingsService).list(pageRequest, APP_ID, ENV_ID);
    settingResource.list(
        APP_ID, APP_ID, ENV_ID, ACCOUNT_ID, new ArrayList<>(), false, false, null, 10000, null, null, pageRequest);
    assertThat(pageRequest.getLimit()).isEqualTo("50");
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testCustomMaxPageSizeIsDisabled() {
    PageRequest<SettingAttribute> pageRequest = new PageRequest<>();
    doReturn(false).when(featureFlagService).isEnabled(CUSTOM_MAX_PAGE_SIZE, ACCOUNT_ID);
    doReturn(new PageResponse()).when(settingsService).list(pageRequest, APP_ID, ENV_ID);
    settingResource.list(
        APP_ID, APP_ID, ENV_ID, ACCOUNT_ID, new ArrayList<>(), false, false, null, 10000, null, null, pageRequest);
    assertThat(pageRequest.getLimit()).isEqualTo(null);
  }
}
