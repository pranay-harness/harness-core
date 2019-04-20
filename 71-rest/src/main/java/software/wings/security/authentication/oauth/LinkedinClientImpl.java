package software.wings.security.authentication.oauth;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.github.scribejava.apis.LinkedInApi20;
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
import software.wings.app.MainConfiguration;
import software.wings.security.SecretManager;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.ExecutionException;

@Singleton
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LinkedinClientImpl extends BaseOauthClient implements OauthClient {
  OAuth20Service service;

  static final String PROTECTED_RESOURCE_URL = "https://api.linkedin.com/v1/people/~:(%s)";
  static final String EMAIL_FIELD_NAME = "email-address";
  static final String NAME_FIELD_NAME = "firstName";

  @Inject
  public LinkedinClientImpl(MainConfiguration mainConfiguration, SecretManager secretManager) {
    super(secretManager);
    LinkedinConfig linkedinConfig = mainConfiguration.getLinkedinConfig();
    service = new ServiceBuilder(linkedinConfig.getClientId())
                  .apiSecret(linkedinConfig.getClientSecret())
                  .callback(linkedinConfig.getCallbackUrl())
                  .scope("r_basicprofile r_emailaddress")
                  .build(LinkedInApi20.instance());
  }

  @Override
  public String getName() {
    return "linkedin";
  }

  @Override
  public URI getRedirectUrl() {
    URIBuilder uriBuilder = null;
    try {
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
    OauthUserInfo oauthUserInfo = parseResponseAndFetchUserInfo(accessToken);
    populateEmptyFields(oauthUserInfo);
    return oauthUserInfo;
  }

  private OAuth2AccessToken getAccessToken(final String code)
      throws InterruptedException, ExecutionException, IOException {
    return service.getAccessToken(code);
  }

  private OauthUserInfo parseResponseAndFetchUserInfo(OAuth2AccessToken accessToken)
      throws IOException, ExecutionException, InterruptedException {
    String email = getValueFromLinkedin(accessToken, EMAIL_FIELD_NAME, "emailAddress");
    String name = getValueFromLinkedin(accessToken, NAME_FIELD_NAME, "firstName");
    return OauthUserInfo.builder().email(email).name(name).build();
  }

  private String getValueFromLinkedin(OAuth2AccessToken accessToken, String emailFieldName, String jsonkey)
      throws InterruptedException, ExecutionException, IOException {
    final OAuthRequest request = new OAuthRequest(Verb.GET, String.format(PROTECTED_RESOURCE_URL, emailFieldName));
    request.addHeader("x-li-format", "json");
    request.addHeader("Accept-Language", "ru-RU");
    service.signRequest(accessToken, request);
    final Response response = service.execute(request);
    JSONObject jsonObject = new JSONObject(response.getBody());
    return jsonObject.getString(jsonkey);
  }
}
