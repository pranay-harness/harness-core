package software.wings.beans.sso;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import lombok.Data;
import org.hibernate.validator.constraints.NotBlank;

@OwnedBy(PL)
@Data
public class LdapLinkGroupRequest {
  @NotBlank String ldapGroupDN;
  @NotBlank String ldapGroupName;
}
