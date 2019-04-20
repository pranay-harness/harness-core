package software.wings.service.impl;

import static software.wings.utils.Validator.notNullCheck;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.exception.InvalidRequestException;
import software.wings.beans.Event.Type;
import software.wings.beans.Role;
import software.wings.beans.RoleType;
import software.wings.beans.User;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AppService;
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
  @Inject private AuditServiceHelper auditServiceHelper;
  @Inject private AppService appService;

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
    Role savedRole = wingsPersistence.saveAndGet(Role.class, role);

    // TODO: AUDIT: Once this entity is yamlized, this can be removed
    auditServiceHelper.reportForAuditingUsingAccountId(role.getAccountId(), null, role, Type.CREATE);
    return savedRole;
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

    Role updatedRole = get(role.getUuid());
    // TODO: AUDIT: Once this entity is yamlized, this can be removed
    auditServiceHelper.reportForAuditingUsingAccountId(role.getAccountId(), savedRole, updatedRole, Type.UPDATE);
    return updatedRole;
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

    // TODO: AUDIT: Once this entity is yamlized, this can be removed
    auditServiceHelper.reportDeleteForAuditingUsingAccountId(role.getAccountId(), role);
  }

  @Override
  public Role getAccountAdminRole(String accountId) {
    return wingsPersistence.createQuery(Role.class)
        .filter("roleType", RoleType.ACCOUNT_ADMIN)
        .filter(Role.ACCOUNT_ID_KEY, accountId)
        .get();
  }

  @Override
  public List<Role> getAccountRoles(String accountId) {
    return wingsPersistence.createQuery(Role.class).filter(Role.ACCOUNT_ID_KEY, accountId).asList();
  }

  @Override
  public Role getAppAdminRole(String accountId, String appId) {
    return wingsPersistence.createQuery(Role.class)
        .filter("roleType", RoleType.APPLICATION_ADMIN)
        .filter(Role.ACCOUNT_ID_KEY, accountId)
        .filter("appId", appId)
        .get();
  }

  @Override
  public void deleteByAccountId(String accountId) {
    wingsPersistence.delete(wingsPersistence.createQuery(Role.class).filter(Role.ACCOUNT_ID_KEY, accountId));
  }
}
