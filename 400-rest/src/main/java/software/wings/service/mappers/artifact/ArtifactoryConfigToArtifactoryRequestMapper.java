package software.wings.service.mappers.artifact;

import io.harness.artifactory.ArtifactoryConfigRequest;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.config.ArtifactoryConfig;
import software.wings.service.intfc.security.EncryptionService;

import java.util.List;

public class ArtifactoryConfigToArtifactoryRequestMapper {
  public static ArtifactoryConfigRequest toArtifactoryRequest(
      ArtifactoryConfig config, EncryptionService encryptionService, List<EncryptedDataDetail> encryptedDetails) {
    if (config.hasCredentials()) {
      encryptionService.decrypt(config, encryptedDetails, false);
    }
    return ArtifactoryConfigRequest.builder()
        .artifactoryUrl(config.getArtifactoryUrl())
        .username(config.getUsername())
        .password(config.getPassword())
        .hasCredentials(config.hasCredentials())
        .build();
  }
}
