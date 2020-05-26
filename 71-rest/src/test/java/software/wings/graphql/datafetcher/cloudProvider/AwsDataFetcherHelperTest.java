package software.wings.graphql.datafetcher.cloudProvider;

import static io.harness.rule.OwnerRule.IGOR;
import static software.wings.graphql.datafetcher.cloudProvider.CreateCloudProviderDataFetcherTest.usageScope;

import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
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
import software.wings.beans.AwsConfig;
import software.wings.beans.SettingAttribute;
import software.wings.graphql.datafetcher.secrets.UsageScopeController;
import software.wings.graphql.schema.mutation.cloudProvider.aws.QLAwsCloudProviderInput;
import software.wings.graphql.schema.mutation.cloudProvider.aws.QLAwsCrossAccountAttributes;
import software.wings.graphql.schema.mutation.cloudProvider.aws.QLAwsManualCredentials;

import java.sql.SQLException;

public class AwsDataFetcherHelperTest extends WingsBaseTest {
  private static final String NAME = "K8S";
  private static final String ACCOUNT_ID = "777";
  private static final String DELEGATE = "DELEGATE";
  public static final String EXTERN_ID = "EXTERN_ID";
  public static final String ARN = "ARN";
  public static final String SECRET_KEY = "SECRET_KEY";
  public static final String ACCESS_KEY = "ACCESS_KEY";

  @Mock private UsageScopeController usageScopeController;

  @InjectMocks private AwsDataFetcherHelper helper = new AwsDataFetcherHelper();

  @Before
  public void setup() throws SQLException {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = IGOR)
  @Category(UnitTests.class)
  public void toSettingAttributeReturnValue() {
    QLAwsCloudProviderInput input =
        QLAwsCloudProviderInput.builder()
            .name(RequestField.ofNullable(NAME))
            .usageScope(RequestField.ofNullable(usageScope()))
            .useEc2IamCredentials(RequestField.ofNullable(Boolean.FALSE))
            .manualCredentials(RequestField.ofNullable(QLAwsManualCredentials.builder()
                                                           .accessKey(RequestField.ofNullable(ACCESS_KEY))
                                                           .secretKeySecretId(RequestField.ofNullable(SECRET_KEY))
                                                           .build()))
            .assumeCrossAccountRole(RequestField.ofNullable(Boolean.TRUE))
            .crossAccountAttributes(RequestField.ofNullable(QLAwsCrossAccountAttributes.builder()
                                                                .crossAccountRoleArn(RequestField.ofNullable(ARN))
                                                                .externalId(RequestField.ofNullable(EXTERN_ID))
                                                                .build()))
            .build();

    SettingAttribute setting = helper.toSettingAttribute(input, ACCOUNT_ID);

    Assertions.assertThat(setting).isNotNull();
    Assertions.assertThat(setting.getName()).isEqualTo(NAME);
    Assertions.assertThat(setting.getValue()).isInstanceOf(AwsConfig.class);
    AwsConfig config = (AwsConfig) setting.getValue();
    Assertions.assertThat(config.getAccessKey()).isEqualTo(ACCESS_KEY);
    Assertions.assertThat(config.getEncryptedSecretKey()).isEqualTo(SECRET_KEY);
    Assertions.assertThat(config.getCrossAccountAttributes()).isNotNull();
    Assertions.assertThat(config.getCrossAccountAttributes().getCrossAccountRoleArn()).isEqualTo(ARN);
    Assertions.assertThat(config.getCrossAccountAttributes().getExternalId()).isEqualTo(EXTERN_ID);
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = IGOR)
  @Category(UnitTests.class)
  public void toSettingAttributeWithEmptyEc2Input() {
    QLAwsCloudProviderInput input = QLAwsCloudProviderInput.builder()
                                        .name(RequestField.absent())
                                        .usageScope(RequestField.absent())
                                        .useEc2IamCredentials(RequestField.ofNullable(Boolean.TRUE))
                                        .ec2IamCredentials(RequestField.ofNull())
                                        .assumeCrossAccountRole(RequestField.absent())
                                        .build();

    SettingAttribute setting = helper.toSettingAttribute(input, ACCOUNT_ID);

    Assertions.assertThat(setting).isNotNull();
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = IGOR)
  @Category(UnitTests.class)
  public void toSettingAttributeWithEmptyManualInput() {
    QLAwsCloudProviderInput input = QLAwsCloudProviderInput.builder()
                                        .name(RequestField.absent())
                                        .usageScope(RequestField.absent())
                                        .useEc2IamCredentials(RequestField.ofNullable(Boolean.FALSE))
                                        .manualCredentials(RequestField.ofNull())
                                        .assumeCrossAccountRole(RequestField.absent())
                                        .build();

    SettingAttribute setting = helper.toSettingAttribute(input, ACCOUNT_ID);

    Assertions.assertThat(setting).isNotNull();
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = IGOR)
  @Category(UnitTests.class)
  public void toSettingAttributeWithEmptyXAccountRoleInput() {
    QLAwsCloudProviderInput input = QLAwsCloudProviderInput.builder()
                                        .name(RequestField.absent())
                                        .usageScope(RequestField.absent())
                                        .useEc2IamCredentials(RequestField.absent())
                                        .assumeCrossAccountRole(RequestField.ofNullable(Boolean.TRUE))
                                        .build();

    SettingAttribute setting = helper.toSettingAttribute(input, ACCOUNT_ID);

    Assertions.assertThat(setting).isNotNull();
  }

