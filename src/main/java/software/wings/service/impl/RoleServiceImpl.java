package software.wings.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.beans.Role;
import software.wings.beans.User;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.RoleService;
import software.wings.service.intfc.UserService;

import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by anubhaw on 3/23/16.
 */
@Singleton
public class RoleServiceImpl implements RoleService {
  private static Logger logger = LoggerFactory.getLogger(RoleServiceImpl.class);
  @Inject private WingsPersistence wingsPersistence;
  @Inject private UserService userService;

  public PageResponse<Role> list(PageRequest<Role> pageRequest) {
    return wingsPersistence.query(Role.class, pageRequest);
  }

  public Role save(Role role) {
    return wingsPersistence.saveAndGet(Role.class, role);
  }

  public Role findByUUID(String uuid) {
    return wingsPersistence.get(Role.class, uuid);
  }

  public Role update(Role role) {
    return save(role);
  }

  public void delete(String roleID) {
    wingsPersistence.delete(Role.class, roleID);
    List<User> users =
        wingsPersistence.createQuery(User.class).disableValidation().field("roles").equal(roleID).asList();
    for (User user : users) {
      userService.revokeRole(user.getUuid(), roleID);
    }
  }
}
