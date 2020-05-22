package software.wings.graphql.datafetcher.cloudProvider;

import static io.harness.rule.OwnerRule.IGOR;
import static software.wings.graphql.datafetcher.cloudProvider.CreateCloudProviderDataFetcherTest.usageScope;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.utils.RequestField;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.wings.WingsBaseTest;
import software.wings.beans.PhysicalDataCenterConfig;
import software.wings.beans.SettingAttribute;
import software.wings.graphql.datafetcher.secrets.UsageScopeController;
import software.wings.graphql.schema.mutation.cloudProvider.QLPhysicalDataCenterCloudProviderInput;
import software.wings.settings.SettingValue;

import java.sql.SQLException;

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

    Assertions.assertThat(setting).isNotNull();
    Assertions.assertThat(setting.getName()).isEqualTo(NAME);
    Assertions.assertThat(setting.getValue()).isInstanceOf(PhysicalDataCenterConfig.class);
    PhysicalDataCenterConfig config = (PhysicalDataCenterConfig) setting.getValue();
    Assertions.assertThat(config.getSettingType()).isEqualTo(SettingValue.SettingVariableTypes.PHYSICAL_DATA_CENTER);
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

    Assertions.assertThat(setting).isNotNull();
  }

  @Test
  @Owner(developers = IGOR)
  @Category(UnitTests.class)
  public void updateSettingAttributePerformance() {
    QLPhysicalDataCenterCloudProviderInput input = QLPhysicalDataCenterCloudProviderInput.builder()
                                                       .name(RequestField.ofNullable(NAME))
                                                       .usageScope(RequestField.ofNullable(usageScope()))
                                                       .build();

    SettingAttribute setting =
        SettingAttribute.Builder.aSettingAttribute()
            .withValue(PhysicalDataCenterConfig.Builder.aPhysicalDataCenterConfig()
                           .withType(SettingValue.SettingVariableTypes.PHYSICAL_DATA_CENTER.name())
                           .build())
            .build();
    helper.updateSettingAttribute(setting, input, ACCOUNT_ID);

    Assertions.assertThat(setting).isNotNull();
    Assertions.assertThat(setting.getName()).isEqualTo(NAME);
    Assertions.assertThat(setting.getValue()).isInstanceOf(PhysicalDataCenterConfig.class);
    PhysicalDataCenterConfig config = (PhysicalDataCenterConfig) setting.getValue();
    Assertions.assertThat(config.getSettingType()).isEqualTo(SettingValue.SettingVariableTypes.PHYSICAL_DATA_CENTER);
  }

  @Test
  @Owner(developers = IGOR)
  @Category(UnitTests.class)
  public void updateSettingAttributeWithEmptyInput() {
    QLPhysicalDataCenterCloudProviderInput input = QLPhysicalDataCenterCloudProviderInput.builder()
                                                       .name(RequestField.ofNull())
                                                       .usageScope(RequestField.ofNull())
                                                       .build();

    SettingAttribute setting =
        SettingAttribute.Builder.aSettingAttribute()
            .withValue(PhysicalDataCenterConfig.Builder.aPhysicalDataCenterConfig()
                           .withType(SettingValue.SettingVariableTypes.PHYSICAL_DATA_CENTER.name())
                           .build())
            .build();
    helper.updateSettingAttribute(setting, input, ACCOUNT_ID);

    Assertions.assertThat(setting).isNotNull();
  }
}