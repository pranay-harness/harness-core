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
import software.wings.security.authentication.oauth.ProvidersImpl.Gitlab;

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;
import com.google.inject.Inject;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.ExecutionException;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.utils.URIBuilder;
import org.json.JSONException;
import org.json.JSONObject;

@OwnedBy(PL)
@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
@TargetModule(_950_NG_AUTHENTICATION_SERVICE)
public class GitlabClient extends BaseOauthClient implements OauthClient {
  OAuth20Service service;

  static String EMAIL_FIELD_NAME = "email";
  static String NAME_FIELD_NAME = "name";
  static String LOGIN_FIELD_NAME = "login";
  static String PROTECTED_RESOURCE_URL = "https://gitlab.com/api/v4/user";

  @Inject
  public GitlabClient(MainConfiguration mainConfiguration, SecretManager secretManager) {
    super(secretManager);
    GitlabConfig gitlabConfig = mainConfiguration.getGitlabConfig();
    if (gitlabConfig != null) {
      service = new ServiceBuilder(gitlabConfig.getClientId())
                    .apiSecret(gitlabConfig.getClientSecret())
                    .callback(gitlabConfig.getCallbackUrl())
                    .scope("read_user")
                    .build(Gitlab.instance());
    }
  }

  @Override
  public String getName() {
    return "gitlab";
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
    populateEmptyFields(oauthUserInfo);
    return oauthUserInfo;
  }

  private OAuth2AccessToken getAccessToken(final String code)
      throws InterruptedException, ExecutionException, IOException {
    log.info("Access token received is: {}", code);
    return service.getAccessToken(code);
  }

  private OauthUserInfo parseResponseAndFetchUserInfo(com.github.scribejava.core.model.Response userResource)
      throws IOException {
    String email = null;
    String name = null;
    JSONObject jsonObject = new JSONObject(userResource.getBody());
    try {
      name = jsonObject.getString(NAME_FIELD_NAME);
      email = jsonObject.getString(EMAIL_FIELD_NAME);
      return OauthUserInfo.builder().email(email).name(name).build();
    } catch (JSONException je) {
      log.error("Unable to parse json in github oauth", je);
    }
    return OauthUserInfo.builder().email(email).name(name).build();
  }

  private com.github.scribejava.core.model.Response getUserResource(final OAuth2AccessToken accessToken)
      throws IOException, ExecutionException, InterruptedException {
    final OAuthRequest request = new OAuthRequest(Verb.GET, PROTECTED_RESOURCE_URL);
    service.signRequest(accessToken, request);
    return service.execute(request);
  }
}
