/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.smtp;

import static io.harness.rule.OwnerRule.VIKAS_M;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.MockitoAnnotations.initMocks;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ng.core.dto.NgSmtpDTO;
import io.harness.ng.core.dto.SmtpConfigDTO;
import io.harness.ng.core.mapper.NgSmtpDTOMapper;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;

import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.Builder;
import software.wings.beans.SettingAttribute.SettingCategory;
import software.wings.helpers.ext.mail.SmtpConfig;
import software.wings.settings.SettingVariableTypes;

import com.google.inject.Inject;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import retrofit2.Call;
import retrofit2.Response;

public class SmtpNgServiceImplTest extends CategoryTest {
  static final String NG_SMTP_SETTINGS_PREFIX = "ngSmtpConfig-";
  private static final String ACCOUNT_ID = "ACCOUNT_ID";
  private static final String USER_NAME = "USER_NAME";
  private static final String APP_ID = "__GLOBAL_APP_ID__";
  private static final String ENV_ID = "__GLOBAL_ENV_ID__";
  private static final String UUID = "UUID";
  private static final String USER_NAME_1 = "USER_NAME_1";
  @Inject @InjectMocks private SmtpNgServiceImpl smtpNgServiceImpl;
  @Mock private NgSMTPSettingsHttpClient ngSMTPSettingsHttpClient;
  private SettingAttribute settingAttribute;

  @Before
  public void setup() {
    initMocks(this);
    SmtpConfig smtpConfig =
        SmtpConfig.builder().username(USER_NAME).fromAddress("noreply@harness.io").host("HOST").build();
    smtpConfig.setType(SettingVariableTypes.SMTP.name());
    settingAttribute = Builder.aSettingAttribute()
                           .withAccountId(ACCOUNT_ID)
                           .withName(NG_SMTP_SETTINGS_PREFIX + USER_NAME)
                           .withAppId(null)
                           .withCategory(SettingCategory.CONNECTOR)
                           .withValue(smtpConfig)
                           .build();
  }

  @Test
  @Owner(developers = VIKAS_M)
  @Category(UnitTests.class)
  public void testGetSmtpSettingsWithoutExistingConfig() throws IOException {
    Call<RestResponse<SettingAttribute>> request = mock(Call.class);
    doReturn(request).when(ngSMTPSettingsHttpClient).getSmtpSettings(ACCOUNT_ID);
    RestResponse<SettingAttribute> mockResponse = new RestResponse<>(settingAttribute);
    doReturn(Response.success(mockResponse)).when(request).execute();
    SmtpConfigDTO smtpConfigDTO =
        SmtpConfigDTO.builder().fromAddress("noreply@harness.io").username(USER_NAME).host("HOST").build();
    NgSmtpDTO ngSmtpDTO = NgSmtpDTO.builder().name(USER_NAME).accountId(ACCOUNT_ID).value(smtpConfigDTO).build();
    assertThat(smtpNgServiceImpl.getSmtpSettings(ACCOUNT_ID)).isEqualTo(ngSmtpDTO);
  }

  @Test
  @Owner(developers = VIKAS_M)
  @Category(UnitTests.class)
  public void testGetSmtpSettingsWithExistingConfig() throws IOException {
    Call<RestResponse<SettingAttribute>> request = mock(Call.class);
    doReturn(request).when(ngSMTPSettingsHttpClient).getSmtpSettings(ACCOUNT_ID);
    RestResponse<SettingAttribute> mockResponse = new RestResponse<>(null);
    doReturn(Response.success(mockResponse)).when(request).execute();
    assertThat(smtpNgServiceImpl.getSmtpSettings(ACCOUNT_ID)).isEqualTo(null);
  }

  @Test
  @Owner(developers = VIKAS_M)
  @Category(UnitTests.class)
  public void testSaveSmtpSettings() throws IOException {
    Call<RestResponse<SettingAttribute>> request = mock(Call.class);
    doReturn(request).when(ngSMTPSettingsHttpClient).saveSmtpSettings(APP_ID, ACCOUNT_ID, settingAttribute);
    RestResponse<SettingAttribute> mockResponse = new RestResponse<>(settingAttribute);
    doReturn(Response.success(mockResponse)).when(request).execute();
    SmtpConfigDTO smtpConfigDTO =
        SmtpConfigDTO.builder().fromAddress("noreply@harness.io").username(USER_NAME).host("HOST").build();
    NgSmtpDTO ngSmtpDTO = NgSmtpDTO.builder().name(USER_NAME).accountId(ACCOUNT_ID).value(smtpConfigDTO).build();
    assertThat(NgSmtpDTOMapper.getSettingAttributeFromNgSmtpDTO(ngSmtpDTO)).isEqualTo(settingAttribute);
    assertThat(smtpNgServiceImpl.saveSmtpSettings(ngSmtpDTO)).isEqualTo(ngSmtpDTO);
  }

  @Test
  @Owner(developers = VIKAS_M)
  @Category(UnitTests.class)
  public void testUpdateSmtpSettings() throws IOException {
    Call<RestResponse<SettingAttribute>> request = mock(Call.class);
    doReturn(request).when(ngSMTPSettingsHttpClient).updateSmtpSettings(UUID, APP_ID, settingAttribute);
    SettingAttribute updatedSettingAttribute = settingAttribute;
    updatedSettingAttribute.setUuid(UUID);
    updatedSettingAttribute.setName(NG_SMTP_SETTINGS_PREFIX + USER_NAME_1);
    RestResponse<SettingAttribute> mockResponse = new RestResponse<>(updatedSettingAttribute);
    doReturn(Response.success(mockResponse)).when(request).execute();
    SmtpConfigDTO smtpConfigDTO =
        SmtpConfigDTO.builder().fromAddress("noreply@harness.io").username(USER_NAME).host("HOST").build();
    NgSmtpDTO ngSmtpDTO =
        NgSmtpDTO.builder().name(USER_NAME_1).uuid(UUID).accountId(ACCOUNT_ID).value(smtpConfigDTO).build();
    assertThat(smtpNgServiceImpl.updateSmtpSettings(ngSmtpDTO)).isEqualTo(ngSmtpDTO);
  }

  @Test
  @Owner(developers = VIKAS_M)
  @Category(UnitTests.class)
  public void testDeleteSmtpSettings() throws IOException {
    Call<RestResponse<Boolean>> request = mock(Call.class);
    doReturn(request).when(ngSMTPSettingsHttpClient).deleteSmtpSettings(ACCOUNT_ID, APP_ID);
    RestResponse<Boolean> mockResponse = new RestResponse<>(true);
    doReturn(Response.success(mockResponse)).when(request).execute();
    assertThat(smtpNgServiceImpl.deleteSmtpSettings(ACCOUNT_ID)).isEqualTo(true);
  }
}
