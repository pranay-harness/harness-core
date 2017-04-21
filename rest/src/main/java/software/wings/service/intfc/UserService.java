package software.wings.service.intfc;

import org.hibernate.validator.constraints.NotEmpty;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.AccountRole;
import software.wings.beans.ApplicationRole;
import software.wings.beans.User;
import software.wings.beans.UserInvite;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.utils.validation.Create;
import software.wings.utils.validation.Update;

import java.util.List;
import javax.validation.Valid;

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
  boolean matchPassword(@NotEmpty String password, @NotEmpty String hash);

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
   * List.
   *
   * @param pageRequest the page request
   * @return the page response
   */
  PageResponse<User> list(PageRequest<User> pageRequest);

  /**
   * Delete.
   *
   * @param userId the user id
   */
  void delete(@NotEmpty String userId);

  /**
   * Gets the.
   *
   * @param userId the user id
   * @return the user
   */
  User get(@NotEmpty String userId);

  /**
   * Revoke role.
   *
   * @param userId the user id
   * @param roleId the role id
   * @return the user
   */
  User revokeRole(@NotEmpty String userId, @NotEmpty String roleId);

  boolean verifyEmail(String emailAddress);

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

  AccountRole getUserAccountRole(String userId, String accountId);

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
  boolean updatePassword(String resetPasswordToken, String password);
}
