package software.wings.graphql.datafetcher.cloudProvider;

import static io.harness.rule.OwnerRule.IGOR;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.utils.RequestField;

import software.wings.WingsBaseTest;
import software.wings.beans.GcpConfig;
import software.wings.beans.SettingAttribute;
import software.wings.graphql.datafetcher.secrets.UsageScopeController;
import software.wings.graphql.schema.mutation.cloudProvider.QLGcpCloudProviderInput;
import software.wings.graphql.schema.mutation.cloudProvider.QLUpdateGcpCloudProviderInput;

import java.sql.SQLException;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class GcpDataFetcherHelperTest extends WingsBaseTest {
  private static final String NAME = "NAME";
  private static final String KEY = "KEY";
  private static final String ACCOUNT_ID = "777";
  private static final String DELEGATE_SELECTOR = "primary";

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
                                        .skipValidation(RequestField.ofNullable(false))
                                        .delegateSelector(RequestField.ofNullable(DELEGATE_SELECTOR))
                                        .useDelegate(RequestField.ofNull())
                                        .build();

    SettingAttribute setting = helper.toSettingAttribute(input, ACCOUNT_ID);

    assertThat(setting).isNotNull();
    assertThat(setting.getName()).isEqualTo(NAME);
    assertThat(setting.getValue()).isInstanceOf(GcpConfig.class);
    GcpConfig config = (GcpConfig) setting.getValue();
    assertThat(config.getEncryptedServiceAccountKeyFileContent()).isEqualTo(KEY);
    assertThat(config.getDelegateSelector()).isEqualTo(DELEGATE_SELECTOR);
    assertThat(config.isSkipValidation()).isFalse();
    assertThat(config.isUseDelegate()).isFalse();
  }

  @Test
  @Owner(developers = IGOR)
  @Category(UnitTests.class)
  public void toSettingAttributeWithEmptyInput() {
    QLGcpCloudProviderInput input = QLGcpCloudProviderInput.builder()
                                        .name(RequestField.ofNull())
                                        .serviceAccountKeySecretId(RequestField.ofNull())
                                        .skipValidation(RequestField.ofNull())
                                        .delegateSelector(RequestField.ofNull())
                                        .useDelegate(RequestField.ofNull())
                                        .build();

    SettingAttribute setting = helper.toSettingAttribute(input, ACCOUNT_ID);

    assertThat(setting).isNotNull();
  }

  @Test
  @Owner(developers = IGOR)
  @Category(UnitTests.class)
  public void updateSettingAttributePerformance() {
    QLUpdateGcpCloudProviderInput input = QLUpdateGcpCloudProviderInput.builder()
                                              .name(RequestField.ofNullable(NAME))
                                              .serviceAccountKeySecretId(RequestField.ofNullable(KEY))
                                              .skipValidation(RequestField.ofNullable(true))
                                              .delegateSelector(RequestField.ofNullable(DELEGATE_SELECTOR))
                                              .useDelegate(RequestField.ofNullable(true))
                                              .build();

    SettingAttribute setting =
        SettingAttribute.Builder.aSettingAttribute().withValue(GcpConfig.builder().build()).build();
    helper.updateSettingAttribute(setting, input, ACCOUNT_ID);

    assertThat(setting).isNotNull();
    assertThat(setting.getName()).isEqualTo(NAME);
    assertThat(setting.getValue()).isInstanceOf(GcpConfig.class);
    GcpConfig config = (GcpConfig) setting.getValue();
    assertThat(config.getEncryptedServiceAccountKeyFileContent()).isEqualTo(KEY);
    assertThat(config.getDelegateSelector()).isEqualTo(DELEGATE_SELECTOR);
    assertThat(config.isUseDelegate()).isTrue();
    assertThat(config.isSkipValidation()).isTrue();
  }

  @Test
  @Owner(developers = IGOR)
  @Category(UnitTests.class)
  public void updateSettingAttributeWithEmptyInput() {
    QLUpdateGcpCloudProviderInput input = QLUpdateGcpCloudProviderInput.builder()
                                              .name(RequestField.ofNull())
                                              .serviceAccountKeySecretId(RequestField.ofNull())
                                              .skipValidation(RequestField.ofNull())
                                              .delegateSelector(RequestField.ofNull())
                                              .useDelegate(RequestField.ofNull())
                                              .build();

    SettingAttribute setting =
        SettingAttribute.Builder.aSettingAttribute().withValue(GcpConfig.builder().build()).build();
    helper.updateSettingAttribute(setting, input, ACCOUNT_ID);

    assertThat(setting).isNotNull();
  }
}
