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
import io.harness.rule.Owner;
import io.harness.utils.RequestField;

import software.wings.WingsBaseTest;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SpotInstConfig;
import software.wings.graphql.datafetcher.secrets.UsageScopeController;
import software.wings.graphql.schema.mutation.cloudProvider.QLSpotInstCloudProviderInput;
import software.wings.graphql.schema.mutation.cloudProvider.QLUpdateSpotInstCloudProviderInput;

import java.sql.SQLException;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class SpotInstDataFetcherHelperTest extends WingsBaseTest {
  private static final String NAME = "NAME";
  private static final String SPOT_INST_ACCOUNT_ID = "SpotInstAccountId";
  private static final String SPOT_INST_TOKEN = "SpotInstToken";
  private static final String ACCOUNT_ID = "777";

  @Mock private UsageScopeController usageScopeController;

  @InjectMocks private SpotInstDataFetcherHelper helper = new SpotInstDataFetcherHelper();

  @Before
  public void setup() throws SQLException {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = IGOR)
  @Category(UnitTests.class)
  public void toSettingAttributeReturnValue() {
    final QLSpotInstCloudProviderInput input = QLSpotInstCloudProviderInput.builder()
                                                   .name(RequestField.ofNullable(NAME))
                                                   .accountId(RequestField.ofNullable(SPOT_INST_ACCOUNT_ID))
                                                   .tokenSecretId(RequestField.ofNullable(SPOT_INST_TOKEN))
                                                   .build();

    SettingAttribute setting = helper.toSettingAttribute(input, ACCOUNT_ID);

    assertThat(setting).isNotNull();
    assertThat(setting.getName()).isEqualTo(NAME);
    assertThat(setting.getValue()).isInstanceOf(SpotInstConfig.class);
    SpotInstConfig config = (SpotInstConfig) setting.getValue();
    assertThat(config.getSpotInstAccountId()).isEqualTo(SPOT_INST_ACCOUNT_ID);
    assertThat(config.getEncryptedSpotInstToken()).isEqualTo(SPOT_INST_TOKEN);
  }

  @Test
  @Owner(developers = IGOR)
  @Category(UnitTests.class)
  public void toSettingAttributeWithEmptyInput() {
    QLSpotInstCloudProviderInput input = QLSpotInstCloudProviderInput.builder()
                                             .name(RequestField.ofNull())
                                             .accountId(RequestField.ofNull())
                                             .tokenSecretId(RequestField.ofNull())
                                             .build();

    SettingAttribute setting = helper.toSettingAttribute(input, ACCOUNT_ID);

    assertThat(setting).isNotNull();
  }

  @Test
  @Owner(developers = IGOR)
  @Category(UnitTests.class)
  public void updateSettingAttributePerformance() {
    final QLUpdateSpotInstCloudProviderInput input = QLUpdateSpotInstCloudProviderInput.builder()
                                                         .name(RequestField.ofNullable(NAME))
                                                         .accountId(RequestField.ofNullable(SPOT_INST_ACCOUNT_ID))
                                                         .tokenSecretId(RequestField.ofNullable(SPOT_INST_TOKEN))
                                                         .build();

    SettingAttribute setting =
        SettingAttribute.Builder.aSettingAttribute().withValue(SpotInstConfig.builder().build()).build();
    helper.updateSettingAttribute(setting, input, ACCOUNT_ID);

    assertThat(setting).isNotNull();
    assertThat(setting.getName()).isEqualTo(NAME);
    assertThat(setting.getValue()).isInstanceOf(SpotInstConfig.class);
    SpotInstConfig config = (SpotInstConfig) setting.getValue();
    assertThat(config.getSpotInstAccountId()).isEqualTo(SPOT_INST_ACCOUNT_ID);
    assertThat(config.getEncryptedSpotInstToken()).isEqualTo(SPOT_INST_TOKEN);
  }

  @Test
  @Owner(developers = IGOR)
  @Category(UnitTests.class)
  public void updateSettingAttributeWithEmptyInput() {
    QLUpdateSpotInstCloudProviderInput input = QLUpdateSpotInstCloudProviderInput.builder()
                                                   .name(RequestField.ofNull())
                                                   .accountId(RequestField.ofNull())
                                                   .tokenSecretId(RequestField.ofNull())
                                                   .build();

    SettingAttribute setting =
        SettingAttribute.Builder.aSettingAttribute().withValue(SpotInstConfig.builder().build()).build();
    helper.updateSettingAttribute(setting, input, ACCOUNT_ID);

    assertThat(setting).isNotNull();
  }
}
