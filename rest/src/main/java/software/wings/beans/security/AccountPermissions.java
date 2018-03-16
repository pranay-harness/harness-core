package software.wings.beans.security;

import lombok.Builder;
import lombok.Data;
import software.wings.security.PermissionAttribute.PermissionType;

import java.util.Set;

/**
 * @author rktummala on 02/09/18
 */
@Builder
@Data
public class AccountPermissions {
  private Set<PermissionType> permissions;
}
