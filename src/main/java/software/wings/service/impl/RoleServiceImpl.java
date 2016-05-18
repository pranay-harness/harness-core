package software.wings.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Role;
import software.wings.beans.User;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
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
  private final Logger logger = LoggerFactory.getLogger(getClass());
  @Inject private WingsPersistence wingsPersistence;
  @Inject private UserService userService;

  @Override
  public PageResponse<Role> list(PageRequest<Role> pageRequest) {
    return wingsPersistence.query(Role.class, pageRequest);
  }

  @Override
  public Role save(Role role) {
    return wingsPersistence.saveAndGet(Role.class, role);
  }

  @Override
  public Role findByUuid(String uuid) {
    return wingsPersistence.get(Role.class, uuid);
  }

  @Override
  public Role update(Role role) {
    return save(role);
  }

  @Override
  public void delete(String roleId) {
    wingsPersistence.delete(Role.class, roleId);
    List<User> users =
        wingsPersistence.createQuery(User.class).disableValidation().field("roles").equal(roleId).asList();
    for (User user : users) {
      userService.revokeRole(user.getUuid(), roleId);
    }
  }
}
