package software.wings.beans.alert;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NoActiveDelegatesAlert implements AlertData {
  private String accountId;

  @Override
  public boolean matches(AlertData alertData) {
    return accountId.equals(((NoActiveDelegatesAlert) alertData).getAccountId());
  }

  @Override
  public String buildTitle() {
    return "No delegates are available";
  }
}
