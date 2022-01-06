/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.cloudProvider;

import static io.harness.rule.OwnerRule.IGOR;

import static software.wings.graphql.datafetcher.cloudProvider.CreateCloudProviderDataFetcherTest.usageScope;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.utils.RequestField;

import software.wings.WingsBaseTest;
import software.wings.beans.PhysicalDataCenterConfig;
import software.wings.beans.SettingAttribute;
import software.wings.graphql.datafetcher.secrets.UsageScopeController;
import software.wings.graphql.schema.mutation.cloudProvider.QLPhysicalDataCenterCloudProviderInput;
import software.wings.graphql.schema.mutation.cloudProvider.QLUpdatePhysicalDataCenterCloudProviderInput;
import software.wings.settings.SettingVariableTypes;

import java.sql.SQLException;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class PhysicalDataCenterDataFetcherHelperTest extends WingsBaseTest {
  private static final String NAME = "NAME";
  private static final String ACCOUNT_ID = "777";

  @Mock private UsageScopeController usageScopeController;

  @InjectMocks private PhysicalDataCenterDataFetcherHelper helper = new PhysicalDataCenterDataFetcherHelper();

  @Before
  public void setup() throws SQLException {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = IGOR)
  @Category(UnitTests.class)
  public void toSettingAttributeReturnValue() {
    QLPhysicalDataCenterCloudProviderInput input = QLPhysicalDataCenterCloudProviderInput.builder()
                                                       .name(RequestField.ofNullable(NAME))
                                                       .usageScope(RequestField.ofNullable(usageScope()))
                                                       .build();

    SettingAttribute setting = helper.toSettingAttribute(input, ACCOUNT_ID);

    assertThat(setting).isNotNull();
    assertThat(setting.getName()).isEqualTo(NAME);
    assertThat(setting.getValue()).isInstanceOf(PhysicalDataCenterConfig.class);
    PhysicalDataCenterConfig config = (PhysicalDataCenterConfig) setting.getValue();
    assertThat(config.getSettingType()).isEqualTo(SettingVariableTypes.PHYSICAL_DATA_CENTER);
  }

  @Test
  @Owner(developers = IGOR)
  @Category(UnitTests.class)
  public void toSettingAttributeWithEmptyInput() {
    QLPhysicalDataCenterCloudProviderInput input = QLPhysicalDataCenterCloudProviderInput.builder()
                                                       .name(RequestField.ofNull())
                                                       .usageScope(RequestField.ofNull())
                                                       .build();

    SettingAttribute setting = helper.toSettingAttribute(input, ACCOUNT_ID);

    assertThat(setting).isNotNull();
  }

  @Test
  @Owner(developers = IGOR)
  @Category(UnitTests.class)
  public void updateSettingAttributePerformance() {
    QLUpdatePhysicalDataCenterCloudProviderInput input = QLUpdatePhysicalDataCenterCloudProviderInput.builder()
                                                             .name(RequestField.ofNullable(NAME))
                                                             .usageScope(RequestField.ofNullable(usageScope()))
                                                             .build();

    SettingAttribute setting = SettingAttribute.Builder.aSettingAttribute()
                                   .withValue(PhysicalDataCenterConfig.Builder.aPhysicalDataCenterConfig()
                                                  .withType(SettingVariableTypes.PHYSICAL_DATA_CENTER.name())
                                                  .build())
                                   .build();
    helper.updateSettingAttribute(setting, input, ACCOUNT_ID);

    assertThat(setting).isNotNull();
    assertThat(setting.getName()).isEqualTo(NAME);
    assertThat(setting.getValue()).isInstanceOf(PhysicalDataCenterConfig.class);
    PhysicalDataCenterConfig config = (PhysicalDataCenterConfig) setting.getValue();
    assertThat(config.getSettingType()).isEqualTo(SettingVariableTypes.PHYSICAL_DATA_CENTER);
  }

  @Test
  @Owner(developers = IGOR)
  @Category(UnitTests.class)
  public void updateSettingAttributeWithEmptyInput() {
    QLUpdatePhysicalDataCenterCloudProviderInput input = QLUpdatePhysicalDataCenterCloudProviderInput.builder()
                                                             .name(RequestField.ofNull())
                                                             .usageScope(RequestField.ofNull())
                                                             .build();

    SettingAttribute setting = SettingAttribute.Builder.aSettingAttribute()
                                   .withValue(PhysicalDataCenterConfig.Builder.aPhysicalDataCenterConfig()
                                                  .withType(SettingVariableTypes.PHYSICAL_DATA_CENTER.name())
                                                  .build())
                                   .build();
    helper.updateSettingAttribute(setting, input, ACCOUNT_ID);

    assertThat(setting).isNotNull();
  }
}
