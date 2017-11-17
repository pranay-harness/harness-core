package software.wings.beans;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AppPermission {
  private AppPermissionEntityType appPermissionEntityType;
  private List<String> appIds;
  private List<String> filters;
  private List<AppPermissionAction> appPermissionActions;
}
