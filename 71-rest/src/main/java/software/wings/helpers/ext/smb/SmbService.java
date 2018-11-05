package software.wings.helpers.ext.smb;

import software.wings.beans.SmbConfig;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;

public interface SmbService {
  boolean isRunning(SmbConfig smbConfig, List<EncryptedDataDetail> encryptionDetails);
  List<String> getArtifactPaths(SmbConfig smbConfig, List<EncryptedDataDetail> encryptionDetails);
  List<BuildDetails> getBuildDetails(SmbConfig smbConfig, List<EncryptedDataDetail> encryptionDetails,
      List<String> artifactPaths, boolean isExpression);
}
