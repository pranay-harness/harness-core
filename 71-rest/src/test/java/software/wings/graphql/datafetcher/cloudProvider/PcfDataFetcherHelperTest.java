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
import software.wings.beans.PcfConfig;
import software.wings.beans.SettingAttribute;
import software.wings.graphql.datafetcher.secrets.UsageScopeController;
import software.wings.graphql.schema.mutation.cloudProvider.QLPcfCloudProviderInput;

import java.sql.SQLException;

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
                                        .usageScope(RequestField.ofNullable(usageScope()))
                                        .endpointUrl(RequestField.ofNullable(URL))
                                        .userName(RequestField.ofNullable(USERNAME))
                                        .passwordSecretId(RequestField.ofNullable(PASSWORD))
                                        .build();

    SettingAttribute setting = helper.toSettingAttribute(input, ACCOUNT_ID);

    Assertions.assertThat(setting).isNotNull();
    Assertions.assertThat(setting.getName()).isEqualTo(NAME);
    Assertions.assertThat(setting.getValue()).isInstanceOf(PcfConfig.class);
    PcfConfig config = (PcfConfig) setting.getValue();
    Assertions.assertThat(config.getEndpointUrl()).isEqualTo(URL);
    Assertions.assertThat(config.getUsername()).isEqualTo(USERNAME);
    Assertions.assertThat(config.getEncryptedPassword()).isEqualTo(PASSWORD);
  }

  @Test
  @Owner(developers = IGOR)
  @Category(UnitTests.class)
  public void toSettingAttributeWithEmptyInput() {
    QLPcfCloudProviderInput input = QLPcfCloudProviderInput.builder()
                                        .name(RequestField.ofNull())
                                        .usageScope(RequestField.ofNull())
                                        .endpointUrl(RequestField.ofNull())
                                        .userName(RequestField.ofNull())
                                        .passwordSecretId(RequestField.ofNull())
                                        .build();

    SettingAttribute setting = helper.toSettingAttribute(input, ACCOUNT_ID);

    Assertions.assertThat(setting).isNotNull();
  }

  @Test
  @Owner(developers = IGOR)
  @Category(UnitTests.class)
  public void updateSettingAttributePerformance() {
    QLPcfCloudProviderInput input = QLPcfCloudProviderInput.builder()
                                        .name(RequestField.ofNullable(NAME))
                                        .usageScope(RequestField.ofNullable(usageScope()))
                                        .endpointUrl(RequestField.ofNullable(URL))
                                        .userName(RequestField.ofNullable(USERNAME))
                                        .passwordSecretId(RequestField.ofNullable(PASSWORD))
                                        .build();

    SettingAttribute setting =
        SettingAttribute.Builder.aSettingAttribute().withValue(PcfConfig.builder().build()).build();
    helper.updateSettingAttribute(setting, input, ACCOUNT_ID);

    Assertions.assertThat(setting).isNotNull();
    Assertions.assertThat(setting.getName()).isEqualTo(NAME);
    Assertions.assertThat(setting.getValue()).isInstanceOf(PcfConfig.class);
    PcfConfig config = (PcfConfig) setting.getValue();
    Assertions.assertThat(config.getEndpointUrl()).isEqualTo(URL);
    Assertions.assertThat(config.getUsername()).isEqualTo(USERNAME);
    Assertions.assertThat(config.getEncryptedPassword()).isEqualTo(PASSWORD);
  }

  @Test
  @Owner(developers = IGOR)
  @Category(UnitTests.class)
  public void updateSettingAttributeWithEmptyInput() {
    QLPcfCloudProviderInput input = QLPcfCloudProviderInput.builder()
                                        .name(RequestField.ofNull())
                                        .usageScope(RequestField.ofNull())
                                        .endpointUrl(RequestField.ofNull())
                                        .userName(RequestField.ofNull())
                                        .passwordSecretId(RequestField.ofNull())
                                        .build();

    SettingAttribute setting =
        SettingAttribute.Builder.aSettingAttribute().withValue(PcfConfig.builder().build()).build();
    helper.updateSettingAttribute(setting, input, ACCOUNT_ID);

    Assertions.assertThat(setting).isNotNull();
  }
}