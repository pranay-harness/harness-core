package software.wings.security;

import com.google.common.collect.ImmutableList;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Created by rishi on 3/24/17.
 */
@Builder
@Data
public class UserRequestInfo {
  private String accountId;
  private List<String> appIds;
  private String envId;

  private boolean allAppsAllowed;
  private boolean allEnvironmentsAllowed;

  private ImmutableList<String> allowedAppIds;
  private ImmutableList<String> allowedEnvIds;

  private boolean appIdFilterRequired;
  private boolean envIdFilterRequired;

  private ImmutableList<PermissionAttribute> permissionAttributes;
}
