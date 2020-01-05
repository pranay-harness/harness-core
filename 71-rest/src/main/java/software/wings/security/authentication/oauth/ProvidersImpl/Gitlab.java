package software.wings.security.authentication.oauth.ProvidersImpl;

import com.github.scribejava.apis.openid.OpenIdJsonTokenExtractor;
import com.github.scribejava.core.builder.api.DefaultApi20;
import com.github.scribejava.core.extractors.TokenExtractor;
import com.github.scribejava.core.model.OAuth2AccessToken;

public class Gitlab extends DefaultApi20 {
  protected Gitlab() {}

  public static Gitlab instance() {
    return Gitlab.InstanceHolder.INSTANCE;
  }

  @Override
  public String getAccessTokenEndpoint() {
    return "https://gitlab.com/oauth/token";
  }

  @Override
  public String getAuthorizationBaseUrl() {
    return "https://gitlab.com/oauth/authorize";
  }

  @Override
  public TokenExtractor<OAuth2AccessToken> getAccessTokenExtractor() {
    return OpenIdJsonTokenExtractor.instance();
  }

  @Override
  public String getRevokeTokenEndpoint() {
    return "https://accounts.google.com/o/oauth2/revoke";
  }

  private static class InstanceHolder {
    private static final Gitlab INSTANCE = new Gitlab();

    private InstanceHolder() {}
  }
}
