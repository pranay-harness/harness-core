package software.wings.helpers.ext.sftp;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.eraro.ErrorCode.INVALID_ARTIFACT_SERVER;
import static io.harness.exception.WingsException.USER;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.WingsException;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.SftpConfig;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.service.impl.SftpHelperService;

import java.util.Collections;
import java.util.List;

@OwnedBy(CDC)
@Singleton
@Slf4j
public class SftpServiceImpl implements SftpService {
  @Inject private SftpHelperService sftpHelperService;

  @Override
  public List<String> getArtifactPaths(SftpConfig sftpConfig, List<EncryptedDataDetail> encryptionDetails) {
    List<String> pathList = Collections.EMPTY_LIST;
    try {
      pathList = sftpHelperService.getSftpPaths(sftpConfig, encryptionDetails);
    } catch (Exception ex) {
      throw new WingsException(INVALID_ARTIFACT_SERVER, USER)
          .addParam("message", "Could not get Artifact paths from SFTP Storage.");
    }
    return pathList;
  }

  @Override
  public boolean isRunning(SftpConfig sftpConfig, List<EncryptedDataDetail> encryptionDetails) {
    return true;
  }

  @Override
  public List<BuildDetails> getBuildDetails(SftpConfig sftpConfig, List<EncryptedDataDetail> encryptionDetails,
      List<String> artifactPaths, boolean isExpression) {
    List<BuildDetails> buildDetailsList;
    try {
      buildDetailsList = sftpHelperService.getArtifactDetails(sftpConfig, encryptionDetails, artifactPaths);
    } catch (Exception e) {
      logger.error("Error while retrieving artifacts build details from SFTP Server : {}", sftpConfig.getSftpUrl());
      throw new WingsException(INVALID_ARTIFACT_SERVER, USER).addParam("message", ExceptionUtils.getMessage(e));
    }
    return buildDetailsList;
  }
}