package software.wings.beans.alert;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GitConnectionErrorAlert implements AlertData {
  private String accountId;
  private String message;

  @Override
  public boolean matches(AlertData alertData) {
    return accountId.equals(((GitConnectionErrorAlert) alertData).accountId);
  }

  @Override
  public String buildTitle() {
    return message;
  }
}
