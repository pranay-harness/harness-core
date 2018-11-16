package software.wings.helpers.ext.sftp;

import software.wings.beans.SftpConfig;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;

public interface SftpService {
  boolean isRunning(SftpConfig sftpConfig, List<EncryptedDataDetail> encryptionDetails);
  List<String> getArtifactPaths(SftpConfig sftpConfig, List<EncryptedDataDetail> encryptionDetails);
  List<BuildDetails> getBuildDetails(SftpConfig sftpConfig, List<EncryptedDataDetail> encryptionDetails,
      List<String> artifactPaths, boolean isExpression);
}