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

import com.github.scribejava.apis.GitHubApi;
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
import org.json.JSONException;
import org.json.JSONObject;

@OwnedBy(PL)
@Singleton
@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
@TargetModule(_950_NG_AUTHENTICATION_SERVICE)
public class GithubClientImpl extends BaseOauthClient implements OauthClient {
  OAuth20Service service;

  static final String EMAIL_FIELD_NAME = "email";
  static final String NAME_FIELD_NAME = "name";
  static final String LOGIN_FIELD_NAME = "login";
  static final String PROTECTED_RESOURCE_URL = "https://api.github.com/user";
  static final String FALLABCK_PROTECTED_RESOURCE_URL = "https://api.github.com/user/emails";

  @Inject
  public GithubClientImpl(MainConfiguration mainConfiguration, SecretManager secretManager) {
    super(secretManager);
    GithubConfig githubConfig = mainConfiguration.getGithubConfig();
    if (githubConfig != null) {
      service = new ServiceBuilder(githubConfig.getClientId())
                    .apiSecret(githubConfig.getClientSecret())
                    .scope("user:email") // replace with desired scope
                    .callback(githubConfig.getCallbackUrl())
                    .build(GitHubApi.instance());
    }
  }

  @Override
  public String getName() {
    return "github";
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
    com.github.scribejava.core.model.Response userResource = getUserResource(accessToken);
    OauthUserInfo oauthUserInfo = parseResponseAndFetchUserInfo(userResource);
    if (null == oauthUserInfo.email) {
      oauthUserInfo.setEmail(fallbackCall(accessToken));
    }
    populateEmptyFields(oauthUserInfo);
    return oauthUserInfo;
  }

  private String fallbackCall(final OAuth2AccessToken accessToken)
      throws InterruptedException, ExecutionException, IOException {
    final OAuthRequest request = new OAuthRequest(Verb.GET, FALLABCK_PROTECTED_RESOURCE_URL);
    service.signRequest(accessToken, request);
    com.github.scribejava.core.model.Response response = service.execute(request);
    return emailFromResponse(response);
  }

  private String emailFromResponse(final Response response) throws IOException {
    JSONArray jsonarray = new JSONArray(response.getBody());
    for (Object jsonObject : jsonarray) {
      if (((JSONObject) jsonObject).getBoolean("primary")) {
        return ((JSONObject) jsonObject).getString(EMAIL_FIELD_NAME);
      }
    }
    return null;
  }

  private OAuth2AccessToken getAccessToken(final String code)
      throws InterruptedException, ExecutionException, IOException {
    return service.getAccessToken(code);
  }

  private OauthUserInfo parseResponseAndFetchUserInfo(com.github.scribejava.core.model.Response userResource)
      throws IOException {
    String loginId = null;
    String email = null;
    String name = null;
    JSONObject jsonObject = new JSONObject(userResource.getBody());
    try {
      loginId = jsonObject.getString(LOGIN_FIELD_NAME);
      name = jsonObject.getString(NAME_FIELD_NAME);
      email = jsonObject.getString(EMAIL_FIELD_NAME);
    } catch (JSONException je) {
      log.info("Unable to parse json in github oauth", je);
    }
    return OauthUserInfo.builder().email(email).name(name).login(loginId).build();
  }

  private com.github.scribejava.core.model.Response getUserResource(final OAuth2AccessToken accessToken)
      throws IOException, ExecutionException, InterruptedException {
    final OAuthRequest request = new OAuthRequest(Verb.GET, PROTECTED_RESOURCE_URL);
    service.signRequest(accessToken, request);
    return service.execute(request);
  }
}
