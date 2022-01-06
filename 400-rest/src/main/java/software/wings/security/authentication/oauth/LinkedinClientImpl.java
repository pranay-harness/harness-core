/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.security.authentication.oauth;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;

import software.wings.app.MainConfiguration;
import software.wings.security.SecretManager;

import com.github.scribejava.apis.LinkedInApi20;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.ExecutionException;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.utils.URIBuilder;

@OwnedBy(PL)
@Singleton
@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
public class LinkedinClientImpl extends BaseOauthClient implements OauthClient {
  OAuth20Service service;

  static final String PROTECTED_RESOURCE_URL = "https://api.linkedin.com/v2/me";
  private static final String GET_EMAIL_ADDRESS_URL =
      "https://api.linkedin.com/v2/emailAddress?q=members&projection=(elements*(handle~))";

  @Inject
  public LinkedinClientImpl(MainConfiguration mainConfiguration, SecretManager secretManager) {
    super(secretManager);
    LinkedinConfig linkedinConfig = mainConfiguration.getLinkedinConfig();
    if (linkedinConfig != null) {
      service = new ServiceBuilder(linkedinConfig.getClientId())
                    .apiSecret(linkedinConfig.getClientSecret())
                    .callback(linkedinConfig.getCallbackUrl())
                    .scope("r_liteprofile r_emailaddress")
                    .build(LinkedInApi20.instance());
    }
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
    String email = getEmailAddressFromLinkedIn(accessToken);
    String name = getValueFromLinkedin(accessToken, email);
    return OauthUserInfo.builder().email(email).name(name).build();
  }

  private String getEmailAddressFromLinkedIn(OAuth2AccessToken accessToken)
      throws InterruptedException, ExecutionException, IOException {
    final OAuthRequest request = new OAuthRequest(Verb.GET, GET_EMAIL_ADDRESS_URL);
    request.addHeader("x-li-format", "json");
    service.signRequest(accessToken, request);
    final Response response = service.execute(request);
    String responseJson = response.getBody();
    log.info("LinkedIn json response for get email address: {}", responseJson);
    DocumentContext jsonContext = JsonPath.parse(responseJson);
    return jsonContext.read("$['elements'][0]['handle~']['emailAddress']");
  }

  private String getValueFromLinkedin(OAuth2AccessToken accessToken, String email)
      throws InterruptedException, ExecutionException, IOException {
    final OAuthRequest request = new OAuthRequest(Verb.GET, PROTECTED_RESOURCE_URL);
    request.addHeader("x-li-format", "json");
    service.signRequest(accessToken, request);
    final Response response = service.execute(request);
    String responseJson = response.getBody();
    log.info("LinkedIn json response for getting user profile: {}", responseJson);
    DocumentContext jsonContext = JsonPath.parse(responseJson);
    String firstName = jsonContext.read("$['firstName']['localized']['en_US']");
    String lastName = jsonContext.read("$['lastName']['localized']['en_US']");
    String name = email;
    if (firstName != null || lastName != null) {
      name = firstName + " " + lastName;
    }
    return name;
  }
}
