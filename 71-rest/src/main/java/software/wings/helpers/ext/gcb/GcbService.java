package software.wings.helpers.ext.gcb;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.security.encryption.EncryptedDataDetail;
import software.wings.beans.GcpConfig;
import software.wings.helpers.ext.gcb.models.BuildOperationDetails;
import software.wings.helpers.ext.gcb.models.GcbBuildDetails;
import software.wings.helpers.ext.gcb.models.RepoSource;

import java.util.List;

@OwnedBy(CDC)
public interface GcbService {
  BuildOperationDetails createBuild(
      GcpConfig gcpConfig, List<EncryptedDataDetail> encryptionDetails, String projectId, GcbBuildDetails buildParams);

  GcbBuildDetails getBuild(
      GcpConfig gcpConfig, List<EncryptedDataDetail> encryptionDetails, String projectId, String buildId);

  BuildOperationDetails runTrigger(GcpConfig gcpConfig, List<EncryptedDataDetail> encryptionDetails, String projectId,
      String triggerId, RepoSource repoSource);
}
