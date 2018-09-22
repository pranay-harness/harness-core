package software.wings.beans.sso;

import lombok.Data;
import org.hibernate.validator.constraints.NotBlank;

/**
 * @author Swapnil on 28/08/18
 */

@Data
public class LdapLinkGroupRequest {
  @NotBlank String ldapGroupDN;
  @NotBlank String ldapGroupName;
}
