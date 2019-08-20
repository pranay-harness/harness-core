package software.wings.helpers.ext.sftp;

import io.harness.security.encryption.EncryptedDataDetail;
import software.wings.beans.SftpConfig;
import software.wings.helpers.ext.jenkins.BuildDetails;

import java.util.List;

public interface SftpService {
  boolean isRunning(SftpConfig sftpConfig, List<EncryptedDataDetail> encryptionDetails);
  List<String> getArtifactPaths(SftpConfig sftpConfig, List<EncryptedDataDetail> encryptionDetails);
  List<BuildDetails> getBuildDetails(SftpConfig sftpConfig, List<EncryptedDataDetail> encryptionDetails,
      List<String> artifactPaths, boolean isExpression);
}