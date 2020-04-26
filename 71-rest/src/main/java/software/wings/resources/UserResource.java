package software.wings.resources;

import static com.google.common.collect.ImmutableMap.of;
import static io.harness.beans.PageRequest.DEFAULT_PAGE_SIZE;
import static io.harness.beans.PageResponse.PageResponseBuilder.aPageResponse;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.ReportTarget.REST_API;
import static io.harness.exception.WingsException.USER;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.security.PermissionAttribute.PermissionType.ACCOUNT_MANAGEMENT;
import static software.wings.security.PermissionAttribute.PermissionType.LOGGED_IN;
import static software.wings.security.PermissionAttribute.PermissionType.USER_PERMISSION_MANAGEMENT;
import static software.wings.security.PermissionAttribute.PermissionType.USER_PERMISSION_READ;
import static software.wings.utils.Utils.urlDecode;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.configuration.DeployMode;
import io.harness.eraro.ErrorCode;
import io.harness.eraro.ResponseMessage;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.logging.AutoLogContext;
import io.harness.logging.ExceptionLogger;
import io.harness.rest.RestResponse;
import io.harness.rest.RestResponse.Builder;
import io.swagger.annotations.Api;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.app.MainConfiguration;
import software.wings.beans.Account;
import software.wings.beans.AccountJoinRequest;
import software.wings.beans.AccountRole;
import software.wings.beans.ApplicationRole;
import software.wings.beans.FeatureFlag;
import software.wings.beans.LoginRequest;
import software.wings.beans.LoginTypeRequest;
import software.wings.beans.PublicUser;
import software.wings.beans.User;
import software.wings.beans.UserInvite;
import software.wings.beans.ZendeskSsoLoginResponse;
import software.wings.beans.loginSettings.PasswordSource;
import software.wings.beans.marketplace.MarketPlaceType;
import software.wings.beans.security.UserGroup;
import software.wings.scheduler.AccountPasswordExpirationJob;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.UserPermissionInfo;
import software.wings.security.UserThreadLocal;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.IdentityServiceAuth;
import software.wings.security.annotations.PublicApi;
import software.wings.security.annotations.Scope;
import software.wings.security.authentication.AuthenticationManager;
import software.wings.security.authentication.LoginTypeResponse;
import software.wings.security.authentication.SsoRedirectRequest;
import software.wings.security.authentication.TwoFactorAdminOverrideSettings;
import software.wings.security.authentication.TwoFactorAuthenticationManager;
import software.wings.security.authentication.TwoFactorAuthenticationMechanism;
import software.wings.security.authentication.TwoFactorAuthenticationSettings;
import software.wings.service.impl.InviteOperationResponse;
import software.wings.service.impl.MarketplaceTypeLogContext;
import software.wings.service.impl.ReCaptchaVerifier;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AuthService;
import software.wings.service.intfc.HarnessUserGroupService;
import software.wings.service.intfc.UserGroupService;
import software.wings.service.intfc.UserService;
import software.wings.utils.AccountPermissionUtils;
import software.wings.utils.CacheManager;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;
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
@AuthRule(permissionType = LOGGED_IN)
@Slf4j
public class UserResource {
  private UserService userService;
  private AuthService authService;
  private AccountService accountService;
  private AuthenticationManager authenticationManager;
  private TwoFactorAuthenticationManager twoFactorAuthenticationManager;
  private CacheManager cacheManager;
  private HarnessUserGroupService harnessUserGroupService;
  private UserGroupService userGroupService;
  private AccountPermissionUtils accountPermissionUtils;
  private MainConfiguration mainConfiguration;
  private AccountPasswordExpirationJob accountPasswordExpirationJob;
  private ReCaptchaVerifier reCaptchaVerifier;
  private static final String BASIC = "Basic";

