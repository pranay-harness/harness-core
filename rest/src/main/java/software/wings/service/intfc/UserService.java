package software.wings.service.intfc;

import org.hibernate.validator.constraints.NotEmpty;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.User;
import software.wings.beans.UserInvite;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.utils.validation.Create;
import software.wings.utils.validation.Update;

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
  @ValidationGroups(Create.class) public User register(@Valid User user);

  /**
   * Match password.
   *
   * @param password the password
   * @param hash     the hash
   * @return true, if successful
   */
  public boolean matchPassword(@NotEmpty String password, @NotEmpty String hash);

  /**
   * Adds the role.
   *
   * @param userId the user id
   * @param roleId the role id
   * @return the user
   */
  public User addRole(@NotEmpty String userId, @NotEmpty String roleId);

  /**
   * Update.
   *
   * @param user the user
   * @return the user
   */
  @ValidationGroups(Update.class) public User update(@Valid User user);

  /**
   * List.
   *
   * @param pageRequest the page request
   * @return the page response
   */
  public PageResponse<User> list(PageRequest<User> pageRequest);

  /**
   * Delete.
   *
   * @param userId the user id
   */
  public void delete(@NotEmpty String userId);

  /**
   * Gets the.
   *
   * @param userId the user id
   * @return the user
   */
  public User get(@NotEmpty String userId);

  /**
   * Revoke role.
   *
   * @param userId the user id
   * @param roleId the role id
   * @return the user
   */
  public User revokeRole(@NotEmpty String userId, @NotEmpty String roleId);

  /**
   * Verify email string.
   *
   * @param token the token
   * @return the string
   */
  boolean verifyEmail(String token);

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
  UserInvite inviteUser(UserInvite userInvite);

  /**
   * List invites page response.
   *
   * @param pageRequest the page request
   * @return the page response
   */
  PageResponse<UserInvite> listInvites(PageRequest<UserInvite> pageRequest);
}
