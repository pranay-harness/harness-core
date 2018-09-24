package software.wings.security;

import lombok.Builder;
import lombok.Data;
import software.wings.security.PermissionAttribute.PermissionType;

import java.util.Set;

@Builder
@Data
public class AccountPermissionSummary {
  private Set<PermissionType> permissions;
}
