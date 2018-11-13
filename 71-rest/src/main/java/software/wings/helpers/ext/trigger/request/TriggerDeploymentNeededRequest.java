package software.wings.helpers.ext.trigger.request;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.GitConfig;
import software.wings.beans.trigger.TriggerCommand.TriggerCommandType;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
public class TriggerDeploymentNeededRequest extends TriggerRequest {
  private GitConfig gitConfig;
  private String gitConnectorId;
  private String currentCommitId;
  private String oldCommitId;
  private String branch;
  private List<String> filePaths;
  private List<EncryptedDataDetail> encryptionDetails;

  public TriggerDeploymentNeededRequest() {
    super(TriggerCommandType.DEPLOYMENT_NEEDED_CHECK);
  }

  @Builder
  public TriggerDeploymentNeededRequest(String accountId, String appId, GitConfig gitConfig, String gitConnectorId,
      String currentCommitId, String oldCommitId, String branch, List<String> filePaths,
      List<EncryptedDataDetail> encryptionDetails) {
    super(TriggerCommandType.DEPLOYMENT_NEEDED_CHECK, accountId, appId);
    this.gitConfig = gitConfig;
    this.gitConnectorId = gitConnectorId;
    this.currentCommitId = currentCommitId;
    this.oldCommitId = oldCommitId;
    this.branch = branch;
    this.filePaths = filePaths;
    this.encryptionDetails = encryptionDetails;
  }
}
