package software.wings.helpers.ext.container;

import static io.harness.rule.OwnerRule.ADWAIT;
import static io.harness.rule.OwnerRule.ANSHUL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static software.wings.beans.KubernetesClusterAuthType.OIDC;

import com.google.inject.Inject;

import com.github.scribejava.apis.openid.OpenIdOAuth2AccessToken;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.oidc.model.OidcTokenRequestData;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.KubernetesClusterConfig;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.OidcGrantType;
import software.wings.delegatetasks.oidc.OidcTokenRetriever;

public class ContainerDeploymentDelegateHelperTest extends WingsBaseTest {
  @Mock private OidcTokenRetriever oidcTokenRetriever;
  @Inject @InjectMocks ContainerDeploymentDelegateHelper containerDeploymentDelegateHelper;

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
    doThrow(new InvalidRequestException("")).when(oidcTokenRetriever).getAccessToken(any());

    try {
      containerDeploymentDelegateHelper.retrieveOpenIdAccessToken(OidcTokenRequestData.builder().build());
    } catch (Exception e) {
      assertThat(e instanceof InvalidRequestException).isTrue();
    }

    try {
      containerDeploymentDelegateHelper.retrieveOpenIdAccessToken(OidcTokenRequestData.builder().build());
    } catch (Exception e) {
      assertThat(e instanceof InvalidRequestException).isTrue();
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
}
