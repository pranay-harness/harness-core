package software.wings.graphql.datafetcher.cloudProvider;

import static io.harness.rule.OwnerRule.IGOR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.graphql.datafetcher.cloudProvider.CreateCloudProviderDataFetcherTest.usageScope;

import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;
import io.harness.utils.RequestField;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.wings.beans.AzureConfig;
import software.wings.beans.GcpConfig;
import software.wings.beans.KubernetesClusterConfig;
import software.wings.beans.PcfConfig;
import software.wings.beans.PhysicalDataCenterConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SpotInstConfig;
import software.wings.graphql.datafetcher.AbstractDataFetcherTest;
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.graphql.schema.mutation.cloudProvider.QLAzureCloudProviderInput;
import software.wings.graphql.schema.mutation.cloudProvider.QLGcpCloudProviderInput;
import software.wings.graphql.schema.mutation.cloudProvider.QLPcfCloudProviderInput;
import software.wings.graphql.schema.mutation.cloudProvider.QLPhysicalDataCenterCloudProviderInput;
import software.wings.graphql.schema.mutation.cloudProvider.QLSpotInstCloudProviderInput;
import software.wings.graphql.schema.mutation.cloudProvider.QLUpdateCloudProviderInput;
import software.wings.graphql.schema.mutation.cloudProvider.QLUpdateCloudProviderPayload;
import software.wings.graphql.schema.mutation.cloudProvider.k8s.QLInheritClusterDetails;
import software.wings.graphql.schema.mutation.cloudProvider.k8s.QLK8sCloudProviderInput;
import software.wings.graphql.schema.type.QLCloudProviderType;
import software.wings.graphql.schema.type.cloudProvider.QLAzureCloudProvider;
import software.wings.graphql.schema.type.cloudProvider.QLGcpCloudProvider;
import software.wings.graphql.schema.type.cloudProvider.QLKubernetesClusterCloudProvider;
import software.wings.graphql.schema.type.cloudProvider.QLPcfCloudProvider;
import software.wings.graphql.schema.type.cloudProvider.QLPhysicalDataCenterCloudProvider;
import software.wings.graphql.schema.type.cloudProvider.QLSpotInstCloudProvider;
import software.wings.graphql.schema.type.cloudProvider.k8s.QLClusterDetailsType;
import software.wings.service.impl.SettingServiceHelper;
import software.wings.service.intfc.SettingsService;
import software.wings.settings.SettingValue;

import java.sql.SQLException;

public class UpdateCloudProviderDataFetcherTest extends AbstractDataFetcherTest {
  private static final String CLOUD_PROVIDER_ID = "CP-ID";
  private static final String ACCOUNT_ID = "ACCOUNT-ID";

  @Mock private SettingsService settingsService;
  @Mock private SettingServiceHelper settingServiceHelper;
  @Mock private PcfDataFetcherHelper pcfDataFetcherHelper;
  @Mock private SpotInstDataFetcherHelper spotInstDataFetcherHelper;
  @Mock private GcpDataFetcherHelper gcpDataFetcherHelper;
  @Mock private K8sDataFetcherHelper k8sDataFetcherHelper;
  @Mock private PhysicalDataCenterDataFetcherHelper physicalDataCenterDataFetcherHelper;
  @Mock private AzureDataFetcherHelper azureDataFetcherHelper;

  @InjectMocks private UpdateCloudProviderDataFetcher dataFetcher = new UpdateCloudProviderDataFetcher();

