package software.wings.beans.security.access;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Data;

/**
 * @author rktummala on 04/12/18
 */
@OwnedBy(PL)
@Data
@Builder
public class GlobalWhitelistConfig {
  private String filters;
}
