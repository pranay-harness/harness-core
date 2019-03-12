package software.wings.security.authentication.oauth;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.github.scribejava.apis.GoogleApi20;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.apache.http.client.utils.URIBuilder;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.app.MainConfiguration;
import software.wings.security.SecretManager;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Singleton
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GoogleClientImpl extends BaseOauthClient implements OauthClient {
  OAuth20Service service;

  static final String PROTECTED_RESOURCE_URL = "https://www.googleapis.com/oauth2/v2/userinfo";
  static final Logger logger = LoggerFactory.getLogger(LinkedinClientImpl.class);

  @Inject
  public GoogleClientImpl(MainConfiguration mainConfiguration, SecretManager secretManager) {
    super(secretManager);
    GoogleConfig googleConfig = mainConfiguration.getGoogleConfig();
    service =
        new ServiceBuilder(googleConfig.getClientId())
            .apiSecret(googleConfig.getClientSecret())
            .callback(googleConfig.getCallbackUrl())
            .scope("https://www.googleapis.com/auth/userinfo.email https://www.googleapis.com/auth/userinfo.profile")
            .build(GoogleApi20.instance());
  }

  @Override
  public String getName() {
    return "google";
  }

  @Override
  public URI getRedirectUrl() {
    URIBuilder uriBuilder = null;
    try {
      final Map<String, String> additionalParams = new HashMap<>();
      additionalParams.put("access_type", "offline");
      // force to reget refresh token (if users are asked not the first time)
      additionalParams.put("prompt", "consent");
      uriBuilder = new URIBuilder(service.getAuthorizationUrl());
      appendStateToURL(uriBuilder);
      return uriBuilder.build();
    } catch (Exception e) {
      throw new WingsException(ErrorCode.UNKNOWN_ERROR, e);
    }
  }

  @Override
  public OauthUserInfo execute(final String code, final String state)
      throws InterruptedException, ExecutionException, IOException {
    verifyState(state);
    final OAuth2AccessToken accessToken = getAccessToken(code);
    Response response = getResponse(accessToken);
    OauthUserInfo oauthUserInfo = parseResponseAndFetchUserInfo(response);
    populateEmptyFields(oauthUserInfo);
    return oauthUserInfo;
  }

  private OauthUserInfo parseResponseAndFetchUserInfo(Response response) throws IOException {
    JSONObject jsonObject = new JSONObject(response.getBody());
    final String name = jsonObject.getString("name");
    final String email = jsonObject.getString("email");
    return OauthUserInfo.builder().name(name).email(email).build();
  }

  private Response getResponse(OAuth2AccessToken accessToken)
      throws InterruptedException, ExecutionException, IOException {
    final String requestUrl = PROTECTED_RESOURCE_URL;
    final OAuthRequest request = new OAuthRequest(Verb.GET, requestUrl);
    service.signRequest(accessToken, request);
    return service.execute(request);
  }

  private OAuth2AccessToken getAccessToken(final String code)
      throws InterruptedException, ExecutionException, IOException {
    logger.info("Access token received is: {}", code);
    return service.getAccessToken(code);
  }
}
