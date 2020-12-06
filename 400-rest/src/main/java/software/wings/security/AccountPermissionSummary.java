package software.wings.security;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import software.wings.security.PermissionAttribute.PermissionType;

import java.util.Set;
import lombok.Builder;
import lombok.Data;

@OwnedBy(PL)
@Data
@Builder
public class AccountPermissionSummary {
  private Set<PermissionType> permissions;
}
