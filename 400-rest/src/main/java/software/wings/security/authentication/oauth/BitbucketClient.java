/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.security.authentication.oauth;

import static io.harness.annotations.dev.HarnessModule._950_NG_AUTHENTICATION_SERVICE;
import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;

import software.wings.app.MainConfiguration;
import software.wings.security.SecretManager;
import software.wings.security.authentication.oauth.ProvidersImpl.Bitbucket;

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.ExecutionException;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.utils.URIBuilder;
import org.json.JSONArray;
import org.json.JSONObject;

@OwnedBy(PL)
@Singleton
@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
@TargetModule(_950_NG_AUTHENTICATION_SERVICE)
public class BitbucketClient extends BaseOauthClient implements OauthClient {
  OAuth20Service service;
  BitbucketConfig bitbucketConfig;

  static final String PROTECTED_RESOURCE_URL_NAME = "https://api.bitbucket.org/2.0/user";
  static final String PROTECTED_RESOURCE_URL_EMAIL = "https://api.bitbucket.org/2.0/user/emails";

  @Inject
  public BitbucketClient(MainConfiguration mainConfiguration, SecretManager secretManager) {
    super(secretManager);
    bitbucketConfig = mainConfiguration.getBitbucketConfig();
    if (bitbucketConfig != null) {
      service = new ServiceBuilder(bitbucketConfig.getClientId())
                    .apiSecret(bitbucketConfig.getClientSecret())
                    .build(Bitbucket.instance());
    }
  }

  @Override
  public String getName() {
    return "bitbucket";
  }

  @Override
  public URI getRedirectUrl() {
    URIBuilder uriBuilder = null;
    try {
      uriBuilder = new URIBuilder("https://bitbucket.org/site/oauth2/authorize");
      uriBuilder.addParameter("response_type", "code");
      uriBuilder.addParameter("client_id", bitbucketConfig.getClientId());
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
    OauthUserInfo oauthUserInfo = fetchUserInfo(accessToken);
    populateEmptyFields(oauthUserInfo);
    return oauthUserInfo;
  }

  private OauthUserInfo fetchUserInfo(OAuth2AccessToken accessToken)
      throws InterruptedException, ExecutionException, IOException {
    String email = getEmail(accessToken);
    String name = getName(accessToken);
    return OauthUserInfo.builder().email(email).name(name).build();
  }

  private String getName(OAuth2AccessToken accessToken) throws IOException, ExecutionException, InterruptedException {
    Response response = getResponse(accessToken, PROTECTED_RESOURCE_URL_NAME);
    return parseResponseAndFetchName(response);
  }

  private String parseResponseAndFetchName(Response response) throws IOException {
    JSONObject jsonObject = new JSONObject(response.getBody());
    return jsonObject.getString("display_name");
  }

  private String getEmail(OAuth2AccessToken accessToken) throws IOException, ExecutionException, InterruptedException {
    Response response = getResponse(accessToken, PROTECTED_RESOURCE_URL_EMAIL);
    return parseResponseAndFetchEmail(response);
  }

  private String parseResponseAndFetchEmail(Response response) throws IOException {
    JSONObject jsonObject = new JSONObject(response.getBody());
    String email = null;

    JSONArray values = (JSONArray) jsonObject.get("values");

    for (Object value : values) {
      if (((JSONObject) value).getBoolean("is_primary")) {
        email = ((JSONObject) value).getString("email");
      }
    }
    return email;
  }

  private Response getResponse(OAuth2AccessToken accessToken, String requestUrl)
      throws InterruptedException, ExecutionException, IOException {
    final OAuthRequest request = new OAuthRequest(Verb.GET, requestUrl);
    service.signRequest(accessToken, request);
    return service.execute(request);
  }

  private OAuth2AccessToken getAccessToken(final String code)
      throws InterruptedException, ExecutionException, IOException {
    return service.getAccessToken(code);
  }
}
