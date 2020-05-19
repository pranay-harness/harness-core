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
import software.wings.beans.KubernetesClusterConfig;
import software.wings.beans.SettingAttribute;
import software.wings.graphql.datafetcher.secrets.UsageScopeController;
import software.wings.graphql.schema.mutation.cloudProvider.k8s.QLInheritClusterDetails;
import software.wings.graphql.schema.mutation.cloudProvider.k8s.QLK8sCloudProviderInput;
import software.wings.graphql.schema.type.cloudProvider.k8s.QLClusterDetailsType;

import java.sql.SQLException;

public class K8sDataFetcherHelperTest extends WingsBaseTest {
  private static final String NAME = "K8S";
  private static final String DELEGATE = "DELEGATE";
  private static final String ACCOUNT_ID = "777";

  @Mock private UsageScopeController usageScopeController;

  @InjectMocks private K8sDataFetcherHelper helper = new K8sDataFetcherHelper();

  @Before
  public void setup() throws SQLException {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = IGOR)
  @Category(UnitTests.class)
  public void toSettingAttributeReturnValue() {
    final QLK8sCloudProviderInput input =
        QLK8sCloudProviderInput.builder()
            .name(RequestField.ofNullable(NAME))
            .usageScope(RequestField.ofNullable(usageScope()))
            .skipValidation(RequestField.ofNullable(Boolean.TRUE))
            .clusterDetailsType(RequestField.ofNullable(QLClusterDetailsType.INHERIT_CLUSTER_DETAILS))
            .inheritClusterDetails(RequestField.ofNullable(
                QLInheritClusterDetails.builder().delegateName(RequestField.ofNullable(DELEGATE)).build()))
            .build();

    SettingAttribute setting = helper.toSettingAttribute(input, ACCOUNT_ID);

    Assertions.assertThat(setting).isNotNull();
    Assertions.assertThat(setting.getName()).isEqualTo(NAME);
    Assertions.assertThat(setting.getValue()).isInstanceOf(KubernetesClusterConfig.class);
    KubernetesClusterConfig config = (KubernetesClusterConfig) setting.getValue();
    Assertions.assertThat(config.isSkipValidation()).isEqualTo(Boolean.TRUE);
    Assertions.assertThat(config.isUseKubernetesDelegate()).isTrue();
    Assertions.assertThat(config.getDelegateName()).isEqualTo(DELEGATE);
  }

  @Test
  @Owner(developers = IGOR)
  @Category(UnitTests.class)
  public void toSettingAttributeWithEmptyInput() {
    final QLK8sCloudProviderInput input = QLK8sCloudProviderInput.builder()
                                              .name(RequestField.ofNull())
                                              .usageScope(RequestField.ofNull())
                                              .skipValidation(RequestField.ofNull())
                                              .clusterDetailsType(RequestField.absent())
                                              .inheritClusterDetails(RequestField.ofNull())
                                              .manualClusterDetails(RequestField.ofNull())
                                              .build();

    SettingAttribute setting = helper.toSettingAttribute(input, ACCOUNT_ID);

    Assertions.assertThat(setting).isNotNull();
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = IGOR)
  @Category(UnitTests.class)
  public void toSettingAttributeWithNoClusterDatailsType() {
    final QLK8sCloudProviderInput input = QLK8sCloudProviderInput.builder()
                                              .name(RequestField.ofNull())
                                              .usageScope(RequestField.ofNull())
                                              .skipValidation(RequestField.ofNull())
                                              .clusterDetailsType(RequestField.ofNull())
                                              .inheritClusterDetails(RequestField.ofNull())
                                              .manualClusterDetails(RequestField.ofNull())
                                              .build();

    SettingAttribute setting = helper.toSettingAttribute(input, ACCOUNT_ID);

    Assertions.assertThat(setting).isNotNull();
  }

  @Test
  @Owner(developers = IGOR)
  @Category(UnitTests.class)
  public void updateSettingAttributePerformance() {
    final QLK8sCloudProviderInput input =
        QLK8sCloudProviderInput.builder()
            .name(RequestField.ofNullable(NAME))
            .usageScope(RequestField.ofNullable(usageScope()))
            .skipValidation(RequestField.ofNullable(Boolean.TRUE))
            .clusterDetailsType(RequestField.ofNullable(QLClusterDetailsType.INHERIT_CLUSTER_DETAILS))
            .inheritClusterDetails(RequestField.ofNullable(
                QLInheritClusterDetails.builder().delegateName(RequestField.ofNullable(DELEGATE)).build()))
            .build();

    SettingAttribute setting =
        SettingAttribute.Builder.aSettingAttribute().withValue(KubernetesClusterConfig.builder().build()).build();
    helper.updateSettingAttribute(setting, input, ACCOUNT_ID);

    Assertions.assertThat(setting).isNotNull();
    Assertions.assertThat(setting.getName()).isEqualTo(NAME);
    Assertions.assertThat(setting.getValue()).isInstanceOf(KubernetesClusterConfig.class);
    KubernetesClusterConfig config = (KubernetesClusterConfig) setting.getValue();
    Assertions.assertThat(config.isSkipValidation()).isTrue();
    Assertions.assertThat(config.isUseKubernetesDelegate()).isTrue();
    Assertions.assertThat(config.getDelegateName()).isEqualTo(DELEGATE);
  }

  @Test
  @Owner(developers = IGOR)
  @Category(UnitTests.class)
  public void updateSettingAttributeWithEmptyInput() {
    final QLK8sCloudProviderInput input = QLK8sCloudProviderInput.builder()
                                              .name(RequestField.ofNull())
                                              .usageScope(RequestField.ofNull())
                                              .skipValidation(RequestField.ofNull())
                                              .clusterDetailsType(RequestField.absent())
                                              .inheritClusterDetails(RequestField.ofNull())
                                              .manualClusterDetails(RequestField.ofNull())
                                              .build();

    SettingAttribute setting =
        SettingAttribute.Builder.aSettingAttribute().withValue(KubernetesClusterConfig.builder().build()).build();
    helper.updateSettingAttribute(setting, input, ACCOUNT_ID);

    Assertions.assertThat(setting).isNotNull();
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = IGOR)
  @Category(UnitTests.class)
  public void updateSettingAttributeWithClusterDetailsType() {
    final QLK8sCloudProviderInput input = QLK8sCloudProviderInput.builder()
                                              .name(RequestField.ofNull())
                                              .usageScope(RequestField.ofNull())
                                              .skipValidation(RequestField.ofNull())
                                              .clusterDetailsType(RequestField.ofNull())
                                              .inheritClusterDetails(RequestField.ofNull())
                                              .manualClusterDetails(RequestField.ofNull())
                                              .build();

    SettingAttribute setting =
        SettingAttribute.Builder.aSettingAttribute().withValue(KubernetesClusterConfig.builder().build()).build();
    helper.updateSettingAttribute(setting, input, ACCOUNT_ID);

    Assertions.assertThat(setting).isNotNull();
  }
}