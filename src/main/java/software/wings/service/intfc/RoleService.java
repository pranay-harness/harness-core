package software.wings.service.intfc;

import software.wings.beans.Role;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;

// TODO: Auto-generated Javadoc

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
  public PageResponse<Role> list(PageRequest<Role> pageRequest);

  /**
   * Save.
   *
   * @param role the role
   * @return the role
   */
  public Role save(Role role);

  /**
   * Find by uuid.
   *
   * @param uuid the uuid
   * @return the role
   */
  public Role get(String uuid);

  /**
   * Update.
   *
   * @param role the role
   * @return the role
   */
  public Role update(Role role);

  /**
   * Delete.
   *
   * @param roleId the role id
   */
  public void delete(String roleId);
}
