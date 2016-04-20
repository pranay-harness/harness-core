package software.wings.service.intfc;

import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.beans.Role;

/**
 * Created by anubhaw on 3/28/16.
 */
public interface RoleService {
  public PageResponse<Role> list(PageRequest<Role> pageRequest);

  public Role save(Role role);

  public Role findByUUID(String uuid);

  public Role update(Role role);

  public void delete(String roleID);
}
