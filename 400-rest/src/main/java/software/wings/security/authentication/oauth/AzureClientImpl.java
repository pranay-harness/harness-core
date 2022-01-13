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

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.builder.api.DefaultApi20;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.oauth.OAuth20Service;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Request.Builder;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.client.utils.URIBuilder;
import org.json.JSONException;
import org.json.JSONObject;

@Singleton
@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
@OwnedBy(PL)
@TargetModule(_950_NG_AUTHENTICATION_SERVICE)
public class AzureClientImpl extends BaseOauthClient implements OauthClient {
  OAuth20Service service;
  static String EMAIL_FIELD_NAME = "userPrincipalName";
  static String NAME_FIELD_NAME = "givenName";

  static final String PROTECTED_RESOURCE_URL = "https://graph.microsoft.com/v1.0/me/";

  @Inject
  public AzureClientImpl(MainConfiguration mainConfiguration, SecretManager secretManager)
      throws UnsupportedEncodingException {
    super(secretManager);
    AzureConfig azureConfig = mainConfiguration.getAzureConfig();
    if (azureConfig != null) {
      String clientId = new String(
          Base64.decodeBase64(azureConfig.getClientId().getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
      String clientSecret = new String(
          Base64.decodeBase64(azureConfig.getClientSecret().getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
      clientSecret = URLEncoder.encode(clientSecret, StandardCharsets.UTF_8.name());
      service = new ServiceBuilder(clientId)
                    .apiSecret(clientSecret)
                    .scope("User.Read") // replace with desired scope
                    .callback(azureConfig.getCallbackUrl())
                    .build(new MicrosoftAzureActiveDirectory20ApiV2());
    }
  }

  @Override
  public String getName() {
    return "azure";
  }

  @Override
  public URI getRedirectUrl() {
    URIBuilder uriBuilder;
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
    okhttp3.Response userResource = getUserResource(accessToken);
    OauthUserInfo oauthUserInfo = parseResponseAndFetchUserInfo(userResource);
    populateEmptyFields(oauthUserInfo);
    return oauthUserInfo;
  }

  private OAuth2AccessToken getAccessToken(final String code)
      throws InterruptedException, ExecutionException, IOException {
    return service.getAccessToken(code);
  }

  private OauthUserInfo parseResponseAndFetchUserInfo(okhttp3.Response userResource) throws IOException {
    String email = null;
    String name = null;
    JSONObject jsonObject = new JSONObject(userResource.body().string());
    try {
      email = jsonObject.getString(EMAIL_FIELD_NAME);
      if (jsonObject.has(NAME_FIELD_NAME)) {
        name = jsonObject.getString(NAME_FIELD_NAME);
      }
      return OauthUserInfo.builder().email(email).name(name).build();
    } catch (JSONException je) {
      log.info("Unable to parse json in azure oauth", je);
      return OauthUserInfo.builder().email(email).name(name).build();
    } finally {
      userResource.close();
    }
  }

  private okhttp3.Response getUserResource(final OAuth2AccessToken accessToken) throws IOException {
    Request request = new Builder()
                          .url(PROTECTED_RESOURCE_URL)
                          .addHeader("Authorization", "Bearer " + accessToken.getAccessToken())
                          .build();

    OkHttpClient client = new OkHttpClient();
    return client.newCall(request).execute();
  }

  private static class MicrosoftAzureActiveDirectory20ApiV2 extends DefaultApi20 {
    private static final String MSFT_LOGIN_URL = "https://login.microsoftonline.com";
    private static final String SLASH = "/";
    private static final String COMMON = "common";
    private static final String TOKEN_URI = "oauth2/v2.0/token";
    private static final String AUTHORIZATION_URI = "oauth2/v2.0/authorize";

    @Override
    public String getAccessTokenEndpoint() {
      return MSFT_LOGIN_URL + SLASH + COMMON + SLASH + TOKEN_URI;
    }

    @Override
    protected String getAuthorizationBaseUrl() {
      return MSFT_LOGIN_URL + SLASH + COMMON + SLASH + AUTHORIZATION_URI;
    }
  }
}
