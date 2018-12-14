package software.wings.beans.sso;

import lombok.Data;
import org.hibernate.validator.constraints.NotBlank;

/**
 * @author Swapnil
 */

@Data
public class SamlLinkGroupRequest {
  @NotBlank String samlGroupName;
}
