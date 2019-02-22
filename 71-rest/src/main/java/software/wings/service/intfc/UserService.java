package software.wings.service.intfc;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.validation.Create;
import io.harness.validation.Update;
import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.NotEmpty;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.Account;
import software.wings.beans.AccountRole;
import software.wings.beans.ApplicationRole;
import software.wings.beans.User;
import software.wings.beans.UserInvite;
import software.wings.beans.ZendeskSsoLoginResponse;
import software.wings.beans.security.UserGroup;
import software.wings.security.SecretManager;
import software.wings.service.intfc.ownership.OwnedByAccount;

import java.net.URISyntaxException;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * Created by anubhaw on 3/28/16.
 */
public interface UserService extends OwnedByAccount {
  /**
   * Register a new user with basic account information. Create the account if that
   * account did not exist.
   *
   * @param user the user
   * @return the user
   */
  @ValidationGroups(Create.class) User register(@Valid User user);

  /**
   * Start the trial registration with an email.
   *
   * @param email the email of the user who is registering for a trial account.
   */
  boolean trialSignup(String email);

  User getUserSummary(User user);

  List<User> getUserSummary(List<User> userList);

  /**
   * Register a new user within an existing account.
   *
   * @param user the user
   * @param account the account the user should be registered to
   * @return the user
   */
  User registerNewUser(User user, Account account);

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
   * Update name of an user
   *
   * @param userId the user id
   * @param name new name
   * @return the user
   */
  @ValidationGroups(Update.class) User updateName(@NotBlank String userId, @NotBlank String name);

  /**
   * Update the user and the associated user groups.
   *
   * @param userId the user id
   * @param userGroups updated user groups
   * @param accountId the account id
   * @param sendNotification send notification flag
   * @return the user
   */
  @ValidationGroups(Update.class)
  User updateUserGroupsOfUser(String userId, List<UserGroup> userGroups, String accountId, boolean sendNotification);

  /**
   * Update the user fullname and the associated user groups.
   *
   * @param userId       user
   * @param userGroups updated user groups
   * @param name       user's name
   * @param accountId the account id
   * @return the user
   */
  @ValidationGroups(Update.class)
  User updateUserGroupsAndNameOfUser(@NotBlank String userId, List<UserGroup> userGroups, @NotBlank String name,
      @NotBlank String accountId, boolean sendNotification);

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
   * @param addUser    whether to add the specified current as account admin to the specified account
   * @return the account
   */
  Account addAccount(Account account, User user, boolean addUser);

  /**
   * Retrieve an user by its email.
   *
   * @param email
   * @return
   */
  User getUserByEmail(String email);

  UserInvite getUserInviteByEmail(String email);

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
   * Resend the invitation email.
   * @param accountId account id
   * @param email     email address
   * @return the boolean
   */
  boolean resendInvitationEmail(@NotNull UserService userService, @NotBlank String accountId, @NotBlank String email);

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

  String getUserInviteUrl(String email, Account account) throws URISyntaxException;

  String getUserInviteUrl(String email) throws URISyntaxException;

  /**
   * Send user invitation email
   *
   * @param userInvite  user invite
   * @param account     account
   */
  void sendNewInvitationMail(UserInvite userInvite, Account account);

  /**
   * Send added new role email
   *
   * @param user        user
   * @param account     account
   */
  void sendAddedGroupEmail(User user, Account account);

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
   * Complete the user invite and login the user in one call.
   *
   * @param userInvite the user invite
   * @return the logged-in user
   */
  User completeInviteAndSignIn(UserInvite userInvite);

  /**
   * Complete the trial user signup. Both the trial account and the account admin user will be created
   * as part of this operation.
   *
   * @param user The user to be signed up for a free trial
   * @param userInvite the user invite.
   * @return the completed user invite
   */
  UserInvite completeTrialSignup(User user, UserInvite userInvite);

  /**
   * Complete the trial user signup and signin. Both the trial account and the account admin user will be created
   * as part of this operation.
   *
   * @param user The user to be signed up for a free trial
   * @param userInvite the user invite.
   * @return the completed user invite
   */
  User completeTrialSignupAndSignIn(User user, UserInvite userInvite);

  /**
   * Delete invite user invite.
   *
   * @param accountId the account id
   * @param inviteId  the invite id
   * @return the user invite
   */
  UserInvite deleteInvite(String accountId, String inviteId);

  /**
   * Delete existing user invites by email
   * @param accountId the account id
   * @param email     the user email
   * @return the boolean
   */
  boolean deleteInvites(@NotBlank String accountId, @NotBlank String email);

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

  boolean isUserVerified(User user);

  List<User> getUsersOfAccount(@NotEmpty String accountId);
}
