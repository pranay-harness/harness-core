package software.wings.beans.security;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.Filter;
import io.harness.beans.GenericEntityFilter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.PermissionAttribute.PermissionType;

import java.util.Set;

@OwnedBy(PL)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppPermission {
  private PermissionType permissionType;
  private GenericEntityFilter appFilter;
  private Filter entityFilter;
  private Set<Action> actions;
}
