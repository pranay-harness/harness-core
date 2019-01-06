package software.wings.beans;

import lombok.Builder;
import lombok.Data;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;

@Data
@Builder
public class GitFetchFilesConfig {
  private GitFileConfig gitFileConfig;
  private GitConfig gitConfig;
  private List<EncryptedDataDetail> encryptedDataDetails;
}
