package software.wings.helpers.ext.container;

import static io.harness.rule.OwnerRule.ACASIAN;
import static io.harness.rule.OwnerRule.ADWAIT;
import static io.harness.rule.OwnerRule.ANSHUL;
import static io.harness.rule.OwnerRule.YOGESH;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.KubernetesClusterAuthType.OIDC;

import com.google.common.collect.ImmutableMap;

import com.github.scribejava.apis.openid.OpenIdOAuth2AccessToken;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.extensions.DaemonSet;
import io.fabric8.kubernetes.api.model.extensions.Deployment;
import io.fabric8.kubernetes.client.VersionInfo;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.oidc.model.OidcTokenRequestData;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import software.wings.WingsBaseTest;
import software.wings.beans.KubernetesClusterConfig;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.Log.LogLevel;
import software.wings.beans.OidcGrantType;
import software.wings.beans.SettingAttribute;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.cloudprovider.gke.KubernetesContainerService;
import software.wings.delegatetasks.oidc.OidcTokenRetriever;
import software.wings.service.impl.ContainerServiceParams;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ContainerDeploymentDelegateHelperTest extends WingsBaseTest {
  @Mock private OidcTokenRetriever oidcTokenRetriever;
  @Mock private KubernetesContainerService kubernetesContainerService;
  @Mock ExecutionLogCallback logCallback;
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
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testRetrieveOpenIdAccessToken_ExceptionTest() throws Exception {
    doThrow(new InvalidRequestException("abc")).when(oidcTokenRetriever).getAccessToken(any());

    try {
      containerDeploymentDelegateHelper.retrieveOpenIdAccessToken(OidcTokenRequestData.builder().build());
    } catch (Exception e) {
      assertThat(e instanceof InvalidRequestException).isTrue();
      assertThat(e.getMessage()).isEqualTo("Failed to fetch OpenId Access Token. Invalid request: abc");
    }
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testGetOidcIdToken() throws Exception {
    OpenIdOAuth2AccessToken accessToken = mock(OpenIdOAuth2AccessToken.class);
    doReturn("oidcIdToken").when(accessToken).getOpenIdToken();
    doReturn(accessToken).when(oidcTokenRetriever).getAccessToken(any());

    KubernetesClusterConfig clusterConfig = KubernetesClusterConfig.builder()
                                                .authType(OIDC)
                                                .accountId("accId")
                                                .oidcClientId("clientId".toCharArray())
                                                .oidcGrantType(OidcGrantType.password)
                                                .oidcIdentityProviderUrl("url")
                                                .oidcUsername("user")
                                                .oidcPassword("pwd".toCharArray())
                                                .masterUrl("masterUrl")
                                                .oidcSecret("secret".toCharArray())
                                                .build();

    KubernetesConfig kubeConfig = clusterConfig.createKubernetesConfig("namespace");
    String oidcIdToken = containerDeploymentDelegateHelper.getOidcIdToken(kubeConfig);
    assertThat(oidcIdToken).isEqualTo("oidcIdToken");

    doReturn(null).when(oidcTokenRetriever).getAccessToken(any());
    oidcIdToken = containerDeploymentDelegateHelper.getOidcIdToken(kubeConfig);
    assertThat(oidcIdToken).isEqualTo(null);
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testGetContainerInfosWhenReadyByLabel() {
    ContainerServiceParams containerServiceParams = mock(ContainerServiceParams.class);
    KubernetesConfig kubernetesConfig = mock(KubernetesConfig.class);
    List<Pod> existingPods = asList(new Pod());

    when(kubernetesContainerService.getPods(eq(kubernetesConfig), any(List.class), anyMap())).thenReturn(existingPods);
    doReturn(null)
        .when(containerDeploymentDelegateHelper)
        .getContainerInfosWhenReadyByLabels(any(ContainerServiceParams.class), any(KubernetesConfig.class),
            any(ExecutionLogCallback.class), anyMap(), anyList());

    containerDeploymentDelegateHelper.getContainerInfosWhenReadyByLabel(
        "name", "value", containerServiceParams, kubernetesConfig, logCallback, existingPods);

    verify(containerDeploymentDelegateHelper, times(1))
        .getContainerInfosWhenReadyByLabels(
            containerServiceParams, kubernetesConfig, logCallback, ImmutableMap.of("name", "value"), existingPods);
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

    when(kubernetesContainerService.getControllers(any(KubernetesConfig.class), anyList(), anyMap()))
        .thenReturn(controllers);

    containerDeploymentDelegateHelper.getContainerInfosWhenReadyByLabels(
        containerServiceParams, kubernetesConfig, logCallback, ImmutableMap.of("name", "value"), existingPods);

    verify(kubernetesContainerService, times(1))
        .getContainerInfosWhenReady(kubernetesConfig, Collections.emptyList(), "deployment-name", 0, -1, 30,
            existingPods, false, logCallback, true, 0, "default");
    verify(kubernetesContainerService, times(1))
        .getContainerInfosWhenReady(kubernetesConfig, Collections.emptyList(), "daemonSet-name", 0, -1, 30,
            existingPods, true, logCallback, true, 0, "default");
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

    when(kubernetesContainerService.getPods(kubernetesConfig, Collections.emptyList(), labels))
        .thenReturn(asList(new Pod()));

    final List<Pod> pods =
        containerDeploymentDelegateHelper.getExistingPodsByLabels(containerServiceParams, kubernetesConfig, labels);
    assertThat(pods).hasSize(1);
    verify(kubernetesContainerService, times(1)).getPods(kubernetesConfig, Collections.emptyList(), labels);
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void k8sVersionIsGreaterOrEqualTo116() throws Exception {
    KubernetesConfig kubernetesConfig = KubernetesConfig.builder().namespace("default").build();
    ContainerServiceParams containerServiceParams =
        ContainerServiceParams.builder()
            .settingAttribute(SettingAttribute.Builder.aSettingAttribute().withValue(kubernetesConfig).build())
            .encryptionDetails(Collections.emptyList())
            .build();

    Map<String, String> jsonData = new HashMap<>();
    jsonData.put("major", "1");
    jsonData.put("minor", "16");
    jsonData.put("buildDate", "2020-06-06T10:54:00Z");
    VersionInfo version = new VersionInfo(jsonData);

    when(containerDeploymentDelegateHelper.getKubernetesConfig(containerServiceParams)).thenReturn(kubernetesConfig);
    when(kubernetesContainerService.getVersion(kubernetesConfig, new ArrayList<>())).thenReturn(version);

    boolean result =
        containerDeploymentDelegateHelper.isK8sVersion116OrAbove(containerServiceParams, new ExecutionLogCallback());
    assertThat(result).isTrue();
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void k8sVersionIsGreaterOrEqualTo116WithCharacter() throws Exception {
    KubernetesConfig kubernetesConfig = KubernetesConfig.builder().namespace("default").build();
    ContainerServiceParams containerServiceParams =
        ContainerServiceParams.builder()
            .settingAttribute(SettingAttribute.Builder.aSettingAttribute().withValue(kubernetesConfig).build())
            .encryptionDetails(Collections.emptyList())
            .build();

    Map<String, String> jsonData = new HashMap<>();
    jsonData.put("major", "1");
    jsonData.put("minor", "16+144");
    jsonData.put("buildDate", "2020-06-06T10:54:00Z");
    VersionInfo version = new VersionInfo(jsonData);

    when(containerDeploymentDelegateHelper.getKubernetesConfig(containerServiceParams)).thenReturn(kubernetesConfig);
    when(kubernetesContainerService.getVersion(kubernetesConfig, new ArrayList<>())).thenReturn(version);

    boolean result =
        containerDeploymentDelegateHelper.isK8sVersion116OrAbove(containerServiceParams, new ExecutionLogCallback());
    assertThat(result).isTrue();
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void k8sVersionIsLessThan116() throws Exception {
    KubernetesConfig kubernetesConfig = KubernetesConfig.builder().namespace("default").build();
    ContainerServiceParams containerServiceParams =
        ContainerServiceParams.builder()
            .settingAttribute(SettingAttribute.Builder.aSettingAttribute().withValue(kubernetesConfig).build())
            .encryptionDetails(Collections.emptyList())
            .build();

    Map<String, String> jsonData = new HashMap<>();
    jsonData.put("major", "1");
    jsonData.put("minor", "15");
    jsonData.put("buildDate", "2020-06-06T10:54:00Z");
    VersionInfo version = new VersionInfo(jsonData);

    when(containerDeploymentDelegateHelper.getKubernetesConfig(containerServiceParams)).thenReturn(kubernetesConfig);
    when(kubernetesContainerService.getVersion(kubernetesConfig, new ArrayList<>())).thenReturn(version);

    boolean result =
        containerDeploymentDelegateHelper.isK8sVersion116OrAbove(containerServiceParams, new ExecutionLogCallback());
    assertThat(result).isFalse();
  }
}
