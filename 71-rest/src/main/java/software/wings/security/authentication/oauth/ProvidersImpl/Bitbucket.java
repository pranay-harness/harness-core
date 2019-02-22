package software.wings.security.authentication.oauth.ProvidersImpl;

import com.github.scribejava.apis.openid.OpenIdJsonTokenExtractor;
import com.github.scribejava.core.builder.api.DefaultApi20;
import com.github.scribejava.core.extractors.TokenExtractor;
import com.github.scribejava.core.model.OAuth2AccessToken;

public class Bitbucket extends DefaultApi20 {
  protected Bitbucket() {}

  public static Bitbucket instance() {
    return Bitbucket.InstanceHolder.INSTANCE;
  }

  public String getAccessTokenEndpoint() {
    return "https://bitbucket.org/site/oauth2/access_token";
  }

  public String getAuthorizationBaseUrl() {
    return "https://bitbucket.org/site/oauth2/authorize";
  }

  public TokenExtractor<OAuth2AccessToken> getAccessTokenExtractor() {
    return OpenIdJsonTokenExtractor.instance();
  }

  public String getRevokeTokenEndpoint() {
    return "https://accounts.google.com/o/oauth2/revoke";
  }

  private static class InstanceHolder {
    private static final Bitbucket INSTANCE = new Bitbucket();

    private InstanceHolder() {}
  }
}