  @Before
  public void setup() throws SQLException {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = IGOR)
  @Category(UnitTests.class)
  public void updatePcf() {
    SettingAttribute setting = SettingAttribute.Builder.aSettingAttribute()
                                   .withCategory(SettingAttribute.SettingCategory.CLOUD_PROVIDER)
                                   .withValue(PcfConfig.builder().accountId(ACCOUNT_ID).build())
                                   .build();

    doReturn(setting).when(settingsService).getByAccount(ACCOUNT_ID, CLOUD_PROVIDER_ID);

    doNothing()
        .when(pcfDataFetcherHelper)
        .updateSettingAttribute(isA(SettingAttribute.class), isA(QLPcfCloudProviderInput.class), isA(String.class));

    doReturn(SettingAttribute.Builder.aSettingAttribute()
                 .withCategory(SettingAttribute.SettingCategory.CLOUD_PROVIDER)
                 .withValue(PcfConfig.builder().accountId(ACCOUNT_ID).build())
                 .build())
        .when(settingsService)
        .updateWithSettingFields(setting, setting.getUuid(), GLOBAL_APP_ID);

    doNothing()
        .when(settingServiceHelper)
        .updateSettingAttributeBeforeResponse(isA(SettingAttribute.class), isA(Boolean.class));

    QLUpdateCloudProviderPayload payload =
        dataFetcher.mutateAndFetch(QLUpdateCloudProviderInput.builder()
                                       .cloudProviderId(CLOUD_PROVIDER_ID)
                                       .cloudProviderType(QLCloudProviderType.PCF)
                                       .pcfCloudProvider(QLPcfCloudProviderInput.builder()
                                                             .name(RequestField.ofNullable("NAME"))
                                                             .endpointUrl(RequestField.ofNullable("URL"))
                                                             .userName(RequestField.ofNullable("USER"))
                                                             .passwordSecretId(RequestField.ofNullable("PASS"))
                                                             .usageScope(RequestField.ofNull())
                                                             .build())
                                       .build(),
            MutationContext.builder().accountId(ACCOUNT_ID).build());

    verify(settingsService, times(1)).getByAccount(ACCOUNT_ID, CLOUD_PROVIDER_ID);
    verify(settingsService, times(1)).updateWithSettingFields(setting, setting.getUuid(), GLOBAL_APP_ID);
    verify(settingServiceHelper, times(1))
        .updateSettingAttributeBeforeResponse(isA(SettingAttribute.class), isA(Boolean.class));

    assertThat(payload.getCloudProvider()).isNotNull();
    assertThat(payload.getCloudProvider()).isInstanceOf(QLPcfCloudProvider.class);
  }

  @Test
  @Owner(developers = IGOR)
  @Category(UnitTests.class)
  public void updateSpotInst() {
    SettingAttribute setting = SettingAttribute.Builder.aSettingAttribute()
                                   .withCategory(SettingAttribute.SettingCategory.CLOUD_PROVIDER)
                                   .withValue(SpotInstConfig.builder().accountId(ACCOUNT_ID).build())
                                   .build();

    doReturn(setting).when(settingsService).getByAccount(ACCOUNT_ID, CLOUD_PROVIDER_ID);

    doReturn(SettingAttribute.Builder.aSettingAttribute()
                 .withUuid(CLOUD_PROVIDER_ID)
                 .withCategory(SettingAttribute.SettingCategory.CLOUD_PROVIDER)
                 .withValue(SpotInstConfig.builder().accountId(ACCOUNT_ID).build())
                 .build())
        .when(settingsService)
        .updateWithSettingFields(setting, setting.getUuid(), GLOBAL_APP_ID);

    doNothing()
        .when(settingServiceHelper)
        .updateSettingAttributeBeforeResponse(isA(SettingAttribute.class), isA(Boolean.class));

    QLUpdateCloudProviderPayload payload = dataFetcher.mutateAndFetch(
        QLUpdateCloudProviderInput.builder()
            .cloudProviderId(CLOUD_PROVIDER_ID)
            .cloudProviderType(QLCloudProviderType.SPOT_INST)
            .spotInstCloudProvider(QLSpotInstCloudProviderInput.builder()
                                       .name(RequestField.ofNullable("NAME"))
                                       .accountId(RequestField.ofNullable("SpotInstAccountId"))
                                       .tokenSecretId(RequestField.ofNullable("SpotInstToken"))
                                       .usageScope(RequestField.ofNullable(usageScope()))
                                       .build())
            .build(),
        MutationContext.builder().accountId(ACCOUNT_ID).build());

    verify(settingsService, times(1)).getByAccount(ACCOUNT_ID, CLOUD_PROVIDER_ID);
    verify(settingsService, times(1)).updateWithSettingFields(setting, setting.getUuid(), GLOBAL_APP_ID);
    verify(settingServiceHelper, times(1))
        .updateSettingAttributeBeforeResponse(isA(SettingAttribute.class), isA(Boolean.class));

    assertThat(payload.getCloudProvider()).isNotNull();
    assertThat(payload.getCloudProvider()).isInstanceOf(QLSpotInstCloudProvider.class);
    assertThat(payload.getCloudProvider().getId()).isEqualTo(CLOUD_PROVIDER_ID);
  }

