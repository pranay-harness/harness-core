package software.wings.beans.alert;

import lombok.Builder;
import lombok.Data;

/**
 * @author rktummala on 12/19/2017
 */
@Data
@Builder
public class GitSyncErrorAlert implements AlertData {
  private String accountId;
  private String message;
  private boolean gitToHarness;

  @Override
  public boolean matches(AlertData alertData) {
    return accountId.equals(((GitSyncErrorAlert) alertData).accountId)
        && (gitToHarness == ((GitSyncErrorAlert) alertData).gitToHarness);
  }

  @Override
  public String buildTitle() {
    return message;
  }
}
