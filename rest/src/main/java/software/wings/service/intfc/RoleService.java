package software.wings.service.intfc;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import software.wings.beans.Role;

import java.util.List;

/**
 * Created by anubhaw on 3/28/16.
 */
public interface RoleService {
  /**
   * List.
   *
   * @param pageRequest the page request
   * @return the page response
   */
  PageResponse<Role> list(PageRequest<Role> pageRequest);

  /**
   * Save.
   *
   * @param role the role
   * @return the role
   */
  Role save(Role role);

  /**
   * Find by uuid.
   *
   * @param uuid the uuid
   * @return the role
   */
  Role get(String uuid);

  /**
   * Update.
   *
   * @param role the role
   * @return the role
   */
  Role update(Role role);

  /**
   * Delete.
   *
   * @param roleId the role id
   */
  void delete(String roleId);

  /**
   * Gets account admin role.
   *
   * @return the account admin role
   */
  Role getAccountAdminRole(String accountId);

  /**
   * Gets account roles.
   *
   * @return the account roles
   */
  List<Role> getAccountRoles(String accountId);

  /**
   * Gets app admin role.
   *
   * @return the app admin role
   */
  Role getAppAdminRole(String accountId, String appId);
}
