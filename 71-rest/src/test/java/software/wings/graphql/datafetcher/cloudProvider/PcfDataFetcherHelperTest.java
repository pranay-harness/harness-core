package software.wings.graphql.datafetcher.cloudProvider;

import static io.harness.rule.OwnerRule.IGOR;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.utils.RequestField;
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
import software.wings.graphql.schema.mutation.cloudProvider.QLUpdatePcfCloudProviderInput;

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
                                        .endpointUrl(RequestField.ofNullable(URL))
                                        .userName(RequestField.ofNullable(USERNAME))
                                        .passwordSecretId(RequestField.ofNullable(PASSWORD))
                                        .skipValidation(RequestField.ofNullable(Boolean.TRUE))
                                        .build();

    SettingAttribute setting = helper.toSettingAttribute(input, ACCOUNT_ID);

    assertThat(setting).isNotNull();
    assertThat(setting.getName()).isEqualTo(NAME);
    assertThat(setting.getValue()).isInstanceOf(PcfConfig.class);
    PcfConfig config = (PcfConfig) setting.getValue();
    assertThat(config.getEndpointUrl()).isEqualTo(URL);
    assertThat(config.getUsername()).isEqualTo(USERNAME);
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
    assertThat(config.getUsername()).isEqualTo(USERNAME);
    assertThat(config.getEncryptedPassword()).isEqualTo(PASSWORD);
    assertThat(config.isSkipValidation()).isEqualTo(Boolean.TRUE);
  }

  @Test
  @Owner(developers = IGOR)
  @Category(UnitTests.class)
  public void updateSettingAttributeWithEmptyInput() {
    QLUpdatePcfCloudProviderInput input = QLUpdatePcfCloudProviderInput.builder()
                                              .name(RequestField.ofNull())
                                              .endpointUrl(RequestField.ofNull())
                                              .userName(RequestField.ofNull())
                                              .passwordSecretId(RequestField.ofNull())
                                              .skipValidation(RequestField.ofNull())
                                              .build();

    SettingAttribute setting =
        SettingAttribute.Builder.aSettingAttribute().withValue(PcfConfig.builder().build()).build();
    helper.updateSettingAttribute(setting, input, ACCOUNT_ID);

    assertThat(setting).isNotNull();
  }
}