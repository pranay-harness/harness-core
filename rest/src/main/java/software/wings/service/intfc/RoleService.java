package software.wings.service.intfc;

import software.wings.beans.Role;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;

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
   * Gets admin role.
   *
   * @return the admin role
   */
  Role getAdminRole();
}
