package software.wings.service.intfc;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.event.model.EventType;
import io.harness.validation.Create;
import io.harness.validation.Update;
import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.query.UpdateOperations;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.Account;
import software.wings.beans.AccountJoinRequest;
import software.wings.beans.AccountRole;
import software.wings.beans.ApplicationRole;
import software.wings.beans.LicenseInfo;
import software.wings.beans.User;
import software.wings.beans.UserInvite;
import software.wings.beans.ZendeskSsoLoginResponse;
import software.wings.beans.loginSettings.PasswordSource;
import software.wings.beans.loginSettings.PasswordStrengthViolations;
import software.wings.beans.marketplace.MarketPlaceType;
import software.wings.beans.security.UserGroup;
import software.wings.security.SecretManager;
import software.wings.security.UserPermissionInfo;
import software.wings.security.authentication.AuthenticationMechanism;
import software.wings.security.authentication.LogoutResponse;
import software.wings.security.authentication.TwoFactorAuthenticationSettings;
import software.wings.security.authentication.oauth.OauthUserInfo;
import software.wings.service.intfc.ownership.OwnedByAccount;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * Created by anubhaw on 3/28/16.
 */
public interface UserService extends OwnedByAccount {
  /**
   * Consider the following characters in email as illegal and prohibit trial signup with the following characters
   */
  List<Character> ILLEGAL_CHARACTERS = Collections.unmodifiableList(Arrays.asList(
      '$', '&', ',', '/', ':', ';', '=', '?', '<', '>', '#', '{', '}', '|', '^', '~', '(', ')', ']', '`', '\'', '\"'));

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

  /**
   * Start the trial registration with an user invite.
   *
   * @param userInvite user invite with registration info
   */
  boolean trialSignup(UserInvite userInvite);

  UserInvite createUserInviteForMarketPlace();

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
   * List.
   *
   * @param pageRequest the page request
   * @param loadUserGroups if load user groups is needed
   * @return the page response
   */
  PageResponse<User> list(PageRequest<User> pageRequest, boolean loadUserGroups);

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
  boolean overrideTwoFactorforAccount(String accountId, boolean adminOverrideTwoFactorEnabled);
  boolean isTwoFactorEnabled(String accountId, String usedId);

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

  User getUserByEmail(String email, String accountId);

  UserInvite getUserInviteByEmailAndAccount(String email, String accountId);

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

  String getUserInviteUrl(UserInvite userInvite, Account account) throws URISyntaxException;

  String getUserInviteUrl(UserInvite userInvite) throws URISyntaxException;

  void sendVerificationEmail(UserInvite userInvite, String url, Map<String, String> params);

  /**
   * Send user invitation email
   *
   * @param userInvite  user invite
   * @param account     account
   */
  void sendNewInvitationMail(UserInvite userInvite, Account account, User user);

  /**
   * Send login invitation email
   *
   * @param userInvite  user invite
   * @param account     account
   */
  void sendUserInvitationToOnlySsoAccountMail(UserInvite userInvite, Account account, User user);

  /**
   * Send added new role email
   *
   * @param user        user
   * @param account     account
   */
  void sendAddedGroupEmail(User user, Account account, List<UserGroup> userGroups);

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
   * @param licenseInfo
   * @return the completed user invite
   */
  UserInvite completeSignup(User user, UserInvite userInvite, LicenseInfo licenseInfo);

  User completeMarketPlaceSignup(User user, UserInvite userInvite, MarketPlaceType marketPlaceType);

  /**
   * Complete the trial user signup using oauth. Both the trial account and the account admin user will be created
   * as part of this operation. Also a default SSO settings corresponding to the identity provider will be created
   * and will set up as the default login mechanism.
   *
   * @param userInfo The user to be signed up for a free trial
   * @param oauthProviderName The oauthClient being used for the signup process.
   * @return the new User
   */
  User signUpUserUsingOauth(OauthUserInfo userInfo, String oauthProviderName);

  /**
   * Complete the trial user signup and signin. Both the trial account and the account admin user will be created
   * as part of this operation.
   *
   * @param userInviteId the user invite id.
   * @return the completed user invite
   */
  User completeTrialSignupAndSignIn(String userInviteId);

  User completeTrialSignupAndSignIn(UserInvite userInvite);

  User completePaidSignupAndSignIn(UserInvite userInvite);

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

  LogoutResponse logout(String accountId, String userId);

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
   * @param claims Map of claims
   * @return
   */

  String generateJWTToken(Map<String, String> claims, @NotNull SecretManager.JWT_CATEGORY category);

  /**
   *
   * @param jwtToken
   * @param category
   * @return
   */
  User verifyJWTToken(@NotEmpty String jwtToken, @NotNull SecretManager.JWT_CATEGORY category);

  boolean isAccountAdmin(String accountId);

  boolean isAccountAdmin(User user, String accountId);

  boolean isUserAccountAdmin(@NotNull UserPermissionInfo userPermissionInfo, @NotNull String accountId);

  boolean isUserAssignedToAccount(User user, String accountId);

  boolean isUserVerified(User user);

  List<User> getUsersOfAccount(@NotEmpty String accountId);

  List<User> getUsersWithThisAsPrimaryAccount(@NotEmpty String accountId);

  AuthenticationMechanism getAuthenticationMechanism(User user);

  /**
   * Set the default account for a user. It means the user will land on the default account next time logging in.
   */
  boolean setDefaultAccount(User user, @NotEmpty String accountId);

  boolean updateLead(String email, String accountId);

  boolean deleteUsersByEmailAddress(String accountId, List<String> usersToRetain);

  /**
   * Disable or Enable an user administratively. Once disabled, the user can no longer login.
   */
  boolean enableUser(String accountId, String userId, boolean enabled);

  boolean passwordExpired(String email);

  void sendPasswordExpirationWarning(String email, Integer passExpirationDays);

  String createSignupSecretToken(String email, Integer passExpirationDays);

  void sendPasswordExpirationMail(String email);

  @ValidationGroups(Update.class) User updateUserProfile(@NotNull User user);

  @ValidationGroups(Update.class)
  User addEventToUserMarketoCampaigns(@NotNull String userId, @NotNull EventType eventType);

  @ValidationGroups(Update.class)
  User updateTwoFactorAuthenticationSettings(@NotNull User user, @NotNull TwoFactorAuthenticationSettings settings);

  /**
   *
   */
  boolean accountJoinRequest(AccountJoinRequest accountJoinRequest);

  //  boolean postCustomEvent(String accountId, String event, String additionalInfo);

  boolean postCustomEvent(String accountId, String event);

  PasswordStrengthViolations checkPasswordViolations(
      String resetPasswordToken, PasswordSource passwordSource, String password);

  User applyUpdateOperations(User user, UpdateOperations<User> updateOperations);

  User unlockUser(String userId, String accountId);

  void sendAccountLockedNotificationMail(User user, int lockoutExpirationTime);

  Account getAccountByIdIfExistsElseGetDefaultAccount(User user, Optional<String> accountId);

  boolean canEnableOrDisable(User user);

  User save(User user, String accountId);

  String saveUserInvite(UserInvite userInvite);
}