  @Inject
  public UserResource(UserService userService, AuthService authService, AccountService accountService,
      AccountPermissionUtils accountPermissionUtils, AuthenticationManager authenticationManager,
      TwoFactorAuthenticationManager twoFactorAuthenticationManager, CacheManager cacheManager,
      HarnessUserGroupService harnessUserGroupService, UserGroupService userGroupService,
      MainConfiguration mainConfiguration, AccountPasswordExpirationJob accountPasswordExpirationJob,
      ReCaptchaVerifier reCaptchaVerifier) {
    this.userService = userService;
    this.authService = authService;
    this.accountService = accountService;
    this.accountPermissionUtils = accountPermissionUtils;
    this.authenticationManager = authenticationManager;
    this.twoFactorAuthenticationManager = twoFactorAuthenticationManager;
    this.cacheManager = cacheManager;
    this.harnessUserGroupService = harnessUserGroupService;
    this.userGroupService = userGroupService;
    this.mainConfiguration = mainConfiguration;
    this.accountPasswordExpirationJob = accountPasswordExpirationJob;
    this.reCaptchaVerifier = reCaptchaVerifier;
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
  @AuthRule(permissionType = USER_PERMISSION_READ)
  public RestResponse<PageResponse<PublicUser>> list(@BeanParam PageRequest<User> pageRequest,
      @QueryParam("accountId") @NotEmpty String accountId,
      @QueryParam("details") @DefaultValue("true") boolean loadUserGroups) {
    Integer offset = Integer.valueOf(pageRequest.getOffset());
    Integer pageSize = Math.min(DEFAULT_PAGE_SIZE, pageRequest.getPageSize());
    List<User> userList = userService.listUsers(accountId, loadUserGroups, pageSize, offset, true);

    PageResponse<PublicUser> pageResponse = aPageResponse()
                                                .withOffset(offset.toString())
                                                .withLimit(pageSize.toString())
                                                .withResponse(getPublicUsers(userList, accountId))
                                                .withTotal(userService.getTotalUserCount(accountId, true))
                                                .build();

    return new RestResponse<>(pageResponse);
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
   *  Start the trial registration with email and user info.
   *  A verification email will be sent to the specified email address.
   *  On successful verification, it creates the account and registers the user.
   *
   * @param userInvite user invite with email and user info
   */
  @PublicApi
  @POST
  @Path("new-trial")
  @Timed
  @ExceptionMetered
  public RestResponse<Boolean> trialSignup(UserInvite userInvite) {
    return new RestResponse<>(userService.trialSignup(userInvite));
  }

  /**
   *  Start the trial registration with email and user info.
   *  A verification email will be sent to the specified email address.
   *  On successful verification, it creates the account and registers the user.
   *
   * @param accountJoinRequest user invite with email and user info
   */
  @PublicApi
  @POST
  @Path("join-account")
  @Timed
  @ExceptionMetered
  public RestResponse<Boolean> accountJoinRequest(AccountJoinRequest accountJoinRequest) {
    return new RestResponse<>(userService.accountJoinRequest(accountJoinRequest));
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
  @AuthRule(permissionType = USER_PERMISSION_MANAGEMENT)
  public RestResponse<User> updateUserGroupsOfUser(
      @QueryParam("accountId") @NotEmpty String accountId, @PathParam("userId") String userId, User user) {
    return getPublicUser(userService.updateUserGroupsOfUser(userId, user.getUserGroups(), accountId, true));
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
    return new RestResponse<>(userService.updateUserProfile(user));
  }

  /**
   * Reset all caches.
   *
   * @return the rest response
   */
  @PUT
  @Path("reset-cache")
  @Scope(value = ResourceType.USER, scope = LOGGED_IN)
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = LOGGED_IN)
  public RestResponse resetCache() {
    User authUser = UserThreadLocal.get();
    if (harnessUserGroupService.isHarnessSupportUser(authUser.getUuid())) {
      cacheManager.resetAllCaches();
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
  @AuthRule(permissionType = USER_PERMISSION_MANAGEMENT)
  public RestResponse delete(@QueryParam("accountId") @NotEmpty String accountId, @PathParam("userId") String userId) {
    User user = userService.get(userId);
    // If user doesn't exists, userService.get throws exception, so we don't have to handle it.
    if (user.isImported()) {
      throw new InvalidRequestException("Can not delete user added via SCIM", USER);
    }
    userService.delete(accountId, userId);
    return new RestResponse();
  }

  @DELETE
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = USER_PERMISSION_MANAGEMENT)
  public RestResponse<Boolean> delete(
      @QueryParam("accountId") @NotEmpty String accountId, @NotEmpty List<String> usersToRetain) {
    if (CollectionUtils.isEmpty(usersToRetain)) {
      throw new InvalidRequestException("All users in the account can not be deleted");
    }
    return new RestResponse<>(userService.deleteUsersByEmailAddress(accountId, usersToRetain));
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
  @AuthRule(permissionType = USER_PERMISSION_MANAGEMENT)
  public RestResponse resendInvitationEmail(@QueryParam("accountId") @NotBlank String accountId,
      @Valid @NotNull ResendInvitationEmailRequest invitationEmailRequest) {
    return new RestResponse<>(userService.resendInvitationEmail(accountId, invitationEmailRequest.getEmail()));
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

  @PublicApi
  @GET
  @Path("check-password-violations")
  @Timed
  @ExceptionMetered
  public RestResponse checkPasswordViolations(@NotEmpty @QueryParam("token") String token,
      @QueryParam("pollType") PasswordSource passwordSource, @HeaderParam(HttpHeaders.AUTHORIZATION) String password) {
    return new RestResponse(userService.checkPasswordViolations(token, passwordSource, password));
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
  @Scope(value = ResourceType.USER, scope = LOGGED_IN)
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = LOGGED_IN)
  public RestResponse<User> get() {
    return new RestResponse<>(UserThreadLocal.get().getPublicUser());
  }

  /**
   * Look up the user object using email and login the user. Intended for internal use only.
   * E.g. The Identity Service authenticated the user through OAuth provider and get the user email, then
   * login this user through this API directly.
   *
   * @return the rest response
   */
  @GET
  @Path("user/login")
  @Timed
  @ExceptionMetered
  @IdentityServiceAuth
  public RestResponse<User> loginUser(@QueryParam("email") String email) {
    return new RestResponse<>(authenticationManager.loginUserForIdentityService(urlDecode(email)));
  }

  /**
   * Get rest response.
   *
   * @param accountId the account id
   * @return the rest response
   */
  @GET
  @Path("account-roles/{accountId}")
  @Scope(value = ResourceType.USER, scope = LOGGED_IN)
  @Timed
  @ExceptionMetered
  public RestResponse<AccountRole> getAccountRole(@PathParam("accountId") String accountId) {
    return new RestResponse<>(
        userService.getUserAccountRole(UserThreadLocal.get().getPublicUser().getUuid(), accountId));
  }

  @GET
  @Path("user-permissions/{accountId}")
  @Scope(value = ResourceType.USER, scope = LOGGED_IN)
  @Timed
  @ExceptionMetered
  public RestResponse<UserPermissionInfo> getUserPermissionInfo(@PathParam("accountId") String accountId) {
    return new RestResponse<>(
        authService.getUserPermissionInfo(accountId, UserThreadLocal.get().getPublicUser(), false));
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
  @AuthRule(permissionType = LOGGED_IN)
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
  @Scope(value = ResourceType.USER, scope = LOGGED_IN)
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
  @Timed
  @ExceptionMetered
  public RestResponse<User> switchAccount(
      @HeaderParam(HttpHeaders.AUTHORIZATION) String authorization, @QueryParam("accountId") String accountId) {
    return new RestResponse<>(
        authenticationManager.switchAccount(authenticationManager.extractToken(authorization, "Bearer"), accountId));
  }

  /**
   * Explicitly set default account for a logged in user. This means the user will be landed in the default account
   * after logged in next time.
   */
  @PUT
  @Path("set-default-account/{accountId}")
  @Scope(value = ResourceType.USER, scope = LOGGED_IN)
  @Timed
  @ExceptionMetered
  public RestResponse<Boolean> setDefaultAccountForCurrentUser(@PathParam("accountId") @NotBlank String accountId) {
    User currentUser = UserThreadLocal.get();
    if (currentUser == null) {
      throw new InvalidRequestException("Invalid User");
    }
    return new RestResponse<>(userService.setDefaultAccount(currentUser, accountId));
  }

  /**
   * Login a user through basic auth.
   *
   * If accountId is specified, it will authenticate using the specified account's auth mechanism. Otherwise
   * it will authenticate using the user's default/primary account's auth mechanism.
   *
   * @return the rest response
   */
  @POST
  @Path("login")
  @PublicApi
  @Timed
  @ExceptionMetered
  public RestResponse<User> login(LoginRequest loginBody, @QueryParam("accountId") String accountId,
      @QueryParam("captcha") @Nullable String captchaToken) {
    if (!StringUtils.isEmpty(captchaToken)) {
      reCaptchaVerifier.verify(captchaToken);
    }

    // accountId field is optional, it could be null.
    return new RestResponse<>(authenticationManager.defaultLoginAccount(
        authenticationManager.extractToken(loginBody.getAuthorization(), BASIC), accountId));
  }

  /**
   * Harness local login.
   * To be used in case of lockout scenarios, if the default login mechanism is 3rd party provider.
   * Only users having account management permissions will be able to login using this.
   *
   * @return the rest response
   */

  @POST
  @Path("harness-local-login")
  @PublicApi
  @Timed
  @ExceptionMetered
  public RestResponse<User> forceLoginUsingHarnessPassword(LoginRequest loginBody) {
    if (!DeployMode.isOnPrem(mainConfiguration.getDeployMode().name())) {
      throw new InvalidRequestException("Invalid Login mechanism");
    }

    return new RestResponse<>(authenticationManager.loginUsingHarnessPassword(
        authenticationManager.extractToken(loginBody.getAuthorization(), BASIC)));
  }

  @POST
  @Path("two-factor-login")
  @PublicApi
  @Timed
  @ExceptionMetered
  public RestResponse<User> twoFactorLogin(@NotNull LoginRequest loginRequest) {
    return new RestResponse<>(twoFactorAuthenticationManager.authenticate(
        authenticationManager.extractToken(loginRequest.getAuthorization(), "JWT")));
  }

  /**
   * Return the specified user's login types (including auth mechanism and redirect request if SSO).
   *
   * If accountId is specified, it will return using the specified account's login type. Otherwise
   * it will authenticate using the user's default/primary account's auth mechanism.
   *
   * @return the rest response
   */
  @POST
  @Path("logintype")
  @PublicApi
  public RestResponse<LoginTypeResponse> getLoginType(
      @NotNull LoginTypeRequest loginTypeRequest, @QueryParam("accountId") String accountId) {
    return new RestResponse<>(authenticationManager.getLoginTypeResponse(loginTypeRequest.getUserName(), accountId));
  }

  @GET
  @Path("onprem-logintype")
  @PublicApi
  @Timed
  @ExceptionMetered
  public RestResponse<LoginTypeResponse> getLoginTypeForOnPremSetup() {
    return new RestResponse<>(authenticationManager.getLoginTypeResponseForOnPrem());
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
  @AuthRule(permissionType = ACCOUNT_MANAGEMENT)
  public RestResponse<TwoFactorAdminOverrideSettings> setTwoFactorAuthAtAccountLevel(
      @PathParam("accountId") @NotEmpty String accountId, @NotNull TwoFactorAdminOverrideSettings settings) {
    // Trying Override = true
    if (settings.isAdminOverrideTwoFactorEnabled()) {
      if (twoFactorAuthenticationManager.isTwoFactorEnabled(accountId, UserThreadLocal.get())) {
        return new RestResponse(twoFactorAuthenticationManager.overrideTwoFactorAuthentication(accountId, settings));
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
      return new RestResponse(twoFactorAuthenticationManager.overrideTwoFactorAuthentication(accountId, settings));
    }
  }

  @PUT
  @Path("disable-two-factor-auth/{accountId}")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = ACCOUNT_MANAGEMENT)
  public RestResponse<Boolean> disableTwoFactorAuth(@PathParam("accountId") @NotEmpty String accountId) {
    return new RestResponse<>(twoFactorAuthenticationManager.disableTwoFactorAuthentication(accountId));
  }

  @PUT
  @Path("disable/{userId}")
  @Timed
  @ExceptionMetered
  public RestResponse<Boolean> disableUser(
      @PathParam("userId") @NotEmpty String userId, @QueryParam("accountId") @NotEmpty String accountId) {
    // Only if the user the account administrator or in the Harness user group can perform the export operation.
    if (!userService.isAccountAdmin(accountId)) {
      String errorMessage = "User is not account administrator and can't perform the export operation.";
      RestResponse<Boolean> restResponse = accountPermissionUtils.checkIfHarnessUser(errorMessage);
      if (restResponse != null) {
        throw new WingsException(ErrorCode.USER_NOT_AUTHORIZED, USER);
      }
    }
    return new RestResponse<>(userService.enableUser(accountId, userId, false));
  }

  @PUT
  @Path("enable/{userId}")
  @Timed
  @ExceptionMetered
  public RestResponse<Boolean> enableUser(
      @PathParam("userId") @NotEmpty String userId, @QueryParam("accountId") @NotEmpty String accountId) {
    // Only if the user the account administrator or in the Harness user group can perform the export operation.
    if (!userService.isAccountAdmin(accountId)) {
      String errorMessage = "User is not account administrator and can't perform the export operation.";
      RestResponse<Boolean> restResponse = accountPermissionUtils.checkIfHarnessUser(errorMessage);
      if (restResponse != null) {
        throw new WingsException(ErrorCode.USER_NOT_AUTHORIZED, USER);
      }
    }
    return new RestResponse<>(userService.enableUser(accountId, userId, true));
  }

  @GET
  @Path("two-factor-auth-info/{accountId}")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = LOGGED_IN)
  public RestResponse<Boolean> getTwoFactorAuthAdminEnforceInfo(@PathParam("accountId") @NotEmpty String accountId) {
    return new RestResponse(twoFactorAuthenticationManager.getTwoFactorAuthAdminEnforceInfo(accountId));
  }

  @PUT
  @Path("disable-two-factor-auth")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = LOGGED_IN)
  public RestResponse<User> disableTwoFactorAuth() {
    return new RestResponse(twoFactorAuthenticationManager.disableTwoFactorAuthentication(UserThreadLocal.get()));
  }

  @PUT
  @Path("enable-two-factor-auth")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = LOGGED_IN)
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
  @Scope(value = ResourceType.USER, scope = LOGGED_IN)
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = LOGGED_IN, skipAuth = true)
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
  @AuthRule(permissionType = USER_PERMISSION_MANAGEMENT)
  public RestResponse<PageResponse<UserInvite>> listInvites(
      @QueryParam("accountId") @NotEmpty String accountId, @BeanParam PageRequest<UserInvite> pageRequest) {
    return getPublicUserInvites(userService.listInvites(pageRequest));
  }

  /**
   * List invites rest response.
   */
  @POST
  @Path("trigger-account-password-expiration-job")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = USER_PERMISSION_MANAGEMENT)
  public RestResponse<String> triggerAccountPasswordExpirationCheckJob() {
    accountPermissionUtils.checkIfHarnessUser("User not authorized");
    accountPasswordExpirationJob.checkAccountPasswordExpiration();
    return new RestResponse<>("Running password expiration cron job");
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
  @AuthRule(permissionType = USER_PERMISSION_MANAGEMENT)
  public RestResponse<List<InviteOperationResponse>> inviteUsers(
      @QueryParam("accountId") @NotEmpty String accountId, @NotNull UserInvite userInvite) {
    userInvite.setAccountId(accountId);
    userInvite.setAppId(GLOBAL_APP_ID);
    return new RestResponse<>(userService.inviteUsers(userInvite));
  }

  /**
   * Checks the status of the invite and either redirects on the login page with corresponding message or to the
   * password sign-up page.
   * @param accountId  the account id
   * @param inviteId   the invite id
   * @return the rest response
   */
  @PublicApi
  @GET
  @Path("invites/{inviteId}/status")
  @Timed
  @ExceptionMetered
  public RestResponse<InviteOperationResponse> checkInvite(
      @QueryParam("accountId") @NotEmpty String accountId, @PathParam("inviteId") @NotEmpty String inviteId) {
    UserInvite userInvite = new UserInvite();
    userInvite.setAccountId(accountId);
    userInvite.setUuid(inviteId);
    try {
      return new RestResponse<>(userService.checkInviteStatus(userInvite));
    } catch (Exception e) {
      logger.error("error checking invite", e);
      return new RestResponse<>(InviteOperationResponse.FAIL);
    }
  }

  @POST
  @Path("custom-event")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = LOGGED_IN)
  public RestResponse<Boolean> postCustomEvent(
      @QueryParam("accountId") @NotEmpty String accountId, @NotEmpty String event) {
    return new RestResponse<>(userService.postCustomEvent(accountId, event));
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
  public RestResponse<InviteOperationResponse> completeInvite(@QueryParam("accountId") @NotEmpty String accountId,
      @PathParam("inviteId") @NotEmpty String inviteId, @NotNull UserInvite userInvite) {
    userInvite.setAccountId(accountId);
    userInvite.setUuid(inviteId);
    return new RestResponse<>(userService.completeInvite(userInvite));
  }

  /**
   * Complete invite rest response.
   *
   * @param accountName the account name
   * @param inviteId   the invite id
   * @param userInvite the user invite
   * @return the rest response
   */
  @PublicApi
  @PUT
  @Path("invites/mktplace/{inviteId}/signin")
  @Timed
  @ExceptionMetered
  public RestResponse<User> completeMarketPlaceInvite(@QueryParam("account") @NotEmpty String accountName,
      @QueryParam("company") @NotEmpty String companyName,
      @QueryParam("marketPlaceType") MarketPlaceType marketPlaceType, @PathParam("inviteId") @NotEmpty String inviteId,
      @NotNull UserInvite userInvite) {
    try (AutoLogContext ignore1 = new MarketplaceTypeLogContext(marketPlaceType, OVERRIDE_ERROR)) {
      // only AWS / GCP marketplaces are supported
      logger.info("Marketplace Signup. marketPlaceType= {}", marketPlaceType);

      try {
        companyName = URLDecoder.decode(companyName, StandardCharsets.UTF_8.displayName());
        accountName = URLDecoder.decode(accountName, StandardCharsets.UTF_8.displayName());
      } catch (UnsupportedEncodingException e) {
        logger.info("Account Name and Company Name must be UTF-8 compliant. accountName: {} companyName: {} Err: {}",
            accountName, companyName, e.getMessage());
        throw new WingsException(ErrorCode.INVALID_ARGUMENT, "Account Name and Company Name must be UTF-8 compliant.");
      }

      userInvite.setUuid(inviteId);
      User user = User.Builder.anUser()
                      .email(userInvite.getEmail())
                      .name(userInvite.getName())
                      .password(userInvite.getPassword())
                      .accountName(accountName)
                      .companyName(companyName)
                      .build();

      User savedUser = userService.completeMarketPlaceSignup(user, userInvite, marketPlaceType);
      return new RestResponse<>(savedUser);
    }
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
  @Path("invites/trial/{inviteId}/new-signin")
  @Timed
  @ExceptionMetered
  public RestResponse<User> completeTrialSignupAndSignIn(@PathParam("inviteId") @NotEmpty String inviteId) {
    return new RestResponse<>(userService.completeTrialSignupAndSignIn(inviteId));
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
  @AuthRule(permissionType = USER_PERMISSION_MANAGEMENT)
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
  @AuthRule(permissionType = USER_PERMISSION_MANAGEMENT)
  public RestResponse<Boolean> reset2fa(
      @PathParam("userId") @NotEmpty String userId, @QueryParam("accountId") @NotEmpty String accountId) {
    return new RestResponse<>(twoFactorAuthenticationManager.sendTwoFactorAuthenticationResetEmail(userId));
  }

  @PUT
  @Path("lead-update")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = LOGGED_IN)
  public RestResponse<Boolean> updateMarketoForUser(
      @NotEmpty @QueryParam("email") String email, @NotEmpty @QueryParam("accountId") String accountId) {
    return new RestResponse<>(userService.updateLead(email, accountId));
  }

  @PUT
  @Path("unlock-user")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = USER_PERMISSION_MANAGEMENT)
  public RestResponse<User> unlockUser(
      @NotEmpty @QueryParam("email") String email, @NotEmpty @QueryParam("accountId") String accountId) {
    return new RestResponse<>(userService.unlockUser(email, accountId));
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
    userInvites.forEach(this ::setUserGroupSummary);
    return new RestResponse<>(pageResponse);
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

  private List<PublicUser> getPublicUsers(List<User> users, String accountId) {
    if (isEmpty(users)) {
      return Collections.emptyList();
    }
    List<PublicUser> publicUserList = new ArrayList<>();
    AtomicInteger index = new AtomicInteger(0);
    users.forEach(user -> {
      List<UserGroup> userGroups = user.getUserGroups();
      boolean inviteAccepted = checkIfInvitationIsAccepted(user, accountId);
      if (!inviteAccepted) {
        maskUserNameWithPendingInvitation(user);
      }
      user = userService.getUserSummary(user);
      setUserGroupSummary(user, userGroups);
      users.set(index.getAndIncrement(), user);
      publicUserList.add(PublicUser.builder().user(user).inviteAccepted(inviteAccepted).build());
    });
    return publicUserList;
  }

  private void maskUserNameWithPendingInvitation(User user) {
    user.setName("User with pending invitation");
  }

  private boolean checkIfInvitationIsAccepted(User user, String accountId) {
    List<Account> accounts = user.getAccounts();
    if (!(isNotEmpty(accounts) && accounts.contains(accountService.get(accountId)))) {
      return false;
    }
    return true;
  }

  private void setUserGroupSummary(User user, List<UserGroup> userGroups) {
    if (user == null) {
      return;
    }
    List<UserGroup> userGroupSummaryList = userGroupService.getUserGroupSummary(userGroups);
    user.setUserGroups(userGroupSummaryList);
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
