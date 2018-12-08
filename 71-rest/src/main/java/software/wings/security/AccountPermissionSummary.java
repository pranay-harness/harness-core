package software.wings.security;

import lombok.Builder;
import lombok.Data;
import software.wings.security.PermissionAttribute.PermissionType;

import java.util.Set;

@Data
@Builder
public class AccountPermissionSummary {
  private Set<PermissionType> permissions;
}
