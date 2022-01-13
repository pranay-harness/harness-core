/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service;

import static io.harness.rule.OwnerRule.GARVIT;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.SettingAttribute;
import software.wings.beans.alert.SettingAttributeValidationFailedAlert;
import software.wings.service.intfc.SettingsService;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;

@Slf4j
public class SettingAttributeValidationFailedAlertTest extends WingsBaseTest {
  @Mock SettingsService settingsService;

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testMatches() {
    SettingAttributeValidationFailedAlert s1 = SettingAttributeValidationFailedAlert.builder().settingId("s1").build();
    SettingAttributeValidationFailedAlert s2 = SettingAttributeValidationFailedAlert.builder().settingId("s2").build();
    SettingAttributeValidationFailedAlert s3 =
        SettingAttributeValidationFailedAlert.builder().settingId("s1").settingCategory("category").build();
    assertThat(s1.matches(s2)).isFalse();
    assertThat(s1.matches(s3)).isTrue();
    assertThat(s2.matches(s3)).isFalse();
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testBuildTitle() throws IllegalAccessException {
    String errMsg = "error msg";
    SettingAttributeValidationFailedAlert s1 = SettingAttributeValidationFailedAlert.builder()
                                                   .settingId("s1")
                                                   .settingCategory("category")
                                                   .connectivityError(errMsg)
                                                   .build();
    FieldUtils.writeField(s1, "settingsService", settingsService, true);

    String settingName = "n1";
    doReturn(SettingAttribute.Builder.aSettingAttribute().withName(settingName).build())
        .when(settingsService)
        .get("s1");
    String title = s1.buildTitle();
    assertThat(title).contains(settingName);
    assertThat(title).contains(errMsg);

    SettingAttributeValidationFailedAlert s2 =
        SettingAttributeValidationFailedAlert.builder().settingId("s2").settingCategory("category").build();
    FieldUtils.writeField(s2, "settingsService", settingsService, true);

    doReturn(SettingAttribute.Builder.aSettingAttribute().withName(settingName).build())
        .when(settingsService)
        .get("s2");
    title = s2.buildTitle();
    assertThat(title).contains(settingName);

    doReturn(SettingAttribute.Builder.aSettingAttribute().build()).when(settingsService).get("s1");
    assertThat(s1.buildTitle()).contains(errMsg);
  }
}
