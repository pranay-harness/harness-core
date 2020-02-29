package software.wings.helpers.ext.smb;

import static io.harness.eraro.ErrorCode.INVALID_ARTIFACT_SERVER;
import static io.harness.exception.WingsException.USER;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.hierynomus.mssmb2.SMBApiException;
import io.harness.eraro.ErrorCode;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.WingsException;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.SmbConfig;
import software.wings.exception.InvalidArtifactServerException;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.service.impl.SmbHelperService;

import java.util.Collections;
import java.util.List;

@Singleton
@Slf4j
public class SmbServiceImpl implements SmbService {
  @Inject private SmbHelperService smbHelperService;

  @Override
  public List<String> getArtifactPaths(SmbConfig smbConfig, List<EncryptedDataDetail> encryptionDetails) {
    List<String> pathList = Collections.EMPTY_LIST;
    try {
      pathList = smbHelperService.getSmbPaths(smbConfig, encryptionDetails);
    } catch (Exception ex) {
      throw new WingsException(ErrorCode.INVALID_ARTIFACT_SERVER, WingsException.USER)
          .addParam("message", "Could not get Artifact paths from SMB Storage.");
    }
    return pathList;
  }

  @Override
  public boolean isRunning(SmbConfig smbConfig, List<EncryptedDataDetail> encryptionDetails) {
    try {
      smbHelperService.checkConnection(smbConfig, encryptionDetails);
    } catch (SMBApiException SMBe) {
      throw new InvalidArtifactServerException("Invalid Samba Server credentials", SMBe);
    } catch (Exception e) {
      throw new InvalidArtifactServerException(ExceptionUtils.getMessage(e), e);
    }
    return true;
  }

  @Override
  public List<BuildDetails> getBuildDetails(SmbConfig smbConfig, List<EncryptedDataDetail> encryptionDetails,
      List<String> artifactPaths, boolean isExpression) {
    List<BuildDetails> buildDetailsList = Lists.newArrayList();
    try {
      buildDetailsList = smbHelperService.getArtifactDetails(smbConfig, encryptionDetails, artifactPaths);
    } catch (Exception e) {
      throw new WingsException(INVALID_ARTIFACT_SERVER, USER).addParam("message", ExceptionUtils.getMessage(e));
    }
    return buildDetailsList;
  }
}
