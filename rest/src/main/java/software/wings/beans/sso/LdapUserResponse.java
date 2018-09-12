package software.wings.beans.sso;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.NotBlank;

/**
 * @author Swapni on 28/08/18
 */

@Builder
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LdapUserResponse {
  @NotBlank String dn;
  @NotBlank String email;
  @NotBlank String name;
}
