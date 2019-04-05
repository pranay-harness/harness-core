package software.wings.beans.settings.helm;

import io.harness.delegate.task.TaskParameters;
import lombok.Builder;
import lombok.Data;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;

@Data
@Builder
public class HelmRepoConfigValidationTaskParams implements TaskParameters {
  private String accountId;
  private String appId;
  HelmRepoConfig helmRepoConfig;
  private List<EncryptedDataDetail> encryptedDataDetails;
}
