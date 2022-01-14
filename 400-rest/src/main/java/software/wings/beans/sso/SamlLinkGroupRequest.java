package software.wings.beans.sso;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import lombok.Data;
import org.hibernate.validator.constraints.NotBlank;

/**
 * @author Swapnil
 */
@OwnedBy(PL)
@Data
@TargetModule(HarnessModule._950_NG_AUTHENTICATION_SERVICE)
public class SamlLinkGroupRequest {
  @NotBlank String samlGroupName;
}
