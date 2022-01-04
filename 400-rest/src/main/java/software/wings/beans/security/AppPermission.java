package software.wings.beans.security;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import software.wings.security.AppFilter;
import software.wings.security.Filter;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.PermissionAttribute.PermissionType;

import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@OwnedBy(PL)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppPermission {
  private PermissionType permissionType;
  private AppFilter appFilter;
  private Filter entityFilter;
  private Set<Action> actions;
}
