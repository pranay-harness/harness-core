/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.cloudProvider;

import static io.harness.rule.OwnerRule.ABOSII;
import static io.harness.rule.OwnerRule.IGOR;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.utils.RequestField;

import software.wings.WingsBaseTest;
import software.wings.beans.PcfConfig;
import software.wings.beans.SettingAttribute;
import software.wings.graphql.datafetcher.secrets.UsageScopeController;
import software.wings.graphql.schema.mutation.cloudProvider.QLPcfCloudProviderInput;
import software.wings.graphql.schema.mutation.cloudProvider.QLUpdatePcfCloudProviderInput;

import java.sql.SQLException;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class PcfDataFetcherHelperTest extends WingsBaseTest {
  private static final String NAME = "NAME";
  private static final String URL = "URL";
  private static final String USERNAME = "username";
  private static final String PASSWORD = "password";
  private static final String ACCOUNT_ID = "777";

  @Mock private UsageScopeController usageScopeController;

  @InjectMocks private PcfDataFetcherHelper helper = new PcfDataFetcherHelper();

  @Before
  public void setup() throws SQLException {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = IGOR)
  @Category(UnitTests.class)
  public void toSettingAttributeReturnValue() {
    QLPcfCloudProviderInput input = QLPcfCloudProviderInput.builder()
                                        .name(RequestField.ofNullable(NAME))
                                        .endpointUrl(RequestField.ofNullable(URL))
                                        .userName(RequestField.ofNullable(USERNAME))
                                        .userNameSecretId(RequestField.ofNull())
                                        .passwordSecretId(RequestField.ofNullable(PASSWORD))
                                        .skipValidation(RequestField.ofNullable(Boolean.TRUE))
                                        .build();

    SettingAttribute setting = helper.toSettingAttribute(input, ACCOUNT_ID);

    assertThat(setting).isNotNull();
    assertThat(setting.getName()).isEqualTo(NAME);
    assertThat(setting.getValue()).isInstanceOf(PcfConfig.class);
    PcfConfig config = (PcfConfig) setting.getValue();
    assertThat(config.getEndpointUrl()).isEqualTo(URL);
    assertThat(config.getUsername()).isEqualTo(USERNAME.toCharArray());
    assertThat(config.getEncryptedPassword()).isEqualTo(PASSWORD);
    assertThat(config.isSkipValidation()).isEqualTo(Boolean.TRUE);
  }

  @Test
  @Owner(developers = IGOR)
  @Category(UnitTests.class)
  public void toSettingAttributeWithEmptyInput() {
    QLPcfCloudProviderInput input = QLPcfCloudProviderInput.builder()
                                        .name(RequestField.ofNull())
                                        .endpointUrl(RequestField.ofNull())
                                        .userName(RequestField.ofNull())
                                        .userNameSecretId(RequestField.ofNullable(USERNAME))
                                        .passwordSecretId(RequestField.ofNull())
                                        .skipValidation(RequestField.ofNull())
                                        .build();

    SettingAttribute setting = helper.toSettingAttribute(input, ACCOUNT_ID);

    assertThat(setting).isNotNull();
  }

  @Test
  @Owner(developers = IGOR)
  @Category(UnitTests.class)
  public void updateSettingAttributePerformance() {
    QLUpdatePcfCloudProviderInput input = QLUpdatePcfCloudProviderInput.builder()
                                              .name(RequestField.ofNullable(NAME))
                                              .endpointUrl(RequestField.ofNullable(URL))
                                              .userName(RequestField.ofNullable(USERNAME))
                                              .userNameSecretId(RequestField.ofNull())
                                              .passwordSecretId(RequestField.ofNullable(PASSWORD))
                                              .skipValidation(RequestField.ofNullable(Boolean.TRUE))
                                              .build();

    SettingAttribute setting =
        SettingAttribute.Builder.aSettingAttribute().withValue(PcfConfig.builder().build()).build();
    helper.updateSettingAttribute(setting, input, ACCOUNT_ID);

    assertThat(setting).isNotNull();
    assertThat(setting.getName()).isEqualTo(NAME);
    assertThat(setting.getValue()).isInstanceOf(PcfConfig.class);
    PcfConfig config = (PcfConfig) setting.getValue();
    assertThat(config.getEndpointUrl()).isEqualTo(URL);
    assertThat(config.getUsername()).isEqualTo(USERNAME.toCharArray());
    assertThat(config.getEncryptedPassword()).isEqualTo(PASSWORD);
    assertThat(config.isSkipValidation()).isEqualTo(Boolean.TRUE);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void toSettingAttributeUsernameAndUsernameSecretId() {
    assertThatThrownBy(() -> toSettingAttributeUsernameAndUsernameSecretId(USERNAME, USERNAME))
        .hasMessageContaining("Cannot set both value and secret reference for username field");
    assertThatThrownBy(() -> toSettingAttributeUsernameAndUsernameSecretId(null, null))
        .hasMessageContaining("One of fields 'userName' or 'userNameSecretId' is required");

    SettingAttribute settingAttribute = toSettingAttributeUsernameAndUsernameSecretId(USERNAME, null);
    assertSettingAttributeUsername(settingAttribute, USERNAME.toCharArray(), null);
    settingAttribute = toSettingAttributeUsernameAndUsernameSecretId(null, USERNAME);
    assertSettingAttributeUsername(settingAttribute, null, USERNAME);
  }

  private SettingAttribute toSettingAttributeUsernameAndUsernameSecretId(String userName, String userNameSecretId) {
    QLPcfCloudProviderInput input = QLPcfCloudProviderInput.builder()
                                        .name(RequestField.ofNullable(NAME))
                                        .endpointUrl(RequestField.ofNullable(URL))
                                        .userName(RequestField.ofNullable(userName))
                                        .userNameSecretId(RequestField.ofNullable(userNameSecretId))
                                        .passwordSecretId(RequestField.ofNullable(PASSWORD))
                                        .skipValidation(RequestField.ofNull())
                                        .build();

    return helper.toSettingAttribute(input, ACCOUNT_ID);
  }

  @Test
  @Owner(developers = IGOR)
  @Category(UnitTests.class)
  public void updateSettingAttributeWithEmptyInput() {
    QLUpdatePcfCloudProviderInput input = QLUpdatePcfCloudProviderInput.builder()
                                              .name(RequestField.ofNull())
                                              .endpointUrl(RequestField.ofNull())
                                              .userName(RequestField.ofNull())
                                              .userNameSecretId(RequestField.ofNull())
                                              .passwordSecretId(RequestField.ofNull())
                                              .skipValidation(RequestField.ofNull())
                                              .build();

    SettingAttribute setting =
        SettingAttribute.Builder.aSettingAttribute().withValue(PcfConfig.builder().build()).build();
    helper.updateSettingAttribute(setting, input, ACCOUNT_ID);

    assertThat(setting).isNotNull();
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void updateSettingAttributeUsernameAndUsernameSecretId() {
    assertThatThrownBy(() -> updateSettingAttributeUsernameAndUsernameSecretId(USERNAME, USERNAME))
        .hasMessageContaining("Cannot set both value and secret reference for username field");

    SettingAttribute attribute = updateSettingAttributeUsernameAndUsernameSecretId(USERNAME, null);
    assertSettingAttributeUsername(attribute, USERNAME.toCharArray(), null);

    attribute = updateSettingAttributeUsernameAndUsernameSecretId(null, USERNAME);
    assertSettingAttributeUsername(attribute, null, USERNAME);

    // KEEP existing values
    attribute = updateSettingAttributeUsernameAndUsernameSecretId(null, null);
    assertSettingAttributeUsername(attribute, USERNAME.toCharArray(), USERNAME);
  }

  private SettingAttribute updateSettingAttributeUsernameAndUsernameSecretId(String userName, String userNameSecretId) {
    QLUpdatePcfCloudProviderInput input = QLUpdatePcfCloudProviderInput.builder()
                                              .name(RequestField.ofNullable(NAME))
                                              .endpointUrl(RequestField.ofNullable(URL))
                                              .userName(RequestField.ofNullable(userName))
                                              .userNameSecretId(RequestField.ofNullable(userNameSecretId))
                                              .passwordSecretId(RequestField.ofNullable(PASSWORD))
                                              .skipValidation(RequestField.ofNull())
                                              .build();

    SettingAttribute setting = SettingAttribute.Builder.aSettingAttribute()
                                   .withValue(PcfConfig.builder()
                                                  .username(USERNAME.toCharArray())
                                                  .encryptedUsername(USERNAME)
                                                  .useEncryptedUsername(true)
                                                  .encryptedPassword(PASSWORD)
                                                  .build())
                                   .build();
    helper.updateSettingAttribute(setting, input, ACCOUNT_ID);

    return setting;
  }

  private void assertSettingAttributeUsername(SettingAttribute attribute, char[] username, String encryptedUsername) {
    PcfConfig config = (PcfConfig) attribute.getValue();
    assertThat(config.getUsername()).isEqualTo(username);
    assertThat(config.getEncryptedUsername()).isEqualTo(encryptedUsername);
    assertThat(config.isUseEncryptedUsername()).isEqualTo(encryptedUsername != null);
  }
}
