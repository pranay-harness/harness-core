/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.resources;

import static io.harness.annotations.dev.HarnessModule._950_NG_SIGNUP;
import static io.harness.annotations.dev.HarnessTeam.GTM;
import static io.harness.eraro.ErrorCode.INVALID_REQUEST;

import static software.wings.security.PermissionAttribute.PermissionType.LOGGED_IN;
import static software.wings.signup.BugsnagConstants.CLUSTER_TYPE;
import static software.wings.signup.BugsnagConstants.FREEMIUM;
import static software.wings.signup.BugsnagConstants.ONBOARDING;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.eraro.Level;
import io.harness.exception.SignupException;
import io.harness.exception.WeakPasswordException;
import io.harness.exception.WingsException;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.PublicApi;

import software.wings.beans.BugsnagTab;
import software.wings.beans.ErrorData;
import software.wings.beans.User;
import software.wings.beans.UserInvite;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.Scope;
import software.wings.service.intfc.signup.AzureMarketplaceIntegrationService;
import software.wings.service.intfc.signup.SignupService;
import software.wings.signup.BugsnagErrorReporter;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import java.util.Collections;
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;

@Api("signup")
@Path("/signup")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Scope(ResourceType.USER)
@AuthRule(permissionType = LOGGED_IN)
@OwnedBy(GTM)
@Slf4j
@TargetModule(_950_NG_SIGNUP)
public class SignupResource {
  @Inject SignupService signupService;
  @Inject AzureMarketplaceIntegrationService azureMarketplaceIntegrationService;
  @Inject BugsnagErrorReporter bugsnagErrorReporter;

  private static final String FAILURE_MESSAGE = "Failed to complete signup";
  private static final String FAILURE_MESSAGE_CONTACT_SUPPORT = "Failed to signup. Please contact harness support";
  private static final List<BugsnagTab> tab =
      Collections.singletonList(BugsnagTab.builder().tabName(CLUSTER_TYPE).key(FREEMIUM).value(ONBOARDING).build());

  /**
   *  Start the trial registration with email and user info. (Doesn't contains password)
   *  A verification email will be sent to the specified email address asking him to setup his password.
   *  On successful verification, it creates the account and registers the user.
   *
   * @param userInvite user invite with email and user info
   */
  @PublicApi
  @POST
  @Path("{source}")
  public RestResponse<Boolean> signup(UserInvite userInvite, @PathParam("source") String source) {
    try {
      return new RestResponse<>(signupService.signup(userInvite, source));
    } catch (SignupException ex) {
      bugsnagErrorReporter.report(ErrorData.builder().exception(ex).email(userInvite.getEmail()).tabs(tab).build());
      throw ex;
    } catch (WeakPasswordException ex) {
      log.error("Password validation failed");
      bugsnagErrorReporter.report(ErrorData.builder().exception(ex).email(userInvite.getEmail()).tabs(tab).build());
      throw new SignupException(ex.getMessage(), ex, INVALID_REQUEST, Level.ERROR, WingsException.USER, null);
    } catch (Exception ex) {
      log.error(FAILURE_MESSAGE, ex);
      bugsnagErrorReporter.report(ErrorData.builder().exception(ex).email(userInvite.getEmail()).tabs(tab).build());
      throw new SignupException(FAILURE_MESSAGE_CONTACT_SUPPORT);
    }
  }

  @PublicApi
  @GET
  @Path("check-validity/azure-marketplace")
  public Response validateToken(@QueryParam("token") String azureMarketplaceToken) {
    try {
      return signupService.checkValidity(azureMarketplaceToken);
    } catch (Exception ex) {
      bugsnagErrorReporter.report(ErrorData.builder().exception(ex).tabs(tab).build());
      throw new SignupException(FAILURE_MESSAGE_CONTACT_SUPPORT);
    }
  }

  @PublicApi
  @POST
  @Path("complete/{token}")
  public RestResponse<User> completeSignup(@NotEmpty @PathParam("token") String secretToken) {
    try {
      return new RestResponse<>(signupService.completeSignup(secretToken));
    } catch (SignupException ex) {
      bugsnagErrorReporter.report(ErrorData.builder().exception(ex).tabs(tab).build());
      throw ex;
    } catch (WeakPasswordException ex) {
      log.error("Password validation failed");
      bugsnagErrorReporter.report(ErrorData.builder().exception(ex).tabs(tab).build());
      throw new SignupException(ex.getMessage(), ex, INVALID_REQUEST, Level.ERROR, WingsException.USER, null);
    } catch (Exception ex) {
      log.error(FAILURE_MESSAGE, ex);
      bugsnagErrorReporter.report(ErrorData.builder().exception(ex).tabs(tab).build());
      throw new SignupException(FAILURE_MESSAGE_CONTACT_SUPPORT);
    }
  }

  @PublicApi
  @PUT
  @Path("azure-marketplace/complete")
  public RestResponse<User> completeAzureSignup(@QueryParam("token") String token) {
    try {
      return new RestResponse<>(signupService.completeAzureMarketplaceSignup(token));
    } catch (SignupException ex) {
      bugsnagErrorReporter.report(ErrorData.builder().exception(ex).tabs(tab).build());
      throw ex;
    } catch (Exception ex) {
      log.error(FAILURE_MESSAGE, ex);
      bugsnagErrorReporter.report(ErrorData.builder().exception(ex).tabs(tab).build());
      throw new SignupException(FAILURE_MESSAGE_CONTACT_SUPPORT);
    }
  }
}
