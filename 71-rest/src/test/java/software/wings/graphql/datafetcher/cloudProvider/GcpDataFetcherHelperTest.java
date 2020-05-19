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
import software.wings.beans.GcpConfig;
import software.wings.beans.SettingAttribute;
import software.wings.graphql.datafetcher.secrets.UsageScopeController;
import software.wings.graphql.schema.mutation.cloudProvider.QLGcpCloudProviderInput;

import java.sql.SQLException;

public class GcpDataFetcherHelperTest extends WingsBaseTest {
  private static final String NAME = "NAME";
  private static final String KEY = "KEY";
  private static final String ACCOUNT_ID = "777";

  @Mock private UsageScopeController usageScopeController;

  @InjectMocks private GcpDataFetcherHelper helper = new GcpDataFetcherHelper();

  @Before
  public void setup() throws SQLException {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = IGOR)
  @Category(UnitTests.class)
  public void toSettingAttributeReturnValue() {
    QLGcpCloudProviderInput input = QLGcpCloudProviderInput.builder()
                                        .name(RequestField.ofNullable(NAME))
                                        .serviceAccountKeySecretId(RequestField.ofNullable(KEY))
                                        .usageScope(RequestField.ofNullable(usageScope()))
                                        .build();

    SettingAttribute setting = helper.toSettingAttribute(input, ACCOUNT_ID);

    Assertions.assertThat(setting).isNotNull();
    Assertions.assertThat(setting.getName()).isEqualTo(NAME);
    Assertions.assertThat(setting.getValue()).isInstanceOf(GcpConfig.class);
    GcpConfig config = (GcpConfig) setting.getValue();
    Assertions.assertThat(config.getEncryptedServiceAccountKeyFileContent()).isEqualTo(KEY);
  }

  @Test
  @Owner(developers = IGOR)
  @Category(UnitTests.class)
  public void toSettingAttributeWithEmptyInput() {
    QLGcpCloudProviderInput input = QLGcpCloudProviderInput.builder()
                                        .name(RequestField.ofNull())
                                        .usageScope(RequestField.ofNull())
                                        .serviceAccountKeySecretId(RequestField.ofNull())
                                        .build();

    SettingAttribute setting = helper.toSettingAttribute(input, ACCOUNT_ID);

    Assertions.assertThat(setting).isNotNull();
  }

  @Test
  @Owner(developers = IGOR)
  @Category(UnitTests.class)
  public void updateSettingAttributePerformance() {
    QLGcpCloudProviderInput input = QLGcpCloudProviderInput.builder()
                                        .name(RequestField.ofNullable(NAME))
                                        .serviceAccountKeySecretId(RequestField.ofNullable(KEY))
                                        .usageScope(RequestField.ofNullable(usageScope()))
                                        .build();

    SettingAttribute setting =
        SettingAttribute.Builder.aSettingAttribute().withValue(GcpConfig.builder().build()).build();
    helper.updateSettingAttribute(setting, input, ACCOUNT_ID);

    Assertions.assertThat(setting).isNotNull();
    Assertions.assertThat(setting.getName()).isEqualTo(NAME);
    Assertions.assertThat(setting.getValue()).isInstanceOf(GcpConfig.class);
    GcpConfig config = (GcpConfig) setting.getValue();
    Assertions.assertThat(config.getEncryptedServiceAccountKeyFileContent()).isEqualTo(KEY);
  }

  @Test
  @Owner(developers = IGOR)
  @Category(UnitTests.class)
  public void updateSettingAttributeWithEmptyInput() {
    QLGcpCloudProviderInput input = QLGcpCloudProviderInput.builder()
                                        .name(RequestField.ofNull())
                                        .usageScope(RequestField.ofNull())
                                        .serviceAccountKeySecretId(RequestField.ofNull())
                                        .build();

    SettingAttribute setting =
        SettingAttribute.Builder.aSettingAttribute().withValue(GcpConfig.builder().build()).build();
    helper.updateSettingAttribute(setting, input, ACCOUNT_ID);

    Assertions.assertThat(setting).isNotNull();
  }
}