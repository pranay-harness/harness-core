/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.intfc.signup;

import static io.harness.annotations.dev.HarnessModule._950_NG_SIGNUP;
import static io.harness.annotations.dev.HarnessTeam.GTM;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.exception.SignupException;

import software.wings.beans.User;
import software.wings.beans.UserInvite;
import software.wings.beans.UserInviteSource.SourceType;
import software.wings.security.authentication.AuthenticationUtils;
import software.wings.security.authentication.SimpleUrlBuilder;
import software.wings.signup.OnpremSignupHandler;

import com.google.inject.Inject;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import javax.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(GTM)
@TargetModule(_950_NG_SIGNUP)
@Slf4j
public class SignupService {
  @Inject MarketoSignupHandler marketoSignupHandler;
  @Inject OnpremSignupHandler onpremSignupHandler;
  @Inject AzureMarketplaceIntegrationService azureMarketplaceIntegrationService;
  @Inject AzureMarketplaceSignupHandler azureMarketplaceSignupHandler;
  @Inject AuthenticationUtils authenticationUtils;

  private static final String TOKEN = "token";
  private static final String ERROR_PATH = "#/login?errorcode=invalidToken";
  private static final String AZURE_SIGNUP = "#/azure-signup";

  public boolean signup(UserInvite userInvite, String source) {
    SourceType sourceType = SourceType.valueOf(source);
    switch (sourceType) {
      case MARKETO_LINKEDIN:
        return marketoSignupHandler.handle(userInvite);
      case ONPREM:
        return onpremSignupHandler.handle(userInvite);
      case AZURE_MARKETPLACE:
        return azureMarketplaceSignupHandler.handle(userInvite);
      default:
        throw new SignupException(String.format("Incorrect source type provided: %s", userInvite.getSource()));
    }
  }

  public User completeSignup(String resetPasswordToken) {
    return marketoSignupHandler.completeSignup(resetPasswordToken);
  }

  public User completeAzureMarketplaceSignup(String resetPasswordToken) {
    return azureMarketplaceSignupHandler.completeSignup(null, resetPasswordToken);
  }

  public Response checkValidity(String token) throws URISyntaxException {
    String baseUrl;
    String redirectUrl;
    try {
      boolean validToken = azureMarketplaceIntegrationService.validate(token);
      if (validToken) {
        baseUrl = authenticationUtils.getBaseUrl() + AZURE_SIGNUP;
        redirectUrl = new SimpleUrlBuilder(baseUrl)
                          .addQueryParam(TOKEN, URLEncoder.encode(token, StandardCharsets.UTF_8.name()))
                          .build();
      } else {
        baseUrl = authenticationUtils.getBaseUrl() + ERROR_PATH;
        redirectUrl = new SimpleUrlBuilder(baseUrl).build();
      }
    } catch (Exception ex) {
      baseUrl = authenticationUtils.getBaseUrl() + ERROR_PATH;
      redirectUrl = new SimpleUrlBuilder(baseUrl).build();
    }
    log.info("Base url is: {},  Redirect url is {}", authenticationUtils.getBaseUrl(), redirectUrl);
    return Response.seeOther(new URI(redirectUrl)).build();
  }
}
