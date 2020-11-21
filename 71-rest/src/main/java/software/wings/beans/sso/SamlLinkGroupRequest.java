package software.wings.beans.sso;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import lombok.Data;
import org.hibernate.validator.constraints.NotBlank;

/**
 * @author Swapnil
 */
@OwnedBy(PL)
@Data
public class SamlLinkGroupRequest {
  @NotBlank String samlGroupName;
}
