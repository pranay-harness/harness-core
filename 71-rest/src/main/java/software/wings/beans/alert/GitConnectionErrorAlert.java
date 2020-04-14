package software.wings.beans.alert;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GitConnectionErrorAlert implements AlertData {
  private String accountId;
  private String message;
  private String gitConnectorId;
  private String branchName;

  @Override
  public boolean matches(AlertData alertData) {
    try {
      GitConnectionErrorAlert gitConnectionErrorAlert = (GitConnectionErrorAlert) alertData;
      return accountId.equals(gitConnectionErrorAlert.accountId)
          && gitConnectorId.equals(gitConnectionErrorAlert.gitConnectorId)
          && branchName.equals(gitConnectionErrorAlert.branchName);
    } catch (Exception ex) {
      return false;
    }
  }

  @Override
  public String buildTitle() {
    return message;
  }
}
