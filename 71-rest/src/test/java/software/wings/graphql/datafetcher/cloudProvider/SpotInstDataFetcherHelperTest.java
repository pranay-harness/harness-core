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
import software.wings.beans.SettingAttribute;
import software.wings.beans.SpotInstConfig;
import software.wings.graphql.datafetcher.secrets.UsageScopeController;
import software.wings.graphql.schema.mutation.cloudProvider.QLSpotInstCloudProviderInput;

import java.sql.SQLException;

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
                                                   .usageScope(RequestField.ofNullable(usageScope()))
                                                   .build();

    SettingAttribute setting = helper.toSettingAttribute(input, ACCOUNT_ID);

    Assertions.assertThat(setting).isNotNull();
    Assertions.assertThat(setting.getName()).isEqualTo(NAME);
    Assertions.assertThat(setting.getValue()).isInstanceOf(SpotInstConfig.class);
    SpotInstConfig config = (SpotInstConfig) setting.getValue();
    Assertions.assertThat(config.getSpotInstAccountId()).isEqualTo(SPOT_INST_ACCOUNT_ID);
    Assertions.assertThat(config.getEncryptedSpotInstToken()).isEqualTo(SPOT_INST_TOKEN);
  }

  @Test
  @Owner(developers = IGOR)
  @Category(UnitTests.class)
  public void toSettingAttributeWithEmptyInput() {
    QLSpotInstCloudProviderInput input = QLSpotInstCloudProviderInput.builder()
                                             .name(RequestField.ofNull())
                                             .accountId(RequestField.ofNull())
                                             .tokenSecretId(RequestField.ofNull())
                                             .usageScope(RequestField.ofNull())
                                             .build();

    SettingAttribute setting = helper.toSettingAttribute(input, ACCOUNT_ID);

    Assertions.assertThat(setting).isNotNull();
  }

  @Test
  @Owner(developers = IGOR)
  @Category(UnitTests.class)
  public void updateSettingAttributePerformance() {
    final QLSpotInstCloudProviderInput input = QLSpotInstCloudProviderInput.builder()
                                                   .name(RequestField.ofNullable(NAME))
                                                   .accountId(RequestField.ofNullable(SPOT_INST_ACCOUNT_ID))
                                                   .tokenSecretId(RequestField.ofNullable(SPOT_INST_TOKEN))
                                                   .usageScope(RequestField.ofNullable(usageScope()))
                                                   .build();

    SettingAttribute setting =
        SettingAttribute.Builder.aSettingAttribute().withValue(SpotInstConfig.builder().build()).build();
    helper.updateSettingAttribute(setting, input, ACCOUNT_ID);

    Assertions.assertThat(setting).isNotNull();
    Assertions.assertThat(setting.getName()).isEqualTo(NAME);
    Assertions.assertThat(setting.getValue()).isInstanceOf(SpotInstConfig.class);
    SpotInstConfig config = (SpotInstConfig) setting.getValue();
    Assertions.assertThat(config.getSpotInstAccountId()).isEqualTo(SPOT_INST_ACCOUNT_ID);
    Assertions.assertThat(config.getEncryptedSpotInstToken()).isEqualTo(SPOT_INST_TOKEN);
  }

  @Test
  @Owner(developers = IGOR)
  @Category(UnitTests.class)
  public void updateSettingAttributeWithEmptyInput() {
    QLSpotInstCloudProviderInput input = QLSpotInstCloudProviderInput.builder()
                                             .name(RequestField.ofNull())
                                             .accountId(RequestField.ofNull())
                                             .tokenSecretId(RequestField.ofNull())
                                             .usageScope(RequestField.ofNull())
                                             .build();

    SettingAttribute setting =
        SettingAttribute.Builder.aSettingAttribute().withValue(SpotInstConfig.builder().build()).build();
    helper.updateSettingAttribute(setting, input, ACCOUNT_ID);

    Assertions.assertThat(setting).isNotNull();
  }
}