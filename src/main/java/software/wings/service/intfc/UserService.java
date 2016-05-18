package software.wings.service.intfc;

import software.wings.beans.User;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;

/**
 * Created by anubhaw on 3/28/16.
 */
public interface UserService {
  public User register(User user);

  public boolean matchPassword(String password, String hash);

  public User addRole(String userId, String roleId);

  public User update(User user);

  public PageResponse<User> list(PageRequest<User> pageRequest);

  public void delete(String userId);

  public User get(String userId);

  public User revokeRole(String userId, String roleId);
}
