package software.wings.beans.alert;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InvalidSMTPConfigAlert implements AlertData {
  String accountId;

  @Override
  public boolean matches(AlertData alertData) {
    return ((InvalidSMTPConfigAlert) alertData).accountId.equals(accountId);
  }

  @Override
  public String buildTitle() {
    return "No Valid SMTP configuration available";
  }
}
