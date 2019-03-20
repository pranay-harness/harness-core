package software.wings.resources;

import static com.google.common.collect.ImmutableMap.of;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.ReportTarget.REST_API;
import static io.harness.exception.WingsException.USER;
import static software.wings.beans.Application.GLOBAL_APP_ID;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter.Operator;
import io.harness.eraro.ErrorCode;
import io.harness.eraro.ResponseMessage;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.logging.ExceptionLogger;
import io.harness.rest.RestResponse;
import io.harness.rest.RestResponse.Builder;
import io.swagger.annotations.Api;
import lombok.Data;
import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.Account;
import software.wings.beans.AccountRole;
import software.wings.beans.ApplicationRole;
import software.wings.beans.FeatureFlag;
import software.wings.beans.User;
import software.wings.beans.UserInvite;
import software.wings.beans.ZendeskSsoLoginResponse;
import software.wings.beans.security.UserGroup;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.UserPermissionInfo;
import software.wings.security.UserThreadLocal;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.PublicApi;
import software.wings.security.annotations.Scope;
import software.wings.security.authentication.AuthenticationManager;
import software.wings.security.authentication.LoginTypeResponse;
import software.wings.security.authentication.SsoRedirectRequest;
import software.wings.security.authentication.TwoFactorAdminOverrideSettings;
import software.wings.security.authentication.TwoFactorAuthenticationManager;
import software.wings.security.authentication.TwoFactorAuthenticationMechanism;
import software.wings.security.authentication.TwoFactorAuthenticationSettings;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AuthService;
import software.wings.service.intfc.HarnessUserGroupService;
import software.wings.service.intfc.UserGroupService;
import software.wings.service.intfc.UserService;
import software.wings.utils.CacheHelper;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Users Resource class.
 *
 * @author Rishi
 */
@Api("users")
@Path("/users")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Scope(ResourceType.USER)
@AuthRule(permissionType = PermissionType.LOGGED_IN)
public class UserResource {
  private UserService userService;
  private AuthService authService;
  private AccountService accountService;
  private AuthenticationManager authenticationManager;
  private TwoFactorAuthenticationManager twoFactorAuthenticationManager;
  private CacheHelper cacheHelper;
  private HarnessUserGroupService harnessUserGroupService;
  private UserGroupService userGroupService;

  /**
   * Instantiates a new User resource.
   * @param userService
   * @param authService
   * @param accountService
   * @param authenticationManager
   * @param twoFactorAuthenticationManager
   * @param cacheHelper
   * @param harnessUserGroupService
   */
  @Inject
  public UserResource(UserService userService, AuthService authService, AccountService accountService,
      AuthenticationManager authenticationManager, TwoFactorAuthenticationManager twoFactorAuthenticationManager,
      CacheHelper cacheHelper, HarnessUserGroupService harnessUserGroupService, UserGroupService userGroupService) {
    this.userService = userService;
    this.authService = authService;
    this.accountService = accountService;
    this.authenticationManager = authenticationManager;
    this.twoFactorAuthenticationManager = twoFactorAuthenticationManager;
    this.cacheHelper = cacheHelper;
    this.harnessUserGroupService = harnessUserGroupService;
    this.userGroupService = userGroupService;
  }