  @Test
  @Owner(developers = IGOR)
  @Category(UnitTests.class)
  public void updateGcp() {
    SettingAttribute setting = SettingAttribute.Builder.aSettingAttribute()
                                   .withCategory(SettingAttribute.SettingCategory.CLOUD_PROVIDER)
                                   .withValue(GcpConfig.builder().accountId(ACCOUNT_ID).build())
                                   .build();

    doReturn(setting).when(settingsService).getByAccount(ACCOUNT_ID, CLOUD_PROVIDER_ID);

    doReturn(SettingAttribute.Builder.aSettingAttribute()
                 .withUuid(CLOUD_PROVIDER_ID)
                 .withCategory(SettingAttribute.SettingCategory.CLOUD_PROVIDER)
                 .withValue(GcpConfig.builder().accountId(ACCOUNT_ID).build())
                 .build())
        .when(settingsService)
        .updateWithSettingFields(setting, setting.getUuid(), GLOBAL_APP_ID);

    doNothing()
        .when(settingServiceHelper)
        .updateSettingAttributeBeforeResponse(isA(SettingAttribute.class), isA(Boolean.class));

    QLUpdateCloudProviderPayload payload =
        dataFetcher.mutateAndFetch(QLUpdateCloudProviderInput.builder()
                                       .cloudProviderId(CLOUD_PROVIDER_ID)
                                       .cloudProviderType(QLCloudProviderType.GCP)
                                       .gcpCloudProvider(QLGcpCloudProviderInput.builder()
                                                             .name(RequestField.ofNullable("NAME"))
                                                             .serviceAccountKeySecretId(RequestField.ofNullable("Key"))
                                                             .usageScope(RequestField.ofNullable(usageScope()))
                                                             .build())
                                       .build(),
            MutationContext.builder().accountId(ACCOUNT_ID).build());

    verify(settingsService, times(1)).getByAccount(ACCOUNT_ID, CLOUD_PROVIDER_ID);
    verify(settingsService, times(1)).updateWithSettingFields(setting, setting.getUuid(), GLOBAL_APP_ID);
    verify(settingServiceHelper, times(1))
        .updateSettingAttributeBeforeResponse(isA(SettingAttribute.class), isA(Boolean.class));

    assertThat(payload.getCloudProvider()).isNotNull();
    assertThat(payload.getCloudProvider()).isInstanceOf(QLGcpCloudProvider.class);
    assertThat(payload.getCloudProvider().getId()).isEqualTo(CLOUD_PROVIDER_ID);
  }

  @Test
  @Owner(developers = IGOR)
  @Category(UnitTests.class)
  public void updateK8s() {
    SettingAttribute setting = SettingAttribute.Builder.aSettingAttribute()
                                   .withCategory(SettingAttribute.SettingCategory.CLOUD_PROVIDER)
                                   .withValue(KubernetesClusterConfig.builder().accountId(ACCOUNT_ID).build())
                                   .build();

    doReturn(setting).when(settingsService).getByAccount(ACCOUNT_ID, CLOUD_PROVIDER_ID);

    doReturn(SettingAttribute.Builder.aSettingAttribute()
                 .withUuid(CLOUD_PROVIDER_ID)
                 .withCategory(SettingAttribute.SettingCategory.CLOUD_PROVIDER)
                 .withValue(KubernetesClusterConfig.builder().accountId(ACCOUNT_ID).build())
                 .build())
        .when(settingsService)
        .updateWithSettingFields(setting, setting.getUuid(), GLOBAL_APP_ID);

    doNothing()
        .when(settingServiceHelper)
        .updateSettingAttributeBeforeResponse(isA(SettingAttribute.class), isA(Boolean.class));

    QLUpdateCloudProviderPayload payload = dataFetcher.mutateAndFetch(
        QLUpdateCloudProviderInput.builder()
            .cloudProviderId(CLOUD_PROVIDER_ID)
            .cloudProviderType(QLCloudProviderType.KUBERNETES_CLUSTER)
            .k8sCloudProvider(
                QLK8sCloudProviderInput.builder()
                    .name(RequestField.ofNullable("K8S"))
                    .usageScope(RequestField.ofNullable(usageScope()))
                    .skipValidation(RequestField.ofNullable(Boolean.TRUE))
                    .clusterDetailsType(RequestField.ofNullable(QLClusterDetailsType.INHERIT_CLUSTER_DETAILS))
                    .inheritClusterDetails(RequestField.ofNullable(
                        QLInheritClusterDetails.builder().delegateName(RequestField.ofNullable("DELEGATE")).build()))
                    .build())
            .build(),
        MutationContext.builder().accountId(ACCOUNT_ID).build());

    verify(settingsService, times(1)).getByAccount(ACCOUNT_ID, CLOUD_PROVIDER_ID);
    verify(settingsService, times(1)).updateWithSettingFields(setting, setting.getUuid(), GLOBAL_APP_ID);
    verify(settingServiceHelper, times(1))
        .updateSettingAttributeBeforeResponse(isA(SettingAttribute.class), isA(Boolean.class));

    assertThat(payload.getCloudProvider()).isNotNull();
    assertThat(payload.getCloudProvider()).isInstanceOf(QLKubernetesClusterCloudProvider.class);
    assertThat(payload.getCloudProvider().getId()).isEqualTo(CLOUD_PROVIDER_ID);
  }

  @Test
  @Owner(developers = IGOR)
  @Category(UnitTests.class)
  public void updatePhysicalDataCenter() {
    SettingAttribute setting =
        SettingAttribute.Builder.aSettingAttribute()
            .withCategory(SettingAttribute.SettingCategory.CLOUD_PROVIDER)
            .withValue(PhysicalDataCenterConfig.Builder.aPhysicalDataCenterConfig()
                           .withType(SettingValue.SettingVariableTypes.PHYSICAL_DATA_CENTER.name())
                           .build())
            .build();

    doReturn(setting).when(settingsService).getByAccount(ACCOUNT_ID, CLOUD_PROVIDER_ID);

    doNothing()
        .when(physicalDataCenterDataFetcherHelper)
        .updateSettingAttribute(
            isA(SettingAttribute.class), isA(QLPhysicalDataCenterCloudProviderInput.class), isA(String.class));

    doReturn(SettingAttribute.Builder.aSettingAttribute()
                 .withCategory(SettingAttribute.SettingCategory.CLOUD_PROVIDER)
                 .withValue(PhysicalDataCenterConfig.Builder.aPhysicalDataCenterConfig()
                                .withType(SettingValue.SettingVariableTypes.PHYSICAL_DATA_CENTER.name())
                                .build())
                 .build())
        .when(settingsService)
        .updateWithSettingFields(setting, setting.getUuid(), GLOBAL_APP_ID);

    doNothing()
        .when(settingServiceHelper)
        .updateSettingAttributeBeforeResponse(isA(SettingAttribute.class), isA(Boolean.class));

    QLUpdateCloudProviderPayload payload =
        dataFetcher.mutateAndFetch(QLUpdateCloudProviderInput.builder()
                                       .cloudProviderId(CLOUD_PROVIDER_ID)
                                       .cloudProviderType(QLCloudProviderType.PHYSICAL_DATA_CENTER)
                                       .physicalDataCenterCloudProvider(QLPhysicalDataCenterCloudProviderInput.builder()
                                                                            .name(RequestField.ofNullable("NAME"))
                                                                            .usageScope(RequestField.ofNull())
                                                                            .build())
                                       .build(),
            MutationContext.builder().accountId(ACCOUNT_ID).build());

    verify(settingsService, times(1)).getByAccount(ACCOUNT_ID, CLOUD_PROVIDER_ID);
    verify(settingsService, times(1)).updateWithSettingFields(setting, setting.getUuid(), GLOBAL_APP_ID);
    verify(settingServiceHelper, times(1))
        .updateSettingAttributeBeforeResponse(isA(SettingAttribute.class), isA(Boolean.class));

    assertThat(payload.getCloudProvider()).isNotNull();
    assertThat(payload.getCloudProvider()).isInstanceOf(QLPhysicalDataCenterCloudProvider.class);
  }

  @Test
  @Owner(developers = IGOR)
  @Category(UnitTests.class)
  public void updateAzure() {
    SettingAttribute setting = SettingAttribute.Builder.aSettingAttribute()
                                   .withCategory(SettingAttribute.SettingCategory.CLOUD_PROVIDER)
                                   .withValue(AzureConfig.builder().accountId(ACCOUNT_ID).build())
                                   .build();

    doReturn(setting).when(settingsService).getByAccount(ACCOUNT_ID, CLOUD_PROVIDER_ID);

    doReturn(SettingAttribute.Builder.aSettingAttribute()
                 .withUuid(CLOUD_PROVIDER_ID)
                 .withCategory(SettingAttribute.SettingCategory.CLOUD_PROVIDER)
                 .withValue(AzureConfig.builder().accountId(ACCOUNT_ID).build())
                 .build())
        .when(settingsService)
        .updateWithSettingFields(setting, setting.getUuid(), GLOBAL_APP_ID);

    doNothing()
        .when(settingServiceHelper)
        .updateSettingAttributeBeforeResponse(isA(SettingAttribute.class), isA(Boolean.class));

    QLUpdateCloudProviderPayload payload =
        dataFetcher.mutateAndFetch(QLUpdateCloudProviderInput.builder()
                                       .cloudProviderId(CLOUD_PROVIDER_ID)
                                       .cloudProviderType(QLCloudProviderType.AZURE)
                                       .azureCloudProvider(QLAzureCloudProviderInput.builder()
                                                               .name(RequestField.ofNullable("Azure"))
                                                               .usageScope(RequestField.ofNullable(usageScope()))
                                                               .clientId(RequestField.ofNullable("CLIENT_ID"))
                                                               .tenantId(RequestField.ofNullable("TENANT_ID"))
                                                               .keySecretId(RequestField.ofNullable("KEY"))
                                                               .build())
                                       .build(),
            MutationContext.builder().accountId(ACCOUNT_ID).build());

    verify(settingsService, times(1)).getByAccount(ACCOUNT_ID, CLOUD_PROVIDER_ID);
    verify(settingsService, times(1)).updateWithSettingFields(setting, setting.getUuid(), GLOBAL_APP_ID);
    verify(settingServiceHelper, times(1))
        .updateSettingAttributeBeforeResponse(isA(SettingAttribute.class), isA(Boolean.class));

    assertThat(payload.getCloudProvider()).isNotNull();
    assertThat(payload.getCloudProvider()).isInstanceOf(QLAzureCloudProvider.class);
    assertThat(payload.getCloudProvider().getId()).isEqualTo(CLOUD_PROVIDER_ID);
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = IGOR)
  @Category(UnitTests.class)
  public void updateOfNonExistingSetting() {
    doReturn(null).when(settingsService).getByAccount(ACCOUNT_ID, CLOUD_PROVIDER_ID);

    dataFetcher.mutateAndFetch(QLUpdateCloudProviderInput.builder()
                                   .cloudProviderId(CLOUD_PROVIDER_ID)
                                   .cloudProviderType(QLCloudProviderType.PCF)
                                   .pcfCloudProvider(QLPcfCloudProviderInput.builder()
                                                         .name(RequestField.ofNullable("NAME"))
                                                         .endpointUrl(RequestField.ofNullable("URL"))
                                                         .userName(RequestField.ofNullable("USER"))
                                                         .passwordSecretId(RequestField.ofNullable("PASS"))
                                                         .usageScope(RequestField.ofNull())
                                                         .build())
                                   .build(),
        MutationContext.builder().accountId(ACCOUNT_ID).build());
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = IGOR)
  @Category(UnitTests.class)
  public void updateWithoutIdParameter() {
    dataFetcher.mutateAndFetch(QLUpdateCloudProviderInput.builder().cloudProviderType(QLCloudProviderType.PCF).build(),
        MutationContext.builder().accountId(ACCOUNT_ID).build());
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = IGOR)
  @Category(UnitTests.class)
  public void updateWithoutTypeParameter() {
    dataFetcher.mutateAndFetch(QLUpdateCloudProviderInput.builder().cloudProviderId(CLOUD_PROVIDER_ID).build(),
        MutationContext.builder().accountId(ACCOUNT_ID).build());
  }
}
