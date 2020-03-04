package software.wings.delegatetasks.oidc;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.delegatetasks.k8s.K8sConstants.OPEN_ID;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Singleton;

import com.github.scribejava.apis.openid.OpenIdOAuth2AccessToken;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.oauth.OAuth20Service;
import io.harness.oidc.model.OidcTokenRequestData;
import software.wings.beans.OidcGrantType;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Singleton
public class OidcTokenRetriever {
  private static final String SEPARATOR = "/";
  private static final String TOKEN = "token";
  private static final String AUTHORIZE = "authorize";
  public static final String SPACE = " ";

  String generateAccessTokenUrl(String providerUrl) {
    return providerUrl + SEPARATOR + TOKEN;
  }

  String generateAuthorizeUrl(String providerUrl) {
    return providerUrl + SEPARATOR + AUTHORIZE;
  }

  public OpenIdOAuth2AccessToken getAccessToken(OidcTokenRequestData oidcTokenRequestData)
      throws InterruptedException, ExecutionException, IOException {
    CustomIdentityProvider instance = CustomIdentityProvider.instance();
    instance.setAccessTokenEndpoint(generateAccessTokenUrl(oidcTokenRequestData.getProviderUrl()));
    instance.setAuthorizationBaseUrl(generateAuthorizeUrl(oidcTokenRequestData.getProviderUrl()));

    String scope = oidcTokenRequestData.getScope();
    scope = addNeededOidcScopesIfNotPresent(scope);
    OAuth20Service service = new ServiceBuilder(oidcTokenRequestData.getClientId())
                                 .apiSecret(oidcTokenRequestData.getClientSecret())
                                 .scope(scope)
                                 .build(instance);

    return (OpenIdOAuth2AccessToken) fetchAccessTokenUsingGrantType(service, oidcTokenRequestData);
  }

  @VisibleForTesting
  String addNeededOidcScopesIfNotPresent(String scope) {
    StringBuilder stringBuilder = new StringBuilder();
    // These 2 scopes are needed for access retrieval
    stringBuilder.append(OPEN_ID);

    if (isBlank(scope)) {
      return stringBuilder.toString();
    }

    String[] scopes = scope.split(SPACE);

    List<String> scopeList = Arrays.stream(scopes)
                                 .filter(individualScopeStr -> isOtherThanDefaultNeededScope(individualScopeStr))
                                 .collect(toList());

    if (isNotEmpty(scopeList)) {
      scopeList.forEach(individualScopeStr -> stringBuilder.append(SPACE).append(individualScopeStr));
    }

    return stringBuilder.toString();
  }

  @VisibleForTesting
  boolean isOtherThanDefaultNeededScope(String individualScopeStr) {
    if (isBlank(individualScopeStr)) {
      return false;
    }

    return !OPEN_ID.equals(individualScopeStr);
  }

  @VisibleForTesting
  OAuth2AccessToken fetchAccessTokenUsingGrantType(OAuth20Service service, OidcTokenRequestData oidcTokenRequestData)
      throws InterruptedException, ExecutionException, IOException {
    OAuth2AccessToken token = null;
    if (OidcGrantType.password.name().equals(oidcTokenRequestData.getGrantType())) {
      token =
          service.getAccessTokenPasswordGrant(oidcTokenRequestData.getUsername(), oidcTokenRequestData.getPassword());
    } else if (OidcGrantType.client_credentials.name().equals(oidcTokenRequestData.getGrantType())) {
      token = service.getAccessTokenClientCredentialsGrant();
    }

    return token;
  }
}
