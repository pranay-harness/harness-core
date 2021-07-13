package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * @author rktummala on 01/11/19
 */

@Data
@Builder
@OwnedBy(PL)
public class DefaultSalesContacts {
  private boolean enabled;
  private List<AccountTypeDefault> accountTypeDefaults;

  @Data
  @Builder
  public static class AccountTypeDefault {
    private String accountType;
    private String emailIds;
  }
}
