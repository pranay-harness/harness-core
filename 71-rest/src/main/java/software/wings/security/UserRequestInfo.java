package software.wings.security;

import static io.harness.annotations.dev.HarnessTeam.PL;

import com.google.common.collect.ImmutableList;

import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Created by rishi on 3/24/17.
 */
@OwnedBy(PL)
@Data
@Builder
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
