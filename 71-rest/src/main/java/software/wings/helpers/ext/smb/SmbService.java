package software.wings.helpers.ext.smb;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.security.encryption.EncryptedDataDetail;
import software.wings.beans.SmbConfig;
import software.wings.helpers.ext.jenkins.BuildDetails;

import java.util.List;

@OwnedBy(CDC)
public interface SmbService {
  boolean isRunning(SmbConfig smbConfig, List<EncryptedDataDetail> encryptionDetails);
  List<String> getArtifactPaths(SmbConfig smbConfig, List<EncryptedDataDetail> encryptionDetails);
  List<BuildDetails> getBuildDetails(SmbConfig smbConfig, List<EncryptedDataDetail> encryptionDetails,
      List<String> artifactPaths, boolean isExpression);
}
