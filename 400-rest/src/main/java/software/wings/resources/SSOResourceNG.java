/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.resources;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static software.wings.security.PermissionAttribute.PermissionType.LOGGED_IN;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.SecretManagerConfig;
import io.harness.beans.SecretText;
import io.harness.eraro.ErrorCode;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.ng.core.account.AuthenticationMechanism;
import io.harness.rest.RestResponse;
import io.harness.secretmanagers.SecretManagerConfigService;
import io.harness.security.annotations.NextGenManagerAuth;

import software.wings.beans.sso.OauthSettings;
import software.wings.security.annotations.AuthRule;
import software.wings.security.authentication.LoginTypeResponse;
import software.wings.security.authentication.LoginTypeResponse.LoginTypeResponseBuilder;
import software.wings.security.authentication.SSOConfig;
import software.wings.security.saml.SamlClientService;
import software.wings.service.intfc.SSOService;
import software.wings.service.intfc.security.SecretManager;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import java.io.InputStream;
import java.util.UUID;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.hibernate.validator.constraints.NotBlank;

@Api(value = "/ng/sso", hidden = true)
@Path("/ng/sso")
@Consumes({MediaType.APPLICATION_JSON, MediaType.MULTIPART_FORM_DATA})
@Produces(MediaType.APPLICATION_JSON)
@NextGenManagerAuth
@Slf4j
@OwnedBy(HarnessTeam.PL)
@TargetModule(HarnessModule._950_NG_AUTHENTICATION_SERVICE)
public class SSOResourceNG {
  private SSOService ssoService;

  @Inject
  public SSOResourceNG(SSOService ssoService) {
    this.ssoService = ssoService;
  }
  @Inject private SamlClientService samlClientService;
  @Inject private SecretManagerConfigService secretManagerConfigService;
  @Inject private SecretManager secretManager;

  @GET
  @Path("get-access-management")
  @Timed
  @AuthRule(permissionType = LOGGED_IN)
  @ExceptionMetered
  public RestResponse<SSOConfig> getAccountAccessManagementSettings(@QueryParam("accountId") String accountId) {
    return new RestResponse<>(ssoService.getAccountAccessManagementSettings(accountId));
  }

  @POST
  @Path("oauth-settings-upload")
  @Timed
  @ExceptionMetered
  public RestResponse<SSOConfig> uploadOathSettings(
      @QueryParam("accountId") String accountId, OauthSettings oauthSettings) {
    return new RestResponse<>(
        ssoService.uploadOauthConfiguration(accountId, oauthSettings.getFilter(), oauthSettings.getAllowedProviders()));
  }

  @PUT
  @Path("assign-auth-mechanism")
  @Timed
  @ExceptionMetered
  public RestResponse<SSOConfig> setAuthMechanism(@QueryParam("accountId") String accountId,
      @QueryParam("authMechanism") AuthenticationMechanism authenticationMechanism) {
    return new RestResponse<>(ssoService.setAuthenticationMechanism(accountId, authenticationMechanism));
  }

  @DELETE
  @Path("delete-oauth-settings")
  @Timed
  @ExceptionMetered
  public RestResponse<SSOConfig> deleteOauthSettings(@QueryParam("accountId") String accountId) {
    return new RestResponse<>(ssoService.deleteOauthConfiguration(accountId));
  }

  @POST
  @Path("saml-idp-metadata-upload")
  @Timed
  @AuthRule(permissionType = LOGGED_IN)
  @ExceptionMetered
  public RestResponse<SSOConfig> uploadSamlMetaData(@QueryParam("accountId") String accountId,
      @FormDataParam("file") InputStream uploadedInputStream, @FormDataParam("displayName") String displayName,
      @FormDataParam("groupMembershipAttr") String groupMembershipAttr,
      @FormDataParam("authorizationEnabled") Boolean authorizationEnabled, @FormDataParam("logoutUrl") String logoutUrl,
      @FormDataParam("entityIdentifier") String entityIdentifier,
      @FormDataParam("samlProviderType") String samlProviderType, @FormDataParam("clientId") String clientId,
      @FormDataParam("clientSecret") String clientSecret) {
    final String clientSecretRef = getCGSecretManagerRefForClientSecret(accountId, true, clientId, clientSecret);
    return new RestResponse<>(ssoService.uploadSamlConfiguration(accountId, uploadedInputStream, displayName,
        groupMembershipAttr, authorizationEnabled, logoutUrl, entityIdentifier, samlProviderType, clientId,
        isEmpty(clientSecretRef) ? null : clientSecretRef.toCharArray()));
  }

