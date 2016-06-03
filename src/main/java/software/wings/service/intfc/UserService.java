package software.wings.service.intfc;

import software.wings.beans.User;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;

// TODO: Auto-generated Javadoc

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
  public User register(User user);

  /**
   * Match password.
   *
   * @param password the password
   * @param hash     the hash
   * @return true, if successful
   */
  public boolean matchPassword(String password, String hash);

  /**
   * Adds the role.
   *
   * @param userId the user id
   * @param roleId the role id
   * @return the user
   */
  public User addRole(String userId, String roleId);

  /**
   * Update.
   *
   * @param user the user
   * @return the user
   */
  public User update(User user);

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
  public void delete(String userId);

  /**
   * Gets the.
   *
   * @param userId the user id
   * @return the user
   */
  public User get(String userId);

  /**
   * Revoke role.
   *
   * @param userId the user id
   * @param roleId the role id
   * @return the user
   */
  public User revokeRole(String userId, String roleId);
}
