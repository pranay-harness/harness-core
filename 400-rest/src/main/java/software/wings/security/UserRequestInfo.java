package software.wings.security;

import static io.harness.annotations.dev.HarnessModule._950_NG_AUTHENTICATION_SERVICE;
import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import com.google.common.collect.ImmutableList;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * Created by rishi on 3/24/17.
 */
@OwnedBy(PL)
@Data
@Builder
@TargetModule(_950_NG_AUTHENTICATION_SERVICE)
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
