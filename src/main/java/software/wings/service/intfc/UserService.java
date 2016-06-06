package software.wings.service.intfc;

import org.hibernate.validator.constraints.NotEmpty;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.User;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.utils.validation.Create;
import software.wings.utils.validation.Update;

import javax.validation.Valid;

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
  @ValidationGroups(Create.class) public User register(@Valid User user);

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
  public User revokeRole(String userId, String roleId);
}
