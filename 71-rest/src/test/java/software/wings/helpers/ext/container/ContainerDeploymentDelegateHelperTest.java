package software.wings.helpers.ext.container;

import static io.harness.k8s.model.KubernetesClusterAuthType.OIDC;
import static io.harness.rule.OwnerRule.ACASIAN;
import static io.harness.rule.OwnerRule.ADWAIT;
import static io.harness.rule.OwnerRule.ANSHUL;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.github.scribejava.apis.openid.OpenIdOAuth2AccessToken;
import io.fabric8.kubernetes.client.VersionInfo;
import io.harness.category.element.UnitTests;
import io.harness.delegate.task.k8s.ContainerDeploymentDelegateBaseHelper;
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
import java.util.Map;

public class ContainerDeploymentDelegateHelperTest extends WingsBaseTest {
  @Mock private KubernetesContainerService kubernetesContainerService;
  @Mock LogCallback logCallback;
  @Mock private EncryptionService encryptionService;
  @Spy @InjectMocks private OidcTokenRetriever oidcTokenRetriever;
  @Spy @InjectMocks ContainerDeploymentDelegateBaseHelper containerDeploymentDelegateBaseHelper;
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
    String configFileContent = containerDeploymentDelegateBaseHelper.getConfigFileContent(kubeConfig);
    assertThat(expected).isEqualTo(configFileContent);
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
