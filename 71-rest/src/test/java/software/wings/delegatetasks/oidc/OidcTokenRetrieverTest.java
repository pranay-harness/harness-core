package software.wings.delegatetasks.oidc;

import static io.harness.rule.OwnerRule.ADWAIT;
import static io.harness.rule.OwnerRule.ANSHUL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static software.wings.delegatetasks.k8s.K8sConstants.OPEN_ID;

import com.google.inject.Inject;

import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.oauth.OAuth20Service;
import io.harness.category.element.UnitTests;
import io.harness.oidc.model.OidcTokenRequestData;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.Spy;
import software.wings.WingsBaseTest;
import software.wings.beans.OidcGrantType;

public class OidcTokenRetrieverTest extends WingsBaseTest {
  @Inject OidcTokenRetriever oidcTokenRetriever;

  @Spy OidcTokenRetriever spyOidcTokenRetriever;

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testAddNeededOidcScopesIfNotPresent() {
    String scope = oidcTokenRetriever.addNeededOidcScopesIfNotPresent(null);
    assertThat(scope).isEqualTo(OPEN_ID);

    scope = oidcTokenRetriever.addNeededOidcScopesIfNotPresent("");
    assertThat(scope).isEqualTo(OPEN_ID);

    scope = oidcTokenRetriever.addNeededOidcScopesIfNotPresent("     ");
    assertThat(scope).isEqualTo(OPEN_ID);

    scope = oidcTokenRetriever.addNeededOidcScopesIfNotPresent(OPEN_ID);
    assertThat(scope).isEqualTo(OPEN_ID);

    scope = oidcTokenRetriever.addNeededOidcScopesIfNotPresent("A B");
    assertThat(scope).isEqualTo(OPEN_ID + " A B");

    scope = oidcTokenRetriever.addNeededOidcScopesIfNotPresent(OPEN_ID + "   "
        + "A      B");
    assertThat(scope).isEqualTo(OPEN_ID + " A B");
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testFetchAccessTokenUsingGrantType() throws Exception {
    OAuth20Service service = mock(OAuth20Service.class);
    OAuth2AccessToken token = mock(OAuth2AccessToken.class);

    doReturn(token).when(service).getAccessTokenPasswordGrant(anyString(), anyString());

    oidcTokenRetriever.fetchAccessTokenUsingGrantType(
        service, OidcTokenRequestData.builder().grantType(OidcGrantType.password.name()).build());
    verify(service, times(1)).getAccessTokenPasswordGrant(anyString(), anyString());
    verify(service, never()).getAccessTokenClientCredentialsGrant();
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testGetAccessToken() throws Exception {
    doReturn(null).when(spyOidcTokenRetriever).fetchAccessTokenUsingGrantType(any(), any());

    spyOidcTokenRetriever.getAccessToken(OidcTokenRequestData.builder()
                                             .clientId("clientId")
                                             .clientSecret("clientSecret")
                                             .scope("email")
                                             .grantType(OidcGrantType.password.name())
                                             .username("username")
                                             .password("password")
                                             .build());

    ArgumentCaptor<OidcTokenRequestData> captor = ArgumentCaptor.forClass(OidcTokenRequestData.class);
    verify(spyOidcTokenRetriever, times(1)).fetchAccessTokenUsingGrantType(any(), captor.capture());
    OidcTokenRequestData tokenRequestData = captor.getValue();

    assertThat(tokenRequestData.getClientId()).isEqualTo("clientId");
    assertThat(tokenRequestData.getUsername()).isEqualTo("username");
    assertThat(tokenRequestData.getPassword()).isEqualTo("password");
    assertThat(tokenRequestData.getClientSecret()).isEqualTo("clientSecret");
    assertThat(tokenRequestData.getScope()).isEqualTo("email");
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testFetchAccessTokenUsingGrantTypeForClientCredentials() throws Exception {
    OAuth20Service service = mock(OAuth20Service.class);
    OAuth2AccessToken token = mock(OAuth2AccessToken.class);

    doReturn(token).when(service).getAccessTokenClientCredentialsGrant();

    oidcTokenRetriever.fetchAccessTokenUsingGrantType(
        service, OidcTokenRequestData.builder().grantType(OidcGrantType.client_credentials.name()).build());
    verify(service, times(1)).getAccessTokenClientCredentialsGrant();
    verify(service, never()).getAccessTokenPasswordGrant(any(), any());
  }
}
