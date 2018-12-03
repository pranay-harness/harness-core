package software.wings.beans;

import io.harness.delegate.task.protocol.TaskParameters;
import lombok.Builder;
import lombok.Data;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;

@Data
@Builder
public class GitFetchFilesTaskParams implements TaskParameters {
  private String accountId;
  private String appId;
  private String activityId;
  private boolean isFinalState;
  private GitFileConfig gitFileConfig;
  private GitConfig gitConfig;
  private List<EncryptedDataDetail> encryptedDataDetails;
}
