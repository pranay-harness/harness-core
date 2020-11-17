package software.wings.api.terraform;

import io.harness.provision.TfVarSource;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.GitConfig;
import software.wings.beans.GitFileConfig;

import java.util.List;

@Data
@Builder
@EqualsAndHashCode(of = "gitFileConfig")
public class TfVarGitSource implements TfVarSource {
  private GitConfig gitConfig;
  private GitFileConfig gitFileConfig;
  private List<EncryptedDataDetail> encryptedDataDetails;

  @Override
  public TfVarSourceType getTfVarSourceType() {
    return TfVarSourceType.GIT;
  }
}