  @PUT
  @Path("saml-idp-metadata-upload")
  @Timed
  @AuthRule(permissionType = LOGGED_IN)
  @ExceptionMetered
  public RestResponse<SSOConfig> updateSamlMetaData(@QueryParam("accountId") String accountId,
      @FormDataParam("file") InputStream uploadedInputStream, @FormDataParam("displayName") String displayName,
      @FormDataParam("groupMembershipAttr") String groupMembershipAttr,
      @FormDataParam("authorizationEnabled") Boolean authorizationEnabled, @FormDataParam("logoutUrl") String logoutUrl,
      @FormDataParam("entityIdentifier") String entityIdentifier,
      @FormDataParam("samlProviderType") String samlProviderType, @FormDataParam("clientId") String clientId,
      @FormDataParam("clientSecret") String clientSecret) {
    final String clientSecretRef = getCGSecretManagerRefForClientSecret(accountId, false, clientId, clientSecret);
    return new RestResponse<>(ssoService.updateSamlConfiguration(accountId, uploadedInputStream, displayName,
        groupMembershipAttr, authorizationEnabled, logoutUrl, entityIdentifier, samlProviderType, clientId,
        isEmpty(clientSecretRef) ? null : clientSecretRef.toCharArray()));
  }

  @DELETE
  @Path("delete-saml-idp-metadata")
  @Timed
  @AuthRule(permissionType = LOGGED_IN)
  @ExceptionMetered
  public RestResponse<SSOConfig> deleteSamlMetaData(@QueryParam("accountId") String accountId) {
    return new RestResponse<SSOConfig>(ssoService.deleteSamlConfiguration(accountId));
  }

  @GET
  @Path("saml-login-test")
  public RestResponse<LoginTypeResponse> getSamlLoginTest(@QueryParam("accountId") @NotBlank String accountId) {
    LoginTypeResponseBuilder builder = LoginTypeResponse.builder();
    try {
      builder.SSORequest(samlClientService.generateTestSamlRequest(accountId));
      return new RestResponse<>(builder.authenticationMechanism(AuthenticationMechanism.SAML).build());
    } catch (Exception e) {
      throw new WingsException(ErrorCode.INVALID_SAML_CONFIGURATION);
    }
  }

  private String getCGSecretManagerRefForClientSecret(
      final String accountId, final boolean isCreateCall, final String clientId, final String clientSecret) {
    final String validationErrorMsg = "Both clientId and clientSecret needs to be provided together for SAML setting";
    if (isCreateCall) {
      if (isNotEmpty(clientId) && isEmpty(clientSecret) || isEmpty(clientId) && isNotEmpty(clientSecret)) {
        throw new InvalidRequestException(validationErrorMsg, WingsException.USER);
      }
    }
    if (isEmpty(clientSecret)) {
      return null;
    }
    if (!isCreateCall && isNotEmpty(clientId) && "********".equals(clientSecret)) {
      return clientSecret;
    } else if (!isCreateCall && isEmpty(clientId) && isNotEmpty(clientSecret)) {
      throw new InvalidRequestException(validationErrorMsg, WingsException.USER);
    }
    SecretManagerConfig secretManagerConfig = secretManagerConfigService.getDefaultSecretManager(accountId);
    SecretText secretText = new SecretText();
    secretText.setValue(clientSecret);
    String secretName = "ClientSecret-" + accountId + "-" + UUID.randomUUID();
    secretText.setName(secretName);
    secretText.setScopedToAccount(true);
    secretText.setKmsId(secretManagerConfig.getUuid());
    return secretManager.saveSecretText(accountId, secretText, true);
  }
}
