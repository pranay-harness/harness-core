package software.wings.service.intfc;

import software.wings.beans.Role;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;

/**
 * Created by anubhaw on 3/28/16.
 */
public interface RoleService {
  public PageResponse<Role> list(PageRequest<Role> pageRequest);

  public Role save(Role role);

  public Role findByUuid(String uuid);

  public Role update(Role role);

  public void delete(String roleId);
}
