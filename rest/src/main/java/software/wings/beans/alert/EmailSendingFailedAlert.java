package software.wings.beans.alert;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EmailSendingFailedAlert implements AlertData {
  private String emailAlertData;

  @Override
  public boolean matches(AlertData alertData) {
    return ((EmailSendingFailedAlert) alertData).getEmailAlertData().equals(emailAlertData);
  }

  @Override
  public String buildTitle() {
    return emailAlertData;
  }
}
