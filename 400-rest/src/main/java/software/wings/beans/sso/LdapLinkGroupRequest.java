package software.wings.beans.sso;

import static io.harness.annotations.dev.HarnessModule._950_NG_AUTHENTICATION_SERVICE;
import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import lombok.Data;
import org.hibernate.validator.constraints.NotBlank;

/**
 * @author Swapnil on 28/08/18
 */
@OwnedBy(PL)
@Data
@TargetModule(_950_NG_AUTHENTICATION_SERVICE)
public class LdapLinkGroupRequest {
  @NotBlank String ldapGroupDN;
  @NotBlank String ldapGroupName;
}
