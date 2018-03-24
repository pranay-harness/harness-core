package software.wings.resources;

import static com.google.common.collect.ImmutableMap.of;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static software.wings.beans.Base.GLOBAL_APP_ID;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.Account;
import software.wings.beans.AccountRole;
import software.wings.beans.ApplicationRole;
import software.wings.beans.ErrorCode;
import software.wings.beans.RestResponse;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.User;
import software.wings.beans.UserInvite;
import software.wings.beans.ZendeskSsoLoginResponse;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.exception.WingsException;
import software.wings.exception.WingsException.ReportTarget;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.UserPermissionInfo;
import software.wings.security.UserThreadLocal;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.PublicApi;
import software.wings.security.annotations.Scope;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AuthService;
import software.wings.service.intfc.UserService;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

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

  /**
   * Instantiates a new User resource.
   *
   * @param userService    the user service
   * @param accountService the account service
   */
  @Inject
  public UserResource(UserService userService, AuthService authService, AccountService accountService) {
    this.userService = userService;
    this.authService = authService;
    this.accountService = accountService;
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
  public RestResponse<PageResponse<User>> list(
      @BeanParam PageRequest<User> pageRequest, @QueryParam("accountId") @NotEmpty String accountId) {
    Account account = accountService.get(accountId);
    pageRequest.addFilter("accounts", Operator.HAS, account);
    PageResponse<User> pageResponse = userService.list(pageRequest);
    if (isNotEmpty(pageResponse)) {
      pageResponse.forEach(user -> {
        int i = 0;
        while (i < user.getAccounts().size()) {
          if (!accountId.equals(user.getAccounts().get(i).getUuid())) {
            user.getAccounts().remove(i);
          } else {
            i++;
          }
        }
        i = 0;
        while (i < user.getRoles().size()) {
          if (!accountId.equals(user.getRoles().get(i).getAccountId())) {
            user.getRoles().remove(i);
          } else {
            i++;
          }
        }
      });
    }
    return new RestResponse<>(pageResponse);
  }

  /**
   * Register.
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

  @POST
  @Path("account")
  @Timed
  @ExceptionMetered
  @Scope(value = ResourceType.USER, scope = PermissionType.LOGGED_IN)
  public RestResponse<Account> addAccount(Account account) {
    User existingUser = UserThreadLocal.get();
    if (existingUser == null) {
      throw new WingsException(ErrorCode.INVALID_REQUEST).addParam("message", "Invalid User");
    }
    return new RestResponse<>(userService.addAccount(account, existingUser));
  }

  /**
   * Update.
   *
   * @param userId the user id
   * @param user   the user
   * @return the rest response
   */
  @PUT
  @Path("{userId}")
  @Scope(value = ResourceType.USER, scope = PermissionType.LOGGED_IN)
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.USER_PERMISSION_MANAGEMENT)
  public RestResponse<User> update(@PathParam("userId") String userId, User user) {
    User authUser = UserThreadLocal.get();
    if (!authUser.getUuid().equals(userId)) {
      throw new WingsException(ErrorCode.ACCESS_DENIED);
    }
    user.setUuid(userId);
    return new RestResponse<>(userService.update(user));
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
   * Login.
   *
   * @return the rest response
   */
  @GET
  @Path("login")
  @PublicApi
  @Timed
  @ExceptionMetered
  public RestResponse<User> login() {
    User user = UserThreadLocal.get();
    return new RestResponse<>(user);
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
    userService.logout(userId);
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
    } catch (WingsException e) {
      return RestResponse.Builder.aRestResponse()
          .withResource(false)
          .withResponseMessages(e.getResponseMessageList(ReportTarget.USER))
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
    return new RestResponse<>(userService.addRole(userId, roleId));
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
    return new RestResponse<>(userService.revokeRole(userId, roleId));
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
    return new RestResponse<>(userService.listInvites(pageRequest));
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
    return new RestResponse<>(userService.getInvite(accountId, inviteId));
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
    return new RestResponse<>(userService.inviteUsers(userInvite));
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
    return new RestResponse<>(userService.completeInvite(userInvite));
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
    return new RestResponse<>(userService.deleteInvite(accountId, inviteId));
  }

  @POST
  @Path("sso/zendesk")
  public RestResponse<ZendeskSsoLoginResponse> zendDesk(@QueryParam("returnTo") @NotEmpty String returnTo) {
    return new RestResponse(userService.generateZendeskSsoJwt(returnTo));
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
}
