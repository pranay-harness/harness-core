/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.cloudProvider;

import static io.harness.rule.OwnerRule.IGOR;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;
import io.harness.utils.RequestField;

import software.wings.WingsBaseTest;
import software.wings.beans.AzureConfig;
import software.wings.beans.SettingAttribute;
import software.wings.graphql.datafetcher.secrets.UsageScopeController;
import software.wings.graphql.schema.mutation.cloudProvider.QLAzureCloudProviderInput;
import software.wings.graphql.schema.mutation.cloudProvider.QLUpdateAzureCloudProviderInput;

import java.sql.SQLException;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class AzureDataFetcherHelperTest extends WingsBaseTest {
  private static final String NAME = "NAME";
  private static final String CLIENT_ID = "URL";
  private static final String TENANT_ID = "username";
  private static final String KEY = "password";
  private static final String ACCOUNT_ID = "777";

  @Mock private UsageScopeController usageScopeController;

  @InjectMocks private AzureDataFetcherHelper helper = new AzureDataFetcherHelper();

  @Before
  public void setup() throws SQLException {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = IGOR)
  @Category(UnitTests.class)
  public void toSettingAttributeReturnValue() {
    QLAzureCloudProviderInput input = QLAzureCloudProviderInput.builder()
                                          .name(RequestField.ofNullable(NAME))
                                          .clientId(RequestField.ofNullable(CLIENT_ID))
                                          .tenantId(RequestField.ofNullable(TENANT_ID))
                                          .keySecretId(RequestField.ofNullable(KEY))
                                          .build();

    SettingAttribute setting = helper.toSettingAttribute(input, ACCOUNT_ID);

    assertThat(setting).isNotNull();
    assertThat(setting.getName()).isEqualTo(NAME);
    assertThat(setting.getValue()).isInstanceOf(AzureConfig.class);
    AzureConfig config = (AzureConfig) setting.getValue();
    assertThat(config.getClientId()).isEqualTo(CLIENT_ID);
    assertThat(config.getTenantId()).isEqualTo(TENANT_ID);
    assertThat(config.getEncryptedKey()).isEqualTo(KEY);
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = IGOR)
  @Category(UnitTests.class)
  public void toSettingAttributeWithEmptyClientIdInput() {
    QLAzureCloudProviderInput input = QLAzureCloudProviderInput.builder()
                                          .name(RequestField.ofNull())
                                          .clientId(RequestField.ofNull())
                                          .tenantId(RequestField.ofNull())
                                          .keySecretId(RequestField.ofNull())
                                          .build();

    SettingAttribute setting = helper.toSettingAttribute(input, ACCOUNT_ID);

    assertThat(setting).isNotNull();
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = IGOR)
  @Category(UnitTests.class)
  public void toSettingAttributeWithEmptyTenantIdInput() {
    QLAzureCloudProviderInput input = QLAzureCloudProviderInput.builder()
                                          .name(RequestField.ofNull())
                                          .clientId(RequestField.ofNull())
                                          .tenantId(RequestField.ofNull())
                                          .keySecretId(RequestField.ofNull())
                                          .build();

    SettingAttribute setting = helper.toSettingAttribute(input, ACCOUNT_ID);

    assertThat(setting).isNotNull();
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = IGOR)
  @Category(UnitTests.class)
  public void toSettingAttributeWithEmptyKeySecretIdInput() {
    QLAzureCloudProviderInput input = QLAzureCloudProviderInput.builder()
                                          .name(RequestField.ofNull())
                                          .clientId(RequestField.ofNull())
                                          .tenantId(RequestField.ofNull())
                                          .keySecretId(RequestField.ofNull())
                                          .build();

    SettingAttribute setting = helper.toSettingAttribute(input, ACCOUNT_ID);

    assertThat(setting).isNotNull();
  }

  @Test
  @Owner(developers = IGOR)
  @Category(UnitTests.class)
  public void updateSettingAttributePerformance() {
    QLUpdateAzureCloudProviderInput input = QLUpdateAzureCloudProviderInput.builder()
                                                .name(RequestField.ofNullable(NAME))
                                                .clientId(RequestField.ofNullable(CLIENT_ID))
                                                .tenantId(RequestField.ofNullable(TENANT_ID))
                                                .keySecretId(RequestField.ofNullable(KEY))
                                                .build();

    SettingAttribute setting =
        SettingAttribute.Builder.aSettingAttribute().withValue(AzureConfig.builder().build()).build();
    helper.updateSettingAttribute(setting, input, ACCOUNT_ID);

    assertThat(setting).isNotNull();
    assertThat(setting.getName()).isEqualTo(NAME);
    assertThat(setting.getValue()).isInstanceOf(AzureConfig.class);
    AzureConfig config = (AzureConfig) setting.getValue();
    assertThat(config.getClientId()).isEqualTo(CLIENT_ID);
    assertThat(config.getTenantId()).isEqualTo(TENANT_ID);
    assertThat(config.getEncryptedKey()).isEqualTo(KEY);
  }

  @Test
  @Owner(developers = IGOR)
  @Category(UnitTests.class)
  public void updateSettingAttributeWithEmptyInput() {
    QLUpdateAzureCloudProviderInput input = QLUpdateAzureCloudProviderInput.builder()
                                                .name(RequestField.ofNull())
                                                .clientId(RequestField.ofNull())
                                                .tenantId(RequestField.ofNull())
                                                .keySecretId(RequestField.ofNull())
                                                .build();

    SettingAttribute setting =
        SettingAttribute.Builder.aSettingAttribute().withValue(AzureConfig.builder().build()).build();
    helper.updateSettingAttribute(setting, input, ACCOUNT_ID);

    assertThat(setting).isNotNull();
  }
}
