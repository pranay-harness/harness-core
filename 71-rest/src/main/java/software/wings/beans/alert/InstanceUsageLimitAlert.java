package software.wings.beans.alert;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.alert.AlertData;
import lombok.AllArgsConstructor;
import lombok.Value;

import javax.validation.constraints.NotNull;

/**
 * When an account is close to it's usage limit, these alerts are triggered.
 */
@Value
@AllArgsConstructor
public class InstanceUsageLimitAlert implements AlertData {
  @NotNull String accountId;
  @NotNull long usagePercentage; // this is if we want different alerts for different usage percentage
  @NotNull String message;

  @Override
  public boolean matches(AlertData alertData) {
    InstanceUsageLimitAlert alert = (InstanceUsageLimitAlert) alertData;
    return alert.getAccountId().equals(accountId) && alert.getUsagePercentage() == usagePercentage;
  }

  @Override
  public String buildTitle() {
    return isEmpty(message) ? "" : message;
  }
}
