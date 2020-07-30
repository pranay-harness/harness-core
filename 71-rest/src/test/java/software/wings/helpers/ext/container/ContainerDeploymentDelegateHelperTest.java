package software.wings.helpers.ext.container;

import static io.harness.k8s.model.KubernetesClusterAuthType.OIDC;
import static io.harness.rule.OwnerRule.ACASIAN;
import static io.harness.rule.OwnerRule.ADWAIT;
import static io.harness.rule.OwnerRule.ANSHUL;
import static io.harness.rule.OwnerRule.YOGESH;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;

import com.github.scribejava.apis.openid.OpenIdOAuth2AccessToken;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.extensions.DaemonSet;
import io.fabric8.kubernetes.api.model.extensions.Deployment;
import io.fabric8.kubernetes.client.VersionInfo;
import io.harness.category.element.UnitTests;
import io.harness.k8s.KubernetesContainerService;
import io.harness.k8s.model.KubernetesConfig;
import io.harness.k8s.model.OidcGrantType;
import io.harness.k8s.oidc.OidcTokenRetriever;
import io.harness.logging.LogCallback;
import io.harness.logging.LogLevel;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import software.wings.WingsBaseTest;
import software.wings.beans.KubernetesClusterConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.helpers.ext.k8s.request.K8sClusterConfig;
import software.wings.service.impl.ContainerServiceParams;
import software.wings.service.intfc.security.EncryptionService;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ContainerDeploymentDelegateHelperTest extends WingsBaseTest {
  @Mock private KubernetesContainerService kubernetesContainerService;
  @Mock LogCallback logCallback;
  @Mock private EncryptionService encryptionService;
  @Spy @InjectMocks private OidcTokenRetriever oidcTokenRetriever;
  @Spy @InjectMocks ContainerDeploymentDelegateHelper containerDeploymentDelegateHelper;

  @Before
  public void setup() {
    doNothing().when(logCallback).saveExecutionLog(anyString());
    doNothing().when(logCallback).saveExecutionLog(anyString(), any(LogLevel.class));
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testGetConfigFileContent() throws Exception {
    String expected = "apiVersion: v1\n"
        + "clusters:\n"
        + "- cluster:\n"
        + "    xxxxxxxx masterUrl\n"
        + "    insecure-skip-tls-verify: true\n"
        + "  name: CLUSTER_NAME\n"
        + "contexts:\n"
        + "- context:\n"
        + "    cluster: CLUSTER_NAME\n"
        + "    user: HARNESS_USER\n"
        + "    namespace: namespace\n"
        + "  name: CURRENT_CONTEXT\n"
        + "current-context: CURRENT_CONTEXT\n"
        + "kind: Config\n"
        + "preferences: {}\n"
        + "users:\n"
        + "- name: HARNESS_USER\n"
        + "  user:\n"
        + "    auth-provider:\n"
        + "      config:\n"
        + "        client-id: clientId\n"
        + "        client-secret: secret\n"
        + "        id-token: id_token\n"
        + "        refresh-token: refresh_token\n"
        + "        idp-issuer-url: url\n"
        + "      name: oidc\n";

    OpenIdOAuth2AccessToken accessToken = mock(OpenIdOAuth2AccessToken.class);
    doReturn("id_token").when(accessToken).getOpenIdToken();
    doReturn(3600).when(accessToken).getExpiresIn();
    doReturn("bearer").when(accessToken).getTokenType();
    doReturn("refresh_token").when(accessToken).getRefreshToken();

    doReturn(accessToken).when(oidcTokenRetriever).getAccessToken(any());

    KubernetesClusterConfig clusterConfig = KubernetesClusterConfig.builder()
                                                .accountId("accId")
                                                .authType(OIDC)
                                                .oidcClientId("clientId".toCharArray())
                                                .oidcGrantType(OidcGrantType.password)
                                                .oidcIdentityProviderUrl("url")
                                                .oidcPassword("pwd".toCharArray())
                                                .oidcUsername("user")
                                                .oidcSecret("secret".toCharArray())
                                                .masterUrl("masterUrl")
                                                .build();

    // Test generating KubernetesConfig from KubernetesClusterConfig
    KubernetesConfig kubeConfig = clusterConfig.createKubernetesConfig("namespace");
    String configFileContent = containerDeploymentDelegateHelper.getConfigFileContent(kubeConfig);
    assertThat(expected).isEqualTo(configFileContent);
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testGetContainerInfosWhenReadyByLabel() {
    ContainerServiceParams containerServiceParams = mock(ContainerServiceParams.class);
    KubernetesConfig kubernetesConfig = mock(KubernetesConfig.class);
    List<Pod> existingPods = asList(new Pod());

    when(kubernetesContainerService.getPods(eq(kubernetesConfig), anyMap())).thenReturn(existingPods);
    doReturn(null)
        .when(containerDeploymentDelegateHelper)
        .getContainerInfosWhenReadyByLabels(any(KubernetesConfig.class), any(LogCallback.class), anyMap(), anyList());

    containerDeploymentDelegateHelper.getContainerInfosWhenReadyByLabel(
        "name", "value", kubernetesConfig, logCallback, existingPods);

    verify(containerDeploymentDelegateHelper, times(1))
        .getContainerInfosWhenReadyByLabels(
            kubernetesConfig, logCallback, ImmutableMap.of("name", "value"), existingPods);
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testGetContainerInfosWhenReadyByLabels() {
    ContainerServiceParams containerServiceParams =
        ContainerServiceParams.builder().encryptionDetails(Collections.emptyList()).build();
    KubernetesConfig kubernetesConfig = KubernetesConfig.builder().namespace("default").build();
    List<Pod> existingPods = asList(new Pod());
    List<? extends HasMetadata> controllers = getMockedControllers();

    when(kubernetesContainerService.getControllers(any(KubernetesConfig.class), anyMap())).thenReturn(controllers);

    containerDeploymentDelegateHelper.getContainerInfosWhenReadyByLabels(
        kubernetesConfig, logCallback, ImmutableMap.of("name", "value"), existingPods);

    verify(kubernetesContainerService, times(1))
        .getContainerInfosWhenReady(
            kubernetesConfig, "deployment-name", 0, -1, 30, existingPods, false, logCallback, true, 0, "default");
    verify(kubernetesContainerService, times(1))
        .getContainerInfosWhenReady(
            kubernetesConfig, "daemonSet-name", 0, -1, 30, existingPods, true, logCallback, true, 0, "default");
  }

  private List<? extends HasMetadata> getMockedControllers() {
    HasMetadata controller_1 = mock(Deployment.class);
    HasMetadata controller_2 = mock(DaemonSet.class);
    ObjectMeta metaData_1 = mock(ObjectMeta.class);
    ObjectMeta metaData_2 = mock(ObjectMeta.class);
    when(controller_1.getKind()).thenReturn("Deployment");
    when(controller_2.getKind()).thenReturn("DaemonSet");
    when(controller_1.getMetadata()).thenReturn(metaData_1);
    when(controller_2.getMetadata()).thenReturn(metaData_2);
    when(metaData_1.getName()).thenReturn("deployment-name");
    when(metaData_2.getName()).thenReturn("daemonSet-name");
    return asList(controller_1, controller_2);
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void getExistingPodsByLabels() {
    ContainerServiceParams containerServiceParams =
        ContainerServiceParams.builder().encryptionDetails(Collections.emptyList()).build();
    KubernetesConfig kubernetesConfig = KubernetesConfig.builder().namespace("default").build();
    Map<String, String> labels = new HashMap<>();

    when(kubernetesContainerService.getPods(kubernetesConfig, labels)).thenReturn(asList(new Pod()));

    final List<Pod> pods =
        containerDeploymentDelegateHelper.getExistingPodsByLabels(containerServiceParams, kubernetesConfig, labels);
    assertThat(pods).hasSize(1);
    verify(kubernetesContainerService, times(1)).getPods(kubernetesConfig, labels);
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void k8sVersionIsGreaterOrEqualTo116() throws Exception {
    KubernetesClusterConfig kubernetesClusterConfig = KubernetesClusterConfig.builder().build();
    KubernetesConfig kubernetesConfig = KubernetesConfig.builder().namespace("default").build();
    ContainerServiceParams containerServiceParams =
        ContainerServiceParams.builder()
            .settingAttribute(SettingAttribute.Builder.aSettingAttribute().withValue(kubernetesClusterConfig).build())
            .encryptionDetails(Collections.emptyList())
            .build();

    Map<String, String> jsonData = new HashMap<>();
    jsonData.put("major", "1");
    jsonData.put("minor", "16");
    jsonData.put("buildDate", "2020-06-06T10:54:00Z");
    VersionInfo version = new VersionInfo(jsonData);

    doReturn(kubernetesConfig).when(containerDeploymentDelegateHelper).getKubernetesConfig(containerServiceParams);
    doReturn(version).when(kubernetesContainerService).getVersion(kubernetesConfig);

    boolean result = containerDeploymentDelegateHelper.useK8sSteadyStateCheck(
        true, containerServiceParams, new ExecutionLogCallback());
    assertThat(result).isTrue();
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void k8sVersionIsGreaterOrEqualTo116WithCharacter() throws Exception {
    KubernetesConfig kubernetesConfig = KubernetesConfig.builder().namespace("default").build();
    ContainerServiceParams containerServiceParams =
        ContainerServiceParams.builder()
            .settingAttribute(SettingAttribute.Builder.aSettingAttribute()
                                  .withValue(KubernetesClusterConfig.builder().build())
                                  .build())
            .encryptionDetails(Collections.emptyList())
            .build();

    Map<String, String> jsonData = new HashMap<>();
    jsonData.put("major", "1");
    jsonData.put("minor", "16+144");
    jsonData.put("buildDate", "2020-06-06T10:54:00Z");
    VersionInfo version = new VersionInfo(jsonData);

    doReturn(kubernetesConfig).when(containerDeploymentDelegateHelper).getKubernetesConfig(containerServiceParams);
    doReturn(version).when(kubernetesContainerService).getVersion(kubernetesConfig);

    boolean result = containerDeploymentDelegateHelper.useK8sSteadyStateCheck(
        true, containerServiceParams, new ExecutionLogCallback());
    assertThat(result).isTrue();
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void k8sVersionIsLessThan116() throws Exception {
    KubernetesConfig kubernetesConfig = KubernetesConfig.builder().namespace("default").build();
    ContainerServiceParams containerServiceParams =
        ContainerServiceParams.builder()
            .settingAttribute(SettingAttribute.Builder.aSettingAttribute()
                                  .withValue(KubernetesClusterConfig.builder().build())
                                  .build())
            .encryptionDetails(Collections.emptyList())
            .build();

    Map<String, String> jsonData = new HashMap<>();
    jsonData.put("major", "1");
    jsonData.put("minor", "15");
    jsonData.put("buildDate", "2020-06-06T10:54:00Z");
    VersionInfo version = new VersionInfo(jsonData);

    doReturn(kubernetesConfig).when(containerDeploymentDelegateHelper).getKubernetesConfig(containerServiceParams);
    doReturn(version).when(kubernetesContainerService).getVersion(kubernetesConfig);

    boolean result = containerDeploymentDelegateHelper.useK8sSteadyStateCheck(
        true, containerServiceParams, new ExecutionLogCallback());
    assertThat(result).isFalse();
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testIsK8sVersion116OrAboveWithFeatureFlagDisabled() throws Exception {
    boolean result = containerDeploymentDelegateHelper.useK8sSteadyStateCheck(
        false, ContainerServiceParams.builder().build(), new ExecutionLogCallback());
    assertThat(result).isFalse();
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testGetControllerCountByLabels() {
    KubernetesConfig kubernetesConfig = KubernetesConfig.builder().namespace("default").build();
    Map<String, String> labels = new HashMap<>();

    List<? extends HasMetadata> controllers = getMockedControllers();
    when(kubernetesContainerService.getControllers(any(KubernetesConfig.class), anyMap())).thenReturn(controllers);
    assertThat(containerDeploymentDelegateHelper.getControllerCountByLabels(kubernetesConfig, labels)).isEqualTo(2);
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testGetKubernetesConfig() {
    K8sClusterConfig k8sClusterConfig =
        K8sClusterConfig.builder()
            .cloudProvider(KubernetesClusterConfig.builder().masterUrl("https://example.com").build())
            .cloudProviderEncryptionDetails(emptyList())
            .build();

    KubernetesConfig kubernetesConfig = containerDeploymentDelegateHelper.getKubernetesConfig(k8sClusterConfig);
    assertThat(kubernetesConfig.getMasterUrl()).isEqualTo("https://example.com");
  }
}
