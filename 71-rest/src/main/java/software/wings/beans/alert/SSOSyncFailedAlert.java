package software.wings.beans.alert;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import javax.validation.constraints.NotNull;

/**
 * Alert for displaying issues with syncing user groups with SSO provider via cron job
 *
 * @author Swapnil on 29/08/18
 */
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SSOSyncFailedAlert implements AlertData {
  @NotNull String accountId;
  @NotNull String ssoId;
  @NotNull String message;

  @Override
  public boolean matches(AlertData alertData) {
    SSOSyncFailedAlert alert = (SSOSyncFailedAlert) alertData;
    return alert.getAccountId().equals(accountId) && alert.getSsoId().equals(ssoId);
  }

  @Override
  public String buildTitle() {
    return isEmpty(message) ? "" : message;
  }
}
