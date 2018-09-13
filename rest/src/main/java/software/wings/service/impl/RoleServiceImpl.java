package software.wings.service.impl;

import static software.wings.utils.Validator.notNullCheck;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.InvalidRequestException;
import software.wings.beans.Role;
import software.wings.beans.RoleType;
import software.wings.beans.User;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.RoleService;
import software.wings.service.intfc.UserService;

import java.util.List;
import java.util.concurrent.ExecutorService;
import javax.validation.executable.ValidateOnExecution;

/**
 * Created by anubhaw on 3/23/16.
 */
@ValidateOnExecution
@Singleton
public class RoleServiceImpl implements RoleService {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private UserService userService;
  @Inject private ExecutorService executorService;

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
    Role savedRole = get(role.getUuid());
    notNullCheck("Role", savedRole);

    ensureNonAdminRole(role);

    wingsPersistence.updateFields(Role.class, role.getUuid(),
        ImmutableMap.of(
            "name", role.getName(), "description", role.getDescription(), "permissions", role.getPermissions()));
    return get(role.getUuid());
  }

  private void ensureNonAdminRole(Role role) {
    if (role.getRoleType() == RoleType.ACCOUNT_ADMIN) {
      throw new InvalidRequestException("Administrator role can not be modified");
    }
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.RoleService#delete(java.lang.String)
   */
  @Override
  public void delete(String roleId) {
    Role role = get(roleId);
    notNullCheck("Role", role);
    ensureNonAdminRole(role);

    boolean delete = wingsPersistence.delete(Role.class, roleId);
    if (delete) {
      executorService.submit(() -> {
        List<User> users =
            wingsPersistence.createQuery(User.class).disableValidation().filter("roles", roleId).asList();
        for (User user : users) {
          userService.revokeRole(user.getUuid(), roleId);
        }
      });
    }
  }

  @Override
  public Role getAccountAdminRole(String accountId) {
    return wingsPersistence.createQuery(Role.class)
        .filter("roleType", RoleType.ACCOUNT_ADMIN)
        .filter("accountId", accountId)
        .get();
  }

  @Override
  public List<Role> getAccountRoles(String accountId) {
    return wingsPersistence.createQuery(Role.class).filter("accountId", accountId).asList();
  }

  @Override
  public Role getAppAdminRole(String accountId, String appId) {
    return wingsPersistence.createQuery(Role.class)
        .filter("roleType", RoleType.APPLICATION_ADMIN)
        .filter("accountId", accountId)
        .filter("appId", appId)
        .get();
  }
}
