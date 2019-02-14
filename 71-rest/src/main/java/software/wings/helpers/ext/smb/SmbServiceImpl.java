package software.wings.helpers.ext.smb;

import static io.harness.eraro.ErrorCode.INVALID_ARTIFACT_SERVER;
import static io.harness.exception.WingsException.USER;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.eraro.ErrorCode;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.WingsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.SmbConfig;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.SmbHelperService;

import java.util.Collections;
import java.util.List;

@Singleton
public class SmbServiceImpl implements SmbService {
  private static final Logger logger = LoggerFactory.getLogger(software.wings.helpers.ext.smb.SmbServiceImpl.class);
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
