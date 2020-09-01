package software.wings.beans.alert;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;

@Data
@Builder
public class GitConnectionErrorAlert implements AlertData {
  @NonNull private String accountId;
  private String message;
  @NonNull private String gitConnectorId;
  @NonNull private String branchName;
  private String repositoryName;

  @Override
  public boolean matches(AlertData alertData) {
    try {
      GitConnectionErrorAlert gitConnectionErrorAlert = (GitConnectionErrorAlert) alertData;
      return accountId.equals(gitConnectionErrorAlert.accountId)
          && gitConnectorId.equals(gitConnectionErrorAlert.gitConnectorId)
          && branchName.equals(gitConnectionErrorAlert.branchName)
          && StringUtils.equals(repositoryName, gitConnectionErrorAlert.getRepositoryName());
    } catch (Exception ex) {
      return false;
    }
  }

  @Override
  public String buildTitle() {
    return message;
  }
}
