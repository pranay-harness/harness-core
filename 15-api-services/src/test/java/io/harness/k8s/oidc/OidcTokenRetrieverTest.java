package io.harness.k8s.oidc;

import static io.harness.k8s.K8sConstants.OPEN_ID;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.oauth.OAuth20Service;
import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.k8s.model.OidcGrantType;
import io.harness.oidc.model.OidcTokenRequestData;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class OidcTokenRetrieverTest extends CategoryTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @InjectMocks OidcTokenRetriever oidcTokenRetriever;

  @Spy OidcTokenRetriever spyOidcTokenRetriever;

  @Test
  @Owner(developers = OwnerRule.ADWAIT)
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
  @Owner(developers = OwnerRule.ADWAIT)
  @Category(UnitTests.class)
  public void testFetchAccessTokenUsingGrantType() throws Exception {
    OAuth20Service service = Mockito.mock(OAuth20Service.class);
    OAuth2AccessToken token = Mockito.mock(OAuth2AccessToken.class);

    Mockito.doReturn(token).when(service).getAccessTokenPasswordGrant(Matchers.anyString(), Matchers.anyString());

    oidcTokenRetriever.fetchAccessTokenUsingGrantType(
        service, OidcTokenRequestData.builder().grantType(OidcGrantType.password.name()).build());
    Mockito.verify(service, Mockito.times(1)).getAccessTokenPasswordGrant(Matchers.anyString(), Matchers.anyString());
    Mockito.verify(service, Mockito.never()).getAccessTokenClientCredentialsGrant();
  }

  @Test
  @Owner(developers = OwnerRule.ANSHUL)
  @Category(UnitTests.class)
  public void testGetAccessToken() throws Exception {
    Mockito.doReturn(null).when(spyOidcTokenRetriever).fetchAccessTokenUsingGrantType(Matchers.any(), Matchers.any());

    spyOidcTokenRetriever.getAccessToken(OidcTokenRequestData.builder()
                                             .clientId("clientId")
                                             .clientSecret("clientSecret")
                                             .scope("email")
                                             .grantType(OidcGrantType.password.name())
                                             .username("username")
                                             .password("password")
                                             .build());

    ArgumentCaptor<OidcTokenRequestData> captor = ArgumentCaptor.forClass(OidcTokenRequestData.class);
    Mockito.verify(spyOidcTokenRetriever, Mockito.times(1))
        .fetchAccessTokenUsingGrantType(Matchers.any(), captor.capture());
    OidcTokenRequestData tokenRequestData = captor.getValue();

    assertThat(tokenRequestData.getClientId()).isEqualTo("clientId");
    assertThat(tokenRequestData.getUsername()).isEqualTo("username");
    assertThat(tokenRequestData.getPassword()).isEqualTo("password");
    assertThat(tokenRequestData.getClientSecret()).isEqualTo("clientSecret");
    assertThat(tokenRequestData.getScope()).isEqualTo("email");
  }

  @Test
  @Owner(developers = OwnerRule.ADWAIT)
  @Category(UnitTests.class)
  public void testFetchAccessTokenUsingGrantTypeForClientCredentials() throws Exception {
    OAuth20Service service = Mockito.mock(OAuth20Service.class);
    OAuth2AccessToken token = Mockito.mock(OAuth2AccessToken.class);

    Mockito.doReturn(token).when(service).getAccessTokenClientCredentialsGrant();

    oidcTokenRetriever.fetchAccessTokenUsingGrantType(
        service, OidcTokenRequestData.builder().grantType(OidcGrantType.client_credentials.name()).build());
    Mockito.verify(service, Mockito.times(1)).getAccessTokenClientCredentialsGrant();
    Mockito.verify(service, Mockito.never()).getAccessTokenPasswordGrant(Matchers.any(), Matchers.any());
  }
}
