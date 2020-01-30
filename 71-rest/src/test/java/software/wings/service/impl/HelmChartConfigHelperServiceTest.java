package software.wings.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.context.ContextElementType;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.api.PhaseElement;
import software.wings.api.ServiceElement;
import software.wings.beans.HelmChartConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.appmanifest.StoreType;
import software.wings.beans.settings.helm.HttpHelmRepoConfig;
import software.wings.helpers.ext.helm.HelmConstants;
import software.wings.helpers.ext.helm.request.HelmChartConfigParams;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ExecutionContext;
import software.wings.utils.WingsTestConstants;

public class HelmChartConfigHelperServiceTest extends WingsBaseTest {
  @Mock private ServiceResourceService serviceResourceService;
  @Mock private SettingsService settingsService;
  @Mock private SecretManager secretManager;
  @Mock private ExecutionContext executionContext;

  private SettingAttribute helmConnector;

  @InjectMocks @Inject private HelmChartConfigHelperService helmChartConfigHelperService;

  @Before
  public void setUp() {
    when(executionContext.renderExpression(anyString()))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgumentAt(0, String.class));
    when(executionContext.getWorkflowExecutionId()).thenReturn(WingsTestConstants.WORKFLOW_EXECUTION_ID);
    when(executionContext.getContextElement(ContextElementType.PARAM, PhaseElement.PHASE_PARAM))
        .thenReturn(PhaseElement.builder()
                        .serviceElement(ServiceElement.builder().uuid(WingsTestConstants.SERVICE_ID).build())
                        .build());
    when(serviceResourceService.getHelmVersionWithDefault(APP_ID, WingsTestConstants.SERVICE_ID))
        .thenReturn(HelmConstants.HelmVersion.V3);

    helmConnector = new SettingAttribute();
    helmConnector.setName("helm-connector");
    helmConnector.setUuid("abc");
    helmConnector.setValue(HttpHelmRepoConfig.builder().build());

    when(settingsService.get(anyString())).thenReturn(helmConnector);
    when(settingsService.getByName(eq(ACCOUNT_ID), eq(APP_ID), anyString())).thenReturn(helmConnector);
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void shouldAlwaysSetHelmVersionInHelmChartConfigTaskParams() {
    ApplicationManifest applicationManifest = ApplicationManifest.builder()
                                                  .helmChartConfig(HelmChartConfig.builder()
                                                                       .chartUrl("abc")
                                                                       .chartName("test")
                                                                       .connectorId("abc")
                                                                       .chartVersion("0.1")
                                                                       .build())
                                                  .storeType(StoreType.HelmChartRepo)
                                                  .build();
    HelmChartConfigParams helmChartConfigTaskParams =
        helmChartConfigHelperService.getHelmChartConfigTaskParams(executionContext, applicationManifest);
    assertThat(helmChartConfigTaskParams.getHelmVersion()).isNull();
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void shouldAlwaysSetHelmVersionInHelmChartConfigTaskParamsIfNullConnectorId() {
    ApplicationManifest applicationManifest =
        ApplicationManifest.builder()
            .helmChartConfig(HelmChartConfig.builder().chartUrl("abc").chartName("test").chartVersion("0.1").build())
            .storeType(StoreType.HelmChartRepo)
            .build();
    HelmChartConfigParams helmChartConfigTaskParams =
        helmChartConfigHelperService.getHelmChartConfigTaskParams(executionContext, applicationManifest);
    assertThat(helmChartConfigTaskParams.getHelmVersion()).isNull();
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void shouldRenderChartConfigFields() {
    final String placeHolder = "${abc}";
    final String placeHolderValue = "foo";

    when(executionContext.renderExpression(placeHolder)).thenReturn(placeHolderValue);

    ApplicationManifest applicationManifest = ApplicationManifest.builder()
                                                  .helmChartConfig(HelmChartConfig.builder()
                                                                       .chartUrl(placeHolder)
                                                                       .chartName(placeHolder)
                                                                       .chartVersion(placeHolder)
                                                                       .build())
                                                  .storeType(StoreType.HelmSourceRepo)
                                                  .build();

    HelmChartConfigParams helmChartConfigTaskParams =
        helmChartConfigHelperService.getHelmChartConfigTaskParams(executionContext, applicationManifest);

    assertThat(helmChartConfigTaskParams.getChartUrl()).isEqualTo(placeHolderValue);
    assertThat(helmChartConfigTaskParams.getChartName()).isEqualTo(placeHolderValue);
    assertThat(helmChartConfigTaskParams.getChartVersion()).isEqualTo(placeHolderValue);
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void testGetHelmChartConfigFromYamlIfNullConfig() {
    assertThat(helmChartConfigHelperService.getHelmChartConfigFromYaml(ACCOUNT_ID, APP_ID, null)).isNull();
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void testGetHelmChartConfigForToYamlIfNullConfig() {
    assertThat(helmChartConfigHelperService.getHelmChartConfigForToYaml(null)).isNull();
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void testGetHelmChartConfigTaskParamsIfNullConfig() {
    assertThat(helmChartConfigHelperService.getHelmChartConfigTaskParams(
                   executionContext, ApplicationManifest.builder().storeType(StoreType.HelmSourceRepo).build()))
        .isNull();
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void testGetHelmChartConfigFromYaml() {
    HelmChartConfig helmChartConfig = HelmChartConfig.builder()
                                          .chartName("apache")
                                          .chartUrl("foo.com")
                                          .basePath("abc")
                                          .connectorName("git_helm")
                                          .chartVersion("42.42.42")
                                          .build();
    HelmChartConfig helmChartConfigFromYaml =
        helmChartConfigHelperService.getHelmChartConfigFromYaml(ACCOUNT_ID, APP_ID, helmChartConfig);

    assertThat(helmChartConfigFromYaml).isNotNull();
    assertThat(helmChartConfigFromYaml.getConnectorId()).isEqualTo(helmConnector.getUuid());
    assertThat(helmChartConfigFromYaml.getChartName()).isEqualTo(helmChartConfig.getChartName());
    assertThat(helmChartConfigFromYaml.getChartUrl()).isEqualTo(helmChartConfig.getChartUrl());
    assertThat(helmChartConfigFromYaml.getChartVersion()).isEqualTo(helmChartConfig.getChartVersion());
    assertThat(helmChartConfigFromYaml.getConnectorName()).isNull();
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void testGetHelmChartConfigForToYaml() {
    HelmChartConfig helmChartConfig = HelmChartConfig.builder()
                                          .chartName("apache")
                                          .chartUrl("foo.com")
                                          .basePath("abc")
                                          .connectorId(helmConnector.getUuid())
                                          .chartVersion("42.42.42")
                                          .build();

    HelmChartConfig helmChartConfigForToYaml =
        helmChartConfigHelperService.getHelmChartConfigForToYaml(helmChartConfig);

    assertThat(helmChartConfigForToYaml).isNotNull();
    assertThat(helmChartConfigForToYaml.getConnectorName()).isNotNull();
    assertThat(helmChartConfigForToYaml.getConnectorId()).isNull();
    assertThat(helmChartConfigForToYaml.getChartName()).isEqualTo(helmChartConfig.getChartName());
    assertThat(helmChartConfigForToYaml.getChartUrl()).isEqualTo(helmChartConfig.getChartUrl());
    assertThat(helmChartConfigForToYaml.getChartVersion()).isEqualTo(helmChartConfig.getChartVersion());
  }
}