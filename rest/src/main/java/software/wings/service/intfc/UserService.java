package software.wings.service.intfc;

import io.harness.validation.Create;
import io.harness.validation.Update;
import org.hibernate.validator.constraints.NotEmpty;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.Account;
import software.wings.beans.AccountRole;
import software.wings.beans.ApplicationRole;
import software.wings.beans.User;
import software.wings.beans.UserInvite;
import software.wings.beans.ZendeskSsoLoginResponse;
import software.wings.beans.security.UserGroup;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.security.SecretManager;

import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * Created by anubhaw on 3/28/16.
 */
public interface UserService {
  /**
   * Register.
   *
   * @param user the user
   * @return the user
   */
  @ValidationGroups(Create.class) User register(@Valid User user);

  /**
   * Match password.
   *
   * @param password the password
   * @param hash     the hash
   * @return true, if successful
   */
  boolean matchPassword(@NotEmpty char[] password, @NotEmpty String hash);

  /**
   * Adds the role.
   *
   * @param userId the user id
   * @param roleId the role id
   * @return the user
   */
  User addRole(@NotEmpty String userId, @NotEmpty String roleId);

  /**
   * Update.
   *
   * @param user the user
   * @return the user
   */
  @ValidationGroups(Update.class) User update(@Valid User user);

  /**
   * Update the user and the associated user groups.
   *
   * @param userId the user id
   * @param userGroups updated user groups
   * @param accountId the account id
   * @return the user
   */
  @ValidationGroups(Update.class)
  User updateUserGroupsOfUser(String userId, List<UserGroup> userGroups, String accountId);

  /**
   * List.
   *
   * @param pageRequest the page request
   * @return the page response
   */
  PageResponse<User> list(PageRequest<User> pageRequest);

  /**
   * Delete.
   *
   * @param accountId the account id
   * @param userId    the user id
   */
  void delete(@NotEmpty String accountId, @NotEmpty String userId);

  /**
   * overrideTwoFactorforAccount
   *
   * @param accountId the account id
   * @param adminOverrideTwoFactorEnabled boolean
   */
  boolean overrideTwoFactorforAccount(String accountId, User user, boolean adminOverrideTwoFactorEnabled);
  boolean isTwoFactorEnabledForAdmin(String accountId, String usedId);

  /**
   * Gets the.
   *
   * @param userId the user id
   * @return the user
   */
  User get(@NotEmpty String userId);

  /**
   * Gets the user and loads the user groups for the given account.
   *
   * @param accountId the account Id
   * @param userId the user id
   * @return the user
   */
  User get(@NotEmpty String accountId, @NotEmpty String userId);

  /**
   * Gets user from cache or db.
   *
   * @param userId the user id
   * @return the user from cache or db
   */
  User getUserFromCacheOrDB(String userId);

  /**
   * Evict user from cache.
   *
   * @param userId the user id
   */
  void evictUserFromCache(String userId);

  /**
   * Revoke role.
   *
   * @param userId the user id
   * @param roleId the role id
   * @return the user
   */
  User revokeRole(@NotEmpty String userId, @NotEmpty String roleId);

  /**
   * Add account account.
   *
   * @param account the account
   * @param user    the user
   * @return the account
   */
  Account addAccount(Account account, User user);

  User registerNewUser(User user, Account account);

  User getUserByEmail(String email);

  /**
   * Verify registered or allowed.
   *
   * @param emailAddress the email address
   */
  void verifyRegisteredOrAllowed(String emailAddress);

  /**
   * Resend verification email boolean.
   *
   * @param email the email
   * @return the boolean
   */
  boolean resendVerificationEmail(String email);

  /**
   * Verify email string.
   *
   * @param token the token
   * @return the string
   */
  boolean verifyToken(String token);

  /**
   * Update stats fetched on.
   *
   * @param user the user
   */
  void updateStatsFetchedOnForUser(User user);

  /**
   * Invite user user invite.
   *
   * @param userInvite the user invite
   * @return the user invite
   */
  List<UserInvite> inviteUsers(UserInvite userInvite);

  /**
   * Invite single user
   *
   * @param userInvite the user invite
   * @return the user invite
   */
  UserInvite inviteUser(UserInvite userInvite);

  /**
   * List invites page response.
   *
   * @param pageRequest the page request
   * @return the page response
   */
  PageResponse<UserInvite> listInvites(PageRequest<UserInvite> pageRequest);

  /**
   * Gets invite.
   *
   * @param accountId the account id
   * @param inviteId  the invite id
   * @return the invite
   */
  UserInvite getInvite(String accountId, String inviteId);

  /**
   * Complete invite user invite.
   *
   * @param userInvite the user invite
   * @return the user invite
   */
  UserInvite completeInvite(UserInvite userInvite);

  /**
   * Delete invite user invite.
   *
   * @param accountId the account id
   * @param inviteId  the invite id
   * @return the user invite
   */
  UserInvite deleteInvite(String accountId, String inviteId);

  /**
   * Gets user account role.
   *
   * @param userId    the user id
   * @param accountId the account id
   * @return the user account role
   */
  AccountRole getUserAccountRole(String userId, String accountId);

  /**
   * Gets user application role.
   *
   * @param userId the user id
   * @param appId  the app id
   * @return the user application role
   */
  ApplicationRole getUserApplicationRole(String userId, String appId);

  /**
   * Reset password boolean.
   *
   * @param emailId the email id
   * @return the boolean
   */
  boolean resetPassword(String emailId);

  /**
   * Update password boolean.
   *
   * @param resetPasswordToken the reset password token
   * @param password           the password
   * @return the boolean
   */
  boolean updatePassword(String resetPasswordToken, char[] password);

  void logout(User user);

  /**
   * Generate zendesk sso jwt zendesk sso login response.
   *
   * @param returnToUrl the return to url
   * @return the zendesk sso login response
   */
  ZendeskSsoLoginResponse generateZendeskSsoJwt(String returnToUrl);

  /**
   *
   * @param userId
   * @return
   */

  String generateJWTToken(@NotEmpty String userId, @NotNull SecretManager.JWT_CATEGORY category);

  /**
   *
   * @param jwtToken
   * @param category
   * @return
   */
  User verifyJWTToken(@NotEmpty String jwtToken, @NotNull SecretManager.JWT_CATEGORY category);

  boolean isUserAssignedToAccount(User user, String accountId);

  List<String> fetchUserEmailAddressesFromUserIds(List<String> userIds);
}
