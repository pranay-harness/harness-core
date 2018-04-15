package software.wings.beans.security.access;

import lombok.Builder;
import lombok.Data;

/**
 * @author rktummala on 04/12/18
 */
@Data
@Builder
public class GlobalWhitelistConfig {
  private String filters;
}
