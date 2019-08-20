package software.wings.beans;

import io.harness.security.encryption.EncryptedDataDetail;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class GitFetchFilesConfig {
  private GitFileConfig gitFileConfig;
  private GitConfig gitConfig;
  private List<EncryptedDataDetail> encryptedDataDetails;
}
