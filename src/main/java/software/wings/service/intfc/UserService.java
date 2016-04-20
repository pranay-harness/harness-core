package software.wings.service.intfc;

import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.beans.User;

/**
 * Created by anubhaw on 3/28/16.
 */
public interface UserService {
  public User register(User user);

  public boolean matchPassword(String password, String hash);

  public User addRole(String userID, String roleID);

  public User update(User user);

  public PageResponse<User> list(PageRequest<User> pageRequest);

  public void delete(String userID);

  public User get(String userID);

  public User revokeRole(String userID, String roleID);
}
