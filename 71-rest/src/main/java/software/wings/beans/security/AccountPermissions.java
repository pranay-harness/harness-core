package software.wings.beans.security;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import software.wings.security.PermissionAttribute.PermissionType;

import java.util.Set;
import lombok.Builder;
import lombok.Data;

/**
 * @author rktummala on 02/09/18
 */
@OwnedBy(PL)
@Data
@Builder
public class AccountPermissions {
  private Set<PermissionType> permissions;
}