  /**
   * List.
   *
   * @param pageRequest the page request
   * @param accountId   the account id
   * @return the rest response
   */
  @GET
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.USER_PERMISSION_MANAGEMENT)
  public RestResponse<PageResponse<User>> list(@BeanParam PageRequest<User> pageRequest,
      @QueryParam("accountId") @NotEmpty String accountId,
      @QueryParam("details") @DefaultValue("true") boolean loadUserGroups) {
    Account account = accountService.get(accountId);
    pageRequest.addFilter("accounts", Operator.HAS, account);
    PageResponse<User> pageResponse = userService.list(pageRequest, loadUserGroups);
    List<User> userList = pageResponse.getResponse();
    if (isNotEmpty(userList)) {
      userList.forEach(user -> user.setAccounts(Arrays.asList(account)));
    }
    return getPublicUsers(pageResponse);
  }

  /**
   * Register a user with account/company name. The user and account will be created as part of this operation
   *
   * @param user the user
   * @return the rest response
   */
  @POST
  @PublicApi
  @Timed
  @ExceptionMetered
  public RestResponse<User> register(User user) {
    user.setAppId(GLOBAL_APP_ID);
    return new RestResponse<>(userService.register(user));
  }

  /**
   *  Start the trial registration with an email. A verification/signup email will be sent to the
   *  specified email address for the next steps in completing the signup process.
   *
   * @param email the email of the user who is registering for a trial account.
   */
  @PublicApi
  @POST
  @Path("trial")
  @Consumes(MediaType.TEXT_PLAIN)
  @Timed
  @ExceptionMetered
  public RestResponse<Boolean> trialSignup(String email) {
    return new RestResponse<>(userService.trialSignup(email));
  }

  @POST
  @Path("account")
  @Timed
  @ExceptionMetered
  public RestResponse<Account> addAccount(
      Account account, @QueryParam("addUser") @DefaultValue("true") boolean addUser) {
    User existingUser = UserThreadLocal.get();
    if (existingUser == null) {
      throw new InvalidRequestException("Invalid User");
    }

    if (harnessUserGroupService.isHarnessSupportUser(existingUser.getUuid())) {
      return new RestResponse<>(userService.addAccount(account, existingUser, addUser));
    } else {
      return Builder.aRestResponse()
          .withResponseMessages(
              Lists.newArrayList(ResponseMessage.builder().message("User not allowed to add account").build()))
          .build();
    }
  }

  /**
   * Update User group
   *
   * @param userId the user id
   * @param user   the user
   * @return the rest response
   */
  @PUT
  @Path("user/{userId}")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.USER_PERMISSION_MANAGEMENT)
  public RestResponse<User> updateUserGroupsAndNameOfUser(
      @QueryParam("accountId") @NotEmpty String accountId, @PathParam("userId") String userId, User user) {
    return getPublicUser(
        userService.updateUserGroupsAndNameOfUser(userId, user.getUserGroups(), user.getName(), accountId, true));
  }

  /**
   * Update User profile
   *
   * @param userId the user id
   * @param user   the user
   * @return the rest response
   */
  @PUT
  @Path("profile/{userId}")
  @Timed
  @ExceptionMetered
  public RestResponse<User> updateUserProfile(
      @QueryParam("accountId") @NotEmpty String accountId, @PathParam("userId") String userId, @NotNull User user) {
    User authUser = UserThreadLocal.get();
    if (!authUser.getUuid().equals(userId)) {
      throw new WingsException(ErrorCode.ACCESS_DENIED, USER);
    }
    user.setUuid(userId);
    if (isEmpty(user.getAppId())) {
      user.setAppId(GLOBAL_APP_ID);
    }
    return new RestResponse<>(userService.update(user));
  }

  /**
   * Reset all caches.
   *
   * @return the rest response
   */
  @PUT
  @Path("reset-cache")
  @Scope(value = ResourceType.USER, scope = PermissionType.LOGGED_IN)
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.LOGGED_IN)
  public RestResponse resetCache() {
    User authUser = UserThreadLocal.get();
    if (harnessUserGroupService.isHarnessSupportUser(authUser.getUuid())) {
      cacheHelper.resetAllCaches();
    } else {
      return Builder.aRestResponse()
          .withResponseMessages(Lists.newArrayList(
              ResponseMessage.builder().message("User not allowed to perform the reset-cache operation").build()))
          .build();
    }

    return Builder.aRestResponse()
        .withResponseMessages(Lists.newArrayList(ResponseMessage.builder().message("Cache reset successful").build()))
        .build();
  }

  /**
   * Delete.
   *
   * @param accountId the account id
   * @param userId    the user id
   * @return the rest response
   */
  @DELETE
  @Path("{userId}")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.USER_PERMISSION_MANAGEMENT)
  public RestResponse delete(@QueryParam("accountId") @NotEmpty String accountId, @PathParam("userId") String userId) {
    userService.delete(accountId, userId);
    return new RestResponse();
  }

  /**
   * Reset password rest response.
   *
   * @param passwordRequest the password request
   * @return the rest response
   */
  @PublicApi
  @POST
  @Path("reset-password")
  @Timed
  @ExceptionMetered
  public RestResponse resetPassword(@NotNull ResetPasswordRequest passwordRequest) {
    return new RestResponse<>(userService.resetPassword(passwordRequest.getEmail()));
  }

  /**
   * Resend invitation email
   *
   * @param invitationEmailRequest the invitation email request
   * @return the rest response
   */
  @POST
  @Path("resend-invitation-email")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.USER_PERMISSION_MANAGEMENT)
  public RestResponse resendInvitationEmail(@QueryParam("accountId") @NotBlank String accountId,
      @Valid @NotNull ResendInvitationEmailRequest invitationEmailRequest) {
    return new RestResponse<>(
        userService.resendInvitationEmail(userService, accountId, invitationEmailRequest.getEmail()));
  }

  /**
   * Update password rest response.
   *
   * @param resetPasswordToken    the reset password token
   * @param updatePasswordRequest the update password request
   * @return the rest response
   */
  @PublicApi
  @POST
  @Path("reset-password/{token}")
  @Timed
  @ExceptionMetered
  public RestResponse updatePassword(
      @NotEmpty @PathParam("token") String resetPasswordToken, UpdatePasswordRequest updatePasswordRequest) {
    return new RestResponse(
        userService.updatePassword(resetPasswordToken, updatePasswordRequest.getPassword().toCharArray()));
  }

  /**
   * Get rest response.
   *
   * @param accountName the account name
   * @return the rest response
   */
  @GET
  @Path("account-name/{accountName}")
  @PublicApi
  @Timed
  @ExceptionMetered
  public RestResponse<String> suggestAccountName(@PathParam("accountName") String accountName) {
    return new RestResponse<>(accountService.suggestAccountName(accountName));
  }

  /**
   * Get rest response.
   *
   * @return the rest response
   */
  @GET
  @Path("user")
  @Scope(value = ResourceType.USER, scope = PermissionType.LOGGED_IN)
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.LOGGED_IN)
  public RestResponse<User> get() {
    return new RestResponse<>(UserThreadLocal.get().getPublicUser());
  }

  /**
   * Get rest response.
   *
   * @param accountId the account id
   * @return the rest response
   */
  @GET
  @Path("account-roles/{accountId}")
  @Scope(value = ResourceType.USER, scope = PermissionType.LOGGED_IN)
  @Timed
  @ExceptionMetered
  public RestResponse<AccountRole> getAccountRole(@PathParam("accountId") String accountId) {
    return new RestResponse<>(
        userService.getUserAccountRole(UserThreadLocal.get().getPublicUser().getUuid(), accountId));
  }

  @GET
  @Path("user-permissions/{accountId}")
  @Scope(value = ResourceType.USER, scope = PermissionType.LOGGED_IN)
  @Timed
  @ExceptionMetered
  public RestResponse<UserPermissionInfo> getUserPermissionInfo(@PathParam("accountId") String accountId) {
    return new RestResponse<>(authService.getUserPermissionInfo(accountId, UserThreadLocal.get().getPublicUser()));
  }

  /**
   * Get list of features flags and their statuses
   *
   * @param accountId account id
   * @return the rest response
   */
  @GET
  @Path("feature-flags/{accountId}")
  @Scope(value = ResourceType.USER)
  @AuthRule(permissionType = PermissionType.LOGGED_IN)
  @Timed
  @ExceptionMetered
  public RestResponse<Collection<FeatureFlag>> getFeatureFlags(@PathParam("accountId") String accountId) {
    return new RestResponse<>(accountService.getFeatureFlags(accountId));
  }

  /**
   * Get rest response.
   *
   * @param appId the app id
   * @return the rest response
   */
  @GET
  @Path("application-roles/{appId}")
  @Scope(value = ResourceType.USER, scope = PermissionType.LOGGED_IN)
  @Timed
  @ExceptionMetered
  public RestResponse<ApplicationRole> getApplicationRole(@PathParam("appId") String appId) {
    return new RestResponse<>(
        userService.getUserApplicationRole(UserThreadLocal.get().getPublicUser().getUuid(), appId));
  }

  /**
   * Explicitly switch account for a logged in user. A new JWT bearer token with the new account ID will be generated
   * and returned as part of the response.
   */
  @GET
  @Path("switch-account")
  @PublicApi
  @Timed
  @ExceptionMetered
  public RestResponse<User> switchAccount(
      @HeaderParam(HttpHeaders.AUTHORIZATION) String authorization, @QueryParam("accountId") String accountId) {
    return new RestResponse<>(
        authenticationManager.switchAccount(authenticationManager.extractToken(authorization, "Bearer"), accountId));
  }

  /**
   * Login.
   *
   * @return the rest response
   */
  @GET
  @Path("login")
  @PublicApi
  @Timed
  @ExceptionMetered
  public RestResponse<User> login(@HeaderParam(HttpHeaders.AUTHORIZATION) String authorization) {
    return new RestResponse<>(
        authenticationManager.defaultLogin(authenticationManager.extractToken(authorization, "Basic")));
  }

  /**
   * Login.
   *
   * @return the rest response
   */
  @GET
  @Path("two-factor-login")
  @PublicApi
  @Timed
  @ExceptionMetered
  public RestResponse<User> twoFactorLogin(@HeaderParam(HttpHeaders.AUTHORIZATION) String authorization) {
    return new RestResponse<>(
        twoFactorAuthenticationManager.authenticate(authenticationManager.extractToken(authorization, "JWT")));
  }

  @GET
  @Path("logintype")
  @PublicApi
  @Timed
  @ExceptionMetered
  public RestResponse<LoginTypeResponse> getLoginType(@QueryParam("userName") String userName) {
    return new RestResponse(authenticationManager.getLoginTypeResponse(urlDecode(userName)));
  }

  @GET
  @Path("oauth2Redirect")
  @PublicApi
  public Response oauth2Redirect(@QueryParam("provider") @NotNull String provider) {
    return authenticationManager.oauth2Redirect(provider.toLowerCase());
  }

  @GET
  @Path("oauth2/{provider}")
  @PublicApi
  public Response oauth2CallbackUrl(@QueryParam("code") String code, @QueryParam("state") String state,
      @PathParam("provider") @NotNull String provider) {
    try {
      return authenticationManager.oauth2CallbackUrl(code, state, provider.toLowerCase());
    } catch (URISyntaxException e) {
      throw new WingsException(ErrorCode.UNKNOWN_ERROR, e);
    }
  }

  @POST
  @Path("sso-redirect-login")
  @PublicApi
  @Timed
  @ExceptionMetered
  public RestResponse<User> redirectlogin(SsoRedirectRequest request) {
    return new RestResponse<>(authenticationManager.ssoRedirectLogin(request.getJwtToken()));
  }

  @GET
  @Path("two-factor-auth/{auth-mechanism}")
  @Timed
  @ExceptionMetered
  public RestResponse<TwoFactorAuthenticationSettings> getTwoFactorAuthSettings(
      @PathParam("auth-mechanism") TwoFactorAuthenticationMechanism authMechanism) {
    return new RestResponse(
        twoFactorAuthenticationManager.createTwoFactorAuthenticationSettings(UserThreadLocal.get(), authMechanism));
  }

  @PUT
  @Path("override-two-factor-auth/{accountId}")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.ACCOUNT_MANAGEMENT)
  public RestResponse<TwoFactorAdminOverrideSettings> setTwoFactorAuthAtAccountLevel(
      @PathParam("accountId") @NotEmpty String accountId, TwoFactorAdminOverrideSettings settings) {
    // Trying Override = true
    if (settings.isAdminOverrideTwoFactorEnabled()) {
      if (twoFactorAuthenticationManager.isTwoFactorEnabledForAdmin(accountId, UserThreadLocal.get())) {
        return new RestResponse(
            twoFactorAuthenticationManager.overrideTwoFactorAuthentication(accountId, UserThreadLocal.get(), settings));
      } else {
        return Builder.aRestResponse()
            .withResponseMessages(
                Lists.newArrayList(ResponseMessage.builder()
                                       .message("Admin has 2FA disabled. Please enable to enforce 2FA on users.")
                                       .build()))
            .build();
      }
    }
    // Trying Override = false
    else {
      return new RestResponse(
          twoFactorAuthenticationManager.overrideTwoFactorAuthentication(accountId, UserThreadLocal.get(), settings));
    }
  }

  @GET
  @Path("two-factor-auth-info/{accountId}")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.LOGGED_IN)
  public RestResponse<Boolean> getTwoFactorAuthAdminEnforceInfo(@PathParam("accountId") @NotEmpty String accountId) {
    return new RestResponse(twoFactorAuthenticationManager.getTwoFactorAuthAdminEnforceInfo(accountId));
  }

  @PUT
  @Path("disable-two-factor-auth")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.LOGGED_IN)
  public RestResponse<User> disableTwoFactorAuth() {
    return new RestResponse(twoFactorAuthenticationManager.disableTwoFactorAuthentication(UserThreadLocal.get()));
  }

  @PUT
  @Path("enable-two-factor-auth")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.LOGGED_IN)
  public RestResponse<User> enableTwoFactorAuth(TwoFactorAuthenticationSettings settings) {
    return new RestResponse(
        twoFactorAuthenticationManager.enableTwoFactorAuthenticationSettings(UserThreadLocal.get(), settings));
  }

  @POST
  @Path("saml-login")
  @Produces(MediaType.TEXT_HTML)
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  @PublicApi
  @Timed
  @ExceptionMetered
  public javax.ws.rs.core.Response samlLogin(@FormParam(value = "SAMLResponse") String samlResponse,
      @Context HttpServletRequest request, @Context HttpServletResponse response) {
    try {
      return authenticationManager.samlLogin(
          request.getHeader(com.google.common.net.HttpHeaders.REFERER), samlResponse);
    } catch (URISyntaxException e) {
      throw new WingsException(ErrorCode.UNKNOWN_ERROR, e);
    }
  }

  /**
   * Logout.
   *
   * @param userId the user id
   * @return the rest response
   */
  @POST
  @Path("{userId}/logout")
  @Scope(value = ResourceType.USER, scope = PermissionType.LOGGED_IN)
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.LOGGED_IN, skipAuth = true)
  public RestResponse logout(@PathParam("userId") String userId) {
    User user = UserThreadLocal.get();
    userService.logout(user);
    return new RestResponse();
  }

  /**
   * Verify token rest response.
   *
   * @param token the token
   * @return the rest response
   * @throws URISyntaxException the uri syntax exception
   */
  @GET
  @Path("verify/{token}")
  @PublicApi
  @Timed
  @ExceptionMetered
  public RestResponse<Map<String, Object>> verifyToken(@PathParam("token") String token) throws URISyntaxException {
    return new RestResponse<>(of("success", userService.verifyToken(token)));
  }

  /**
   * Verify email rest response.
   *
   * @param email the token
   * @return the rest response
   * @throws URISyntaxException the uri syntax exception
   */
  @GET
  @Path("verify-email")
  @PublicApi
  @Timed
  @ExceptionMetered
  public RestResponse<Boolean> verifyEmail(@QueryParam("email") String email) throws URISyntaxException {
    try {
      userService.verifyRegisteredOrAllowed(email);
    } catch (WingsException exception) {
      // TODO: this seems wrong, just letting the exception to be thrown should do the same
      return RestResponse.Builder.aRestResponse()
          .withResource(false)
          .withResponseMessages(ExceptionLogger.getResponseMessageList(exception, REST_API))
          .build();
    }
    return new RestResponse<>(true);
  }

  /**
   * Resend verification email.
   *
   * @param email the token
   * @return the rest response
   * @throws URISyntaxException the uri syntax exception
   */
  @GET
  @Path("resend-verification-email/{email}")
  @PublicApi
  @Timed
  @ExceptionMetered
  public RestResponse<Boolean> resendVerificationEmail(@PathParam("email") String email) throws URISyntaxException {
    return new RestResponse<>(userService.resendVerificationEmail(email));
  }

  /**
   * Assign role.
   *
   * @param userId the user id
   * @param roleId the role id
   * @return the rest response
   */
  @PUT
  @Path("{userId}/role/{roleId}")
  @Timed
  @ExceptionMetered
  public RestResponse<User> assignRole(@PathParam("userId") String userId, @PathParam("roleId") String roleId) {
    return getPublicUser(userService.addRole(userId, roleId));
  }

  /**
   * Revoke role.
   *
   * @param userId the user id
   * @param roleId the role id
   * @return the rest response
   */
  @DELETE
  @Path("{userId}/role/{roleId}")
  @Timed
  @ExceptionMetered
  public RestResponse<User> revokeRole(@PathParam("userId") String userId, @PathParam("roleId") String roleId) {
    return getPublicUser(userService.revokeRole(userId, roleId));
  }

  /**
   * List invites rest response.
   *
   * @param accountId   the account id
   * @param pageRequest the page request
   * @return the rest response
   */
  @GET
  @Path("invites")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.USER_PERMISSION_MANAGEMENT)
  public RestResponse<PageResponse<UserInvite>> listInvites(
      @QueryParam("accountId") @NotEmpty String accountId, @BeanParam PageRequest<UserInvite> pageRequest) {
    return getPublicUserInvites(userService.listInvites(pageRequest));
  }

  /**
   * Gets invite.
   *
   * @param accountId the account id
   * @param inviteId  the invite id
   * @return the invite
   */
  @GET
  @Path("invites/{inviteId}")
  @Timed
  @ExceptionMetered
  public RestResponse<UserInvite> getInvite(
      @QueryParam("accountId") @NotEmpty String accountId, @PathParam("inviteId") @NotEmpty String inviteId) {
    return getPublicUserInvite(userService.getInvite(accountId, inviteId));
  }

  /**
   * Invite users rest response.
   *
   * @param accountId  the account id
   * @param userInvite the user invite
   * @return the rest response
   */
  @POST
  @Path("invites")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.USER_PERMISSION_MANAGEMENT)
  public RestResponse<List<UserInvite>> inviteUsers(
      @QueryParam("accountId") @NotEmpty String accountId, @NotNull UserInvite userInvite) {
    userInvite.setAccountId(accountId);
    userInvite.setAppId(GLOBAL_APP_ID);
    return getPublicUserInvites(userService.inviteUsers(userInvite));
  }

  @GET
  @Path("refresh-token")
  @Timed
  @ExceptionMetered
  public RestResponse<String> refreshToken(@HeaderParam(HttpHeaders.AUTHORIZATION) String oldToken) {
    return new RestResponse<>(authenticationManager.refreshToken(oldToken));
  }

  /**
   * Complete invite rest response.
   *
   * @param accountId  the account id
   * @param inviteId   the invite id
   * @param userInvite the user invite
   * @return the rest response
   */
  @PublicApi
  @PUT
  @Path("invites/{inviteId}")
  @Timed
  @ExceptionMetered
  public RestResponse<UserInvite> completeInvite(@QueryParam("accountId") @NotEmpty String accountId,
      @PathParam("inviteId") @NotEmpty String inviteId, @NotNull UserInvite userInvite) {
    userInvite.setAccountId(accountId);
    userInvite.setUuid(inviteId);
    return getPublicUserInvite(userService.completeInvite(userInvite));
  }

  @PublicApi
  @PUT
  @Path("invites/{inviteId}/signin")
  @Timed
  @ExceptionMetered
  public RestResponse<User> completeInviteAndSignIn(@QueryParam("accountId") @NotEmpty String accountId,
      @PathParam("inviteId") @NotEmpty String inviteId, @NotNull UserInvite userInvite) {
    userInvite.setAccountId(accountId);
    userInvite.setUuid(inviteId);
    return new RestResponse<>(userService.completeInviteAndSignIn(userInvite));
  }

  @PublicApi
  @PUT
  @Path("invites/trial/{inviteId}")
  @Timed
  @ExceptionMetered
  public RestResponse<UserInvite> completeTrialSignup(@QueryParam("account") @NotEmpty String accountName,
      @QueryParam("company") @NotEmpty String companyName, @PathParam("inviteId") @NotEmpty String inviteId,
      @NotNull UserInvite userInvite) {
    userInvite.setUuid(inviteId);
    User user = User.Builder.anUser()
                    .withEmail(userInvite.getEmail())
                    .withName(userInvite.getName())
                    .withPassword(userInvite.getPassword())
                    .withAccountName(accountName)
                    .withCompanyName(companyName)
                    .build();
    return getPublicUserInvite(userService.completeTrialSignup(user, userInvite));
  }

  @PublicApi
  @PUT
  @Path("invites/trial/{inviteId}/signin")
  @Timed
  @ExceptionMetered
  public RestResponse<User> completeTrialSignupAndSignIn(@QueryParam("account") @NotEmpty String accountName,
      @QueryParam("company") @NotEmpty String companyName, @PathParam("inviteId") @NotEmpty String inviteId,
      @NotNull UserInvite userInvite) {
    userInvite.setUuid(inviteId);
    User user = User.Builder.anUser()
                    .withEmail(userInvite.getEmail())
                    .withName(userInvite.getName())
                    .withPassword(userInvite.getPassword())
                    .withAccountName(accountName)
                    .withCompanyName(companyName)
                    .build();
    return new RestResponse<>(userService.completeTrialSignupAndSignIn(user, userInvite));
  }

  /**
   * Delete invite rest response.
   *
   * @param inviteId  the invite id
   * @param accountId the account id
   * @return the rest response
   */
  @DELETE
  @Path("invites/{inviteId}")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.USER_PERMISSION_MANAGEMENT)
  public RestResponse<UserInvite> deleteInvite(
      @PathParam("inviteId") @NotEmpty String inviteId, @QueryParam("accountId") @NotEmpty String accountId) {
    return getPublicUserInvite(userService.deleteInvite(accountId, inviteId));
  }

  @POST
  @Path("sso/zendesk")
  public RestResponse<ZendeskSsoLoginResponse> zendDesk(@QueryParam("returnTo") @NotEmpty String returnTo) {
    return new RestResponse(userService.generateZendeskSsoJwt(returnTo));
  }

  /**
   * Delete invite rest response.
   *
   * @param userId  the user id
   * @param accountId the account id
   * @return the rest response
   */
  @GET
  @Path("reset-two-factor-auth/{userId}")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.USER_PERMISSION_MANAGEMENT)
  public RestResponse<Boolean> reset2fa(
      @PathParam("userId") @NotEmpty String userId, @QueryParam("accountId") @NotEmpty String accountId) {
    return new RestResponse<>(twoFactorAuthenticationManager.sendTwoFactorAuthenticationResetEmail(userId));
  }

  private RestResponse<UserInvite> getPublicUserInvite(UserInvite userInvite) {
    if (userInvite == null) {
      return new RestResponse<>();
    }
    setUserGroupSummary(userInvite);
    return new RestResponse<>(userInvite);
  }

  private RestResponse<PageResponse<UserInvite>> getPublicUserInvites(PageResponse<UserInvite> pageResponse) {
    if (pageResponse == null) {
      return new RestResponse<>();
    }

    List<UserInvite> userInvites = pageResponse.getResponse();
    if (isEmpty(userInvites)) {
      return new RestResponse<>(pageResponse);
    }
    userInvites.forEach(userInvite -> setUserGroupSummary(userInvite));
    return new RestResponse<>(pageResponse);
  }

  private RestResponse<List<UserInvite>> getPublicUserInvites(List<UserInvite> userInvites) {
    if (isEmpty(userInvites)) {
      return new RestResponse<>(userInvites);
    }
    userInvites.forEach(userInvite -> setUserGroupSummary(userInvite));
    return new RestResponse<>(userInvites);
  }

  private void setUserGroupSummary(UserInvite userInvite) {
    List<UserGroup> userGroupSummaryList = userGroupService.getUserGroupSummary(userInvite.getUserGroups());
    userInvite.setUserGroups(userGroupSummaryList);
  }

  private RestResponse<User> getPublicUser(User user) {
    if (user == null) {
      return new RestResponse<>();
    }

    List<UserGroup> userGroups = user.getUserGroups();
    user = userService.getUserSummary(user);
    setUserGroupSummary(user, userGroups);
    return new RestResponse<>(user);
  }

  private RestResponse<PageResponse<User>> getPublicUsers(PageResponse<User> pageResponse) {
    if (pageResponse == null) {
      return new RestResponse<>();
    }

    List<User> users = pageResponse.getResponse();
    if (isEmpty(users)) {
      return new RestResponse<>(pageResponse);
    }

    AtomicInteger index = new AtomicInteger(0);
    users.forEach(user -> {
      List<UserGroup> userGroups = user.getUserGroups();
      user = userService.getUserSummary(user);
      setUserGroupSummary(user, userGroups);
      users.set(index.getAndIncrement(), user);
    });
    return new RestResponse<>(pageResponse);
  }

  private void setUserGroupSummary(User user, List<UserGroup> userGroups) {
    if (user == null) {
      return;
    }
    List<UserGroup> userGroupSummaryList = userGroupService.getUserGroupSummary(userGroups);
    user.setUserGroups(userGroupSummaryList);
  }

  private String urlDecode(String encoded) {
    String decoded = encoded;
    try {
      decoded = URLDecoder.decode(encoded, StandardCharsets.UTF_8.name());
    } catch (UnsupportedEncodingException e) {
      // Should not happen and ignore.
    }
    return decoded;
  }

  /**
   * The type Reset password request.
   */
  public static class ResetPasswordRequest {
    private String email;

    /**
     * Gets email.
     *
     * @return the email
     */
    public String getEmail() {
      return email;
    }

    /**
     * Sets email.
     *
     * @param email the email
     */
    public void setEmail(String email) {
      this.email = email;
    }
  }

  /**
   * The type Update password request.
   */
  public static class UpdatePasswordRequest {
    private String password;

    /**
     * Gets password.
     *
     * @return the password
     */
    public String getPassword() {
      return password;
    }

    /**
     * Sets password.
     *
     * @param password the password
     */
    public void setPassword(String password) {
      this.password = password;
    }
  }

  @Data
  public static class ResendInvitationEmailRequest {
    @NotBlank private String email;
  }
}
