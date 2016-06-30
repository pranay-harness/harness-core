package software.wings.service.impl;

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
import javax.validation.executable.ValidateOnExecution;

// TODO: Auto-generated Javadoc

/**
 * Created by anubhaw on 3/23/16.
 */
@ValidateOnExecution
@Singleton
public class RoleServiceImpl implements RoleService {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private UserService userService;

  /* (non-Javadoc)
   * @see software.wings.service.intfc.RoleService#list(software.wings.dl.PageRequest)
   */
  @Override
  public PageResponse<Role> list(PageRequest<Role> pageRequest) {
    return wingsPersistence.query(Role.class, pageRequest);
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.RoleService#save(software.wings.beans.Role)
   */
  @Override
  public Role save(Role role) {
    return wingsPersistence.saveAndGet(Role.class, role);
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.RoleService#get(java.lang.String)
   */
  @Override
  public Role get(String uuid) {
    return wingsPersistence.get(Role.class, uuid);
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.RoleService#update(software.wings.beans.Role)
   */
  @Override
  public Role update(Role role) {
    return save(role);
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.RoleService#delete(java.lang.String)
   */
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
