package software.wings.service.impl;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Matchers.any;
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
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;

import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import software.wings.WingsBaseTest;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingCategory;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.appmanifest.StoreType;
import software.wings.beans.settings.helm.GCSHelmRepoConfig;
import software.wings.beans.settings.helm.HttpHelmRepoConfig;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ApplicationManifestService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.UsageRestrictionsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.yaml.YamlHelper;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SettingsServiceImplTest extends WingsBaseTest {
  private static final String PASSWORD = "PASSWORD";

  @Mock private ApplicationManifestService applicationManifestService;
  @Mock private EnvironmentService environmentService;
  @Mock private AppService appService;
  @Mock private ServiceResourceService serviceResourceService;
  @Mock private SecretManager secretManager;
  @Mock private UsageRestrictionsService usageRestrictionsService;
  @Mock private SettingServiceHelper settingServiceHelper;

  @Spy @InjectMocks private SettingsServiceImpl settingsService;

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void testEnsureHelmConnectorSafeToDelete() {
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
        .hasAccess(eq(ACCOUNT_ID), eq(false), any(), any(), any(), any(), any(), any());

    when(settingServiceHelper.hasReferencedSecrets(eq(helmConnector))).thenReturn(true);
    settingsService.isFilteredSettingAttribute(null, null, ACCOUNT_ID, null, null, false, null, null, helmConnector);
    verify(secretManager, never())
        .canUseSecretsInAppAndEnv(anySetOf(String.class), eq(ACCOUNT_ID), any(), any(), eq(false), any(), any(), any());
    verify(usageRestrictionsService, times(2))
        .hasAccess(eq(ACCOUNT_ID), eq(false), any(), any(), any(), any(), any(), any());

    helmConnector.setValue(HttpHelmRepoConfig.builder()
                               .chartRepoUrl("http://stable-charts")
                               .encryptedPassword(YamlHelper.ENCRYPTED_VALUE_STR)
                               .accountId(ACCOUNT_ID)
                               .build());
    settingsService.isFilteredSettingAttribute(null, null, ACCOUNT_ID, null, null, false, null, null, helmConnector);
    verify(secretManager, never())
        .canUseSecretsInAppAndEnv(anySetOf(String.class), eq(ACCOUNT_ID), any(), any(), eq(false), any(), any(), any());
    verify(usageRestrictionsService, times(3))
        .hasAccess(eq(ACCOUNT_ID), eq(false), any(), any(), any(), any(), any(), any());

    helmConnector.setValue(HttpHelmRepoConfig.builder()
                               .chartRepoUrl("http://stable-charts")
                               .encryptedPassword(PASSWORD)
                               .accountId(ACCOUNT_ID)
                               .build());
    settingsService.isFilteredSettingAttribute(null, null, ACCOUNT_ID, null, null, false, null, null, helmConnector);
    verify(secretManager, times(1))
        .canUseSecretsInAppAndEnv(anySetOf(String.class), eq(ACCOUNT_ID), any(), any(), eq(false), any(), any(), any());
    verify(usageRestrictionsService, times(3))
        .hasAccess(eq(ACCOUNT_ID), eq(false), any(), any(), any(), any(), any(), any());
  }
}