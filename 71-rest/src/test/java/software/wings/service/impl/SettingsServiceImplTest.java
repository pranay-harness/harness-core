package software.wings.service.impl;

import static io.harness.ccm.license.CeLicenseType.LIMITED_TRIAL;
import static io.harness.rule.OwnerRule.ARVIND;
import static io.harness.rule.OwnerRule.HANTANG;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anySet;
import static org.mockito.Matchers.anySetOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;

import com.google.inject.name.Named;

import io.harness.category.element.UnitTests;
import io.harness.ccm.config.CCMConfig;
import io.harness.ccm.config.CCMSettingService;
import io.harness.ccm.license.CeLicenseInfo;
import io.harness.ccm.setup.service.support.intfc.AWSCEConfigValidationService;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.AwsCrossAccountAttributes;
import software.wings.beans.AwsS3BucketDetails;
import software.wings.beans.GitConfig;
import software.wings.beans.KubernetesClusterConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingCategory;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.appmanifest.StoreType;
import software.wings.beans.ce.CEAwsConfig;
import software.wings.beans.ce.CEGcpConfig;
import software.wings.beans.settings.helm.GCSHelmRepoConfig;
import software.wings.beans.settings.helm.HttpHelmRepoConfig;
import software.wings.features.CeCloudAccountFeature;
import software.wings.features.GitOpsFeature;
import software.wings.features.api.UsageLimitedFeature;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ApplicationManifestService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.UsageRestrictionsService;
import software.wings.service.intfc.security.SecretManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SettingsServiceImplTest extends WingsBaseTest {
  private static final String PASSWORD = "PASSWORD";
  private static final String S3_REGION = "us-east-1";
  private static final String S3_BUCKET_NAME = "ceBucket";
  private static final String S3_BUCKET_PREFIX = "prefix";
  private static final String ROLE_ARN = "arn:aws:iam::830767422336:role/harnessCERole";
  private static final String ACCOUNT_ID = "ACCOUNT_ID";
  private Account account;
  private SettingAttribute settingAttribute;

  @Mock private ApplicationManifestService applicationManifestService;
  @Mock private EnvironmentService environmentService;
  @Mock private AppService appService;
  @Mock private ServiceResourceService serviceResourceService;
  @Mock private SecretManager secretManager;
  @Mock private UsageRestrictionsService usageRestrictionsService;
  @Mock private SettingServiceHelper settingServiceHelper;
  @Mock private AWSCEConfigValidationService awsCeConfigService;
  @Mock private AccountService accountService;
  @Mock private CCMSettingService ccmSettingService;
  @Mock private SettingAttributeDao settingAttributeDao;
  @Mock @Named(GitOpsFeature.FEATURE_NAME) private UsageLimitedFeature gitOpsFeature;
  @Mock @Named(CeCloudAccountFeature.FEATURE_NAME) private UsageLimitedFeature ceCloudAccountFeature;

  @Spy @InjectMocks private SettingsServiceImpl settingsService;

  @Before
  public void setUp() {
    Account account = Account.Builder.anAccount()
                          .withCeLicenseInfo(CeLicenseInfo.builder().licenseType(LIMITED_TRIAL).build())
                          .build();
    when(accountService.get(eq(ACCOUNT_ID))).thenReturn(account);
    KubernetesClusterConfig kubernetesClusterConfig = KubernetesClusterConfig.builder().build();
    settingAttribute = SettingAttribute.Builder.aSettingAttribute()
                           .withAccountId(ACCOUNT_ID)
                           .withValue(kubernetesClusterConfig)
                           .build();
    when(ccmSettingService.isCloudCostEnabled(any(SettingAttribute.class))).thenReturn(true);
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void shouldReturnIfNotExceedingCeTrialLimit() {
    when(settingAttributeDao.list(eq(ACCOUNT_ID), any(SettingCategory.class))).thenReturn(new ArrayList<>());
    settingsService.checkCeTrialLimit(settingAttribute);
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void shouldThrowIfExceedingCeTrialLimit() {
    CCMConfig ccmConfig = CCMConfig.builder().cloudCostEnabled(true).build();
    SettingAttribute settingAttribute1 =
        aSettingAttribute().withValue(KubernetesClusterConfig.builder().ccmConfig(ccmConfig).build()).build();
    SettingAttribute settingAttribute2 =
        aSettingAttribute().withValue(KubernetesClusterConfig.builder().ccmConfig(ccmConfig).build()).build();
    List<SettingAttribute> settingAttributes = Arrays.asList(settingAttribute1, settingAttribute2);
    when(settingAttributeDao.list(eq(ACCOUNT_ID), any(SettingCategory.class))).thenReturn(settingAttributes);
    settingsService.checkCeTrialLimit(settingAttribute);
  }

  @Test
  @Owner(developers = OwnerRule.ROHIT)
  @Category(UnitTests.class)
  public void testValidateAndUpdateCEDetailsMethod() {
    SettingAttribute attribute = new SettingAttribute();
    attribute.setCategory(SettingCategory.CE_CONNECTOR);
    CEAwsConfig ceAwsConfig =
        CEAwsConfig.builder()
            .s3BucketDetails(AwsS3BucketDetails.builder().s3BucketName(S3_BUCKET_NAME).build())
            .awsCrossAccountAttributes(AwsCrossAccountAttributes.builder().crossAccountRoleArn(ROLE_ARN).build())
            .build();
    attribute.setValue(ceAwsConfig);
    doReturn(AwsS3BucketDetails.builder().s3Prefix(S3_BUCKET_PREFIX).region(S3_REGION).build())
        .when(awsCeConfigService)
        .validateCURReportAccessAndReturnS3Config(ceAwsConfig);
    attribute.setAccountId(ACCOUNT_ID);
    when(ceCloudAccountFeature.getMaxUsageAllowedForAccount(ACCOUNT_ID)).thenReturn(2);
    when(ccmSettingService.listCeCloudAccounts(ACCOUNT_ID)).thenReturn(Collections.emptyList());
    settingsService.validateAndUpdateCEDetails(attribute, true);
    CEAwsConfig modifiedConfig = (CEAwsConfig) attribute.getValue();
    assertThat(modifiedConfig.getAwsAccountId()).isEqualTo("830767422336");
    assertThat(modifiedConfig.getAwsMasterAccountId()).isEqualTo("830767422336");
    assertThat(modifiedConfig.getS3BucketDetails())
        .isEqualTo(AwsS3BucketDetails.builder()
                       .s3BucketName(S3_BUCKET_NAME)
                       .s3Prefix(S3_BUCKET_PREFIX)
                       .region(S3_REGION)
                       .build());
  }

  @Test
  @Owner(developers = OwnerRule.ROHIT)
  @Category(UnitTests.class)
  public void testAllowOnlyOneAWSConnectorCase() {
    SettingAttribute attribute = new SettingAttribute();
    attribute.setCategory(SettingCategory.CE_CONNECTOR);
    CEAwsConfig ceAwsConfig =
        CEAwsConfig.builder()
            .s3BucketDetails(AwsS3BucketDetails.builder().s3BucketName(S3_BUCKET_NAME).build())
            .awsCrossAccountAttributes(AwsCrossAccountAttributes.builder().crossAccountRoleArn(ROLE_ARN).build())
            .build();
    attribute.setValue(ceAwsConfig);
    doReturn(AwsS3BucketDetails.builder().s3Prefix(S3_BUCKET_PREFIX).region(S3_REGION).build())
        .when(awsCeConfigService)
        .validateCURReportAccessAndReturnS3Config(ceAwsConfig);
    attribute.setAccountId(ACCOUNT_ID);
    when(ceCloudAccountFeature.getMaxUsageAllowedForAccount(ACCOUNT_ID)).thenReturn(2);
    when(ccmSettingService.listCeCloudAccounts(ACCOUNT_ID)).thenReturn(Collections.singletonList(attribute));

    assertThatExceptionOfType(InvalidRequestException.class)
        .isThrownBy(() -> settingsService.validateAndUpdateCEDetails(attribute, true))
        .withMessage("Cannot enable continuous efficiency for more than 1 AWS cloud account");
  }

  @Test
  @Owner(developers = OwnerRule.ROHIT)
  @Category(UnitTests.class)
  public void testAllowOnlyOneGCPConnectorCase() {
    SettingAttribute attribute = new SettingAttribute();
    attribute.setCategory(SettingCategory.CE_CONNECTOR);
    CEGcpConfig ceGcpConfig = CEGcpConfig.builder().organizationSettingId("orgSettingId").build();

    attribute.setValue(ceGcpConfig);
    attribute.setAccountId(ACCOUNT_ID);
    when(ceCloudAccountFeature.getMaxUsageAllowedForAccount(ACCOUNT_ID)).thenReturn(2);
    when(ccmSettingService.listCeCloudAccounts(ACCOUNT_ID)).thenReturn(Collections.singletonList(attribute));

    assertThatExceptionOfType(InvalidRequestException.class)
        .isThrownBy(() -> settingsService.validateAndUpdateCEDetails(attribute, true))
        .withMessage("Cannot enable continuous efficiency for more than 1 GCP cloud account");
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void testEnsureHelmConnectorSafeToDelete() throws IllegalAccessException {
    FieldUtils.writeField(settingsService, "wingsPersistence", wingsPersistence, true);
    SettingAttribute helmConnector =
        SettingAttribute.Builder.aSettingAttribute()
            .withAccountId(ACCOUNT_ID)
            .withName("http-helm")
            .withUuid("id-1")
            .withCategory(SettingCategory.HELM_REPO)
            .withValue(HttpHelmRepoConfig.builder().chartRepoUrl("http://stable-charts").accountId(ACCOUNT_ID).build())
            .build();
    settingsService.ensureSettingAttributeSafeToDelete(helmConnector);

    shouldNotDeleteIfReferencedInService(helmConnector);
    shouldNotDeleteIfReferencedInEnv(helmConnector);
    shouldNotDeleteIfReferencedInEnvAndService(helmConnector);
  }

  private void shouldNotDeleteIfReferencedInService(SettingAttribute helmConnector) {
    doReturn(asList(helmChartManifestWithIds("s-1", null)))
        .when(applicationManifestService)
        .getAllByConnectorId(anyString(), eq(helmConnector.getUuid()), anySet());
    doReturn(asList("service-1")).when(serviceResourceService).getNames(anyString(), eq(asList("s-1")));

    assertThatExceptionOfType(InvalidRequestException.class)
        .isThrownBy(() -> settingsService.ensureSettingAttributeSafeToDelete(helmConnector))
        .withMessage("Helm Connector [http-helm] is referenced by [1] service(s) [service-1] ");

    doReturn(asList(helmChartManifestWithIds("s-1", null), helmChartManifestWithIds("s-2", null),
                 helmChartManifestWithIds("s-3", null), helmChartManifestWithIds("s-4", null),
                 helmChartManifestWithIds("s-5", null), helmChartManifestWithIds("s-6", null)))
        .when(applicationManifestService)
        .getAllByConnectorId(anyString(), eq(helmConnector.getUuid()), anySet());
    doReturn(asList("service-1", "service-2", "service-3", "service-4", "service-5", "service-6"))
        .when(serviceResourceService)
        .getNames(anyString(), anyList());

    assertThatExceptionOfType(InvalidRequestException.class)
        .isThrownBy(() -> settingsService.ensureSettingAttributeSafeToDelete(helmConnector))
        .withMessage(
            "Helm Connector [http-helm] is referenced by [6] service(s) [service-1, service-2, service-3, service-4, service-5]  and [1] more..");
  }

  private void shouldNotDeleteIfReferencedInEnv(SettingAttribute helmConnector) {
    doReturn(asList(helmChartManifestWithIds("s-1", "env-1")))
        .when(applicationManifestService)
        .getAllByConnectorId(anyString(), eq(helmConnector.getUuid()), anySet());
    doReturn(asList("env-1")).when(environmentService).getNames(anyString(), eq(asList("env-1")));
    doReturn(Collections.emptyList()).when(serviceResourceService).getNames(anyString(), anyList());

    assertThatExceptionOfType(InvalidRequestException.class)
        .isThrownBy(() -> settingsService.ensureSettingAttributeSafeToDelete(helmConnector))
        .withMessage("Helm Connector [http-helm] is referenced and by [1] override in environment(s) [env-1] ");

    doReturn(asList(helmChartManifestWithIds("s-1", "e-1"), helmChartManifestWithIds("s-2", "e-1"),
                 helmChartManifestWithIds("s-3", "e-2"), helmChartManifestWithIds("s-4", "e-3"),
                 helmChartManifestWithIds(null, "e-4"), helmChartManifestWithIds("s-6", "e-5"),
                 helmChartManifestWithIds(null, "e-4"), helmChartManifestWithIds("s-2", "e-6")))
        .when(applicationManifestService)
        .getAllByConnectorId(anyString(), eq(helmConnector.getUuid()), anySet());
    doReturn(asList("env-1", "env-2", "env-3", "env-4", "env-5", "env-6"))
        .when(environmentService)
        .getNames(anyString(), anyList());
    doReturn(Collections.emptyList()).when(serviceResourceService).getNames(anyString(), anyList());

    assertThatExceptionOfType(InvalidRequestException.class)
        .isThrownBy(() -> settingsService.ensureSettingAttributeSafeToDelete(helmConnector))
        .withMessage(
            "Helm Connector [http-helm] is referenced and by [6] override in environment(s) [env-1, env-2, env-3, env-4, env-5] and [1] more..");
  }

  private void shouldNotDeleteIfReferencedInEnvAndService(SettingAttribute helmConnector) {
    List<ApplicationManifest> onlyServiceRef =
        asList(helmChartManifestWithIds("s-1", null), helmChartManifestWithIds("s-2", null),
            helmChartManifestWithIds("s-3", null), helmChartManifestWithIds("s-4", null),
            helmChartManifestWithIds("s-5", null), helmChartManifestWithIds("s-6", null));
    List<ApplicationManifest> envRef =
        asList(helmChartManifestWithIds("s-1", "e-1"), helmChartManifestWithIds("s-2", "e-1"),
            helmChartManifestWithIds("s-3", "e-2"), helmChartManifestWithIds("s-4", "e-3"),
            helmChartManifestWithIds(null, "e-4"), helmChartManifestWithIds("s-6", "e-5"),
            helmChartManifestWithIds(null, "e-4"), helmChartManifestWithIds("s-2", "e-6"));

    doReturn(Stream.concat(onlyServiceRef.stream(), envRef.stream()).collect(Collectors.toList()))
        .when(applicationManifestService)
        .getAllByConnectorId(anyString(), eq(helmConnector.getUuid()), anySet());
    doReturn(asList("env-1", "env-2", "env-3", "env-4", "env-5", "env-6"))
        .when(environmentService)
        .getNames(anyString(), anyList());
    doReturn(asList("service-1", "service-2", "service-3", "service-4", "service-5", "service-6"))
        .when(serviceResourceService)
        .getNames(anyString(), anyList());

    assertThatExceptionOfType(InvalidRequestException.class)
        .isThrownBy(() -> settingsService.ensureSettingAttributeSafeToDelete(helmConnector))
        .withMessage(
            "Helm Connector [http-helm] is referenced by [6] service(s) [service-1, service-2, service-3, service-4, service-5]  and [1] more..and by [6] override in environment(s) [env-1, env-2, env-3, env-4, env-5] and [1] more..");
  }

  private ApplicationManifest helmChartManifestWithIds(String serviceId, String envId) {
    return ApplicationManifest.builder().storeType(StoreType.HelmChartRepo).serviceId(serviceId).envId(envId).build();
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void testIsFilteredSettingAttribute() {
    SettingAttribute helmConnector = SettingAttribute.Builder.aSettingAttribute()
                                         .withAccountId(ACCOUNT_ID)
                                         .withName("http-helm")
                                         .withUuid("id-1")
                                         .withCategory(SettingCategory.HELM_REPO)
                                         .withValue(GCSHelmRepoConfig.builder().accountId(ACCOUNT_ID).build())
                                         .build();

    when(settingServiceHelper.hasReferencedSecrets(eq(helmConnector))).thenReturn(false);
    settingsService.isFilteredSettingAttribute(null, null, ACCOUNT_ID, null, null, false, null, null, helmConnector);
    verify(secretManager, never())
        .canUseSecretsInAppAndEnv(anySetOf(String.class), eq(ACCOUNT_ID), any(), any(), eq(false), any(), any(), any());
    verify(usageRestrictionsService, times(1))
        .hasAccess(eq(ACCOUNT_ID), eq(false), any(), any(), any(), any(), any(), any(), anyBoolean());

    when(settingServiceHelper.hasReferencedSecrets(eq(helmConnector))).thenReturn(true);
    when(settingServiceHelper.getUsedSecretIds(helmConnector)).thenReturn(null);
    settingsService.isFilteredSettingAttribute(null, null, ACCOUNT_ID, null, null, false, null, null, helmConnector);
    verify(secretManager, never())
        .canUseSecretsInAppAndEnv(anySetOf(String.class), eq(ACCOUNT_ID), any(), any(), eq(false), any(), any(), any());
    verify(usageRestrictionsService, times(2))
        .hasAccess(eq(ACCOUNT_ID), eq(false), any(), any(), any(), any(), any(), any(), anyBoolean());

    when(settingServiceHelper.getUsedSecretIds(helmConnector)).thenReturn(Collections.emptySet());
    settingsService.isFilteredSettingAttribute(null, null, ACCOUNT_ID, null, null, false, null, null, helmConnector);
    verify(secretManager, never())
        .canUseSecretsInAppAndEnv(anySetOf(String.class), eq(ACCOUNT_ID), any(), any(), eq(false), any(), any(), any());
    verify(usageRestrictionsService, times(3))
        .hasAccess(eq(ACCOUNT_ID), eq(false), any(), any(), any(), any(), any(), any(), anyBoolean());

    when(settingServiceHelper.getUsedSecretIds(helmConnector)).thenReturn(Collections.singleton(PASSWORD));
    settingsService.isFilteredSettingAttribute(null, null, ACCOUNT_ID, null, null, false, null, null, helmConnector);
    verify(secretManager, times(1))
        .canUseSecretsInAppAndEnv(anySetOf(String.class), eq(ACCOUNT_ID), any(), any(), eq(false), any(), any(), any());
    verify(usageRestrictionsService, times(3))
        .hasAccess(eq(ACCOUNT_ID), eq(false), any(), any(), any(), any(), any(), any(), anyBoolean());
  }

  @Test
  @Owner(developers = ARVIND)
  @Category(UnitTests.class)
  public void validateSettingAttributeTest() {
    SettingAttribute accountSetting =
        aSettingAttribute().withValue(GitConfig.builder().urlType(GitConfig.UrlType.ACCOUNT).build()).build();
    SettingAttribute repoSetting =
        aSettingAttribute().withValue(GitConfig.builder().urlType(GitConfig.UrlType.REPO).build()).build();

    SettingAttribute emptySetting = aSettingAttribute().withValue(GitConfig.builder().build()).build();

    assertThatExceptionOfType(InvalidRequestException.class)
        .isThrownBy(() -> settingsService.validateSettingAttribute(accountSetting, repoSetting))
        .withMessage("UrlType cannot be updated");
    assertThatExceptionOfType(InvalidRequestException.class)
        .isThrownBy(() -> settingsService.validateSettingAttribute(repoSetting, accountSetting))
        .withMessage("UrlType cannot be updated");
    settingsService.validateSettingAttribute(accountSetting, accountSetting);
    settingsService.validateSettingAttribute(repoSetting, repoSetting);
    settingsService.validateSettingAttribute(emptySetting, repoSetting);
    settingsService.validateSettingAttribute(emptySetting, accountSetting);
  }
}