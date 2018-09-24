package software.wings.beans.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.wings.security.Filter;
import software.wings.security.GenericEntityFilter;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.PermissionAttribute.PermissionType;

import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AppPermission {
  private PermissionType permissionType;
  private GenericEntityFilter appFilter;
  private Filter entityFilter;
  private Set<Action> actions;
}