  @Test
  @Owner(developers = IGOR)
  @Category(UnitTests.class)
  public void updateSettingAttributePerformance() {
    QLAwsCloudProviderInput input =
        QLAwsCloudProviderInput.builder()
            .name(RequestField.ofNullable(NAME))
            .usageScope(RequestField.ofNullable(usageScope()))
            .useEc2IamCredentials(RequestField.ofNullable(Boolean.FALSE))
            .manualCredentials(RequestField.ofNullable(QLAwsManualCredentials.builder()
                                                           .accessKey(RequestField.ofNullable(ACCESS_KEY))
                                                           .secretKeySecretId(RequestField.ofNullable(SECRET_KEY))
                                                           .build()))
            .assumeCrossAccountRole(RequestField.ofNullable(Boolean.TRUE))
            .crossAccountAttributes(RequestField.ofNullable(QLAwsCrossAccountAttributes.builder()
                                                                .crossAccountRoleArn(RequestField.ofNullable(ARN))
                                                                .externalId(RequestField.ofNullable(EXTERN_ID))
                                                                .build()))
            .build();

    SettingAttribute setting =
        SettingAttribute.Builder.aSettingAttribute().withValue(AwsConfig.builder().build()).build();
    helper.updateSettingAttribute(setting, input, ACCOUNT_ID);

    Assertions.assertThat(setting).isNotNull();
    Assertions.assertThat(setting.getName()).isEqualTo(NAME);
    Assertions.assertThat(setting.getValue()).isInstanceOf(AwsConfig.class);
    AwsConfig config = (AwsConfig) setting.getValue();
    Assertions.assertThat(config.getAccessKey()).isEqualTo(ACCESS_KEY);
    Assertions.assertThat(config.getEncryptedSecretKey()).isEqualTo(SECRET_KEY);
    Assertions.assertThat(config.getCrossAccountAttributes()).isNotNull();
    Assertions.assertThat(config.getCrossAccountAttributes().getCrossAccountRoleArn()).isEqualTo(ARN);
    Assertions.assertThat(config.getCrossAccountAttributes().getExternalId()).isEqualTo(EXTERN_ID);
  }

  @Test
  @Owner(developers = IGOR)
  @Category(UnitTests.class)
  public void updateSettingAttributeWithEmptyInput() {
    QLAwsCloudProviderInput input = QLAwsCloudProviderInput.builder()
                                        .name(RequestField.absent())
                                        .usageScope(RequestField.absent())
                                        .assumeCrossAccountRole(RequestField.absent())
                                        .crossAccountAttributes(RequestField.absent())
                                        .ec2IamCredentials(RequestField.absent())
                                        .manualCredentials(RequestField.absent())
                                        .useEc2IamCredentials(RequestField.absent())
                                        .build();

    SettingAttribute setting =
        SettingAttribute.Builder.aSettingAttribute().withValue(AwsConfig.builder().build()).build();
    helper.updateSettingAttribute(setting, input, ACCOUNT_ID);

    Assertions.assertThat(setting).isNotNull();
  }
}