package software.wings.beans.security.access;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Wrapper class created for holding all the white list configs for an account. It was needed for CacheHelper.
 * @author rktummala on 04/11/2018
 */
@Data
@Builder
public class WhitelistConfig {
  private String accountId;
  private List<Whitelist> whitelistList;
}
