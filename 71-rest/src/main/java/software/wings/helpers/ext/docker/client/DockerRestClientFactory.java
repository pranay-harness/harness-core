package software.wings.helpers.ext.docker.client;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.security.encryption.EncryptedDataDetail;
import software.wings.beans.DockerConfig;
import software.wings.helpers.ext.docker.DockerRegistryRestClient;

import java.util.List;

@OwnedBy(CDC)
public interface DockerRestClientFactory {
  DockerRegistryRestClient getDockerRegistryRestClient(
      DockerConfig dockerConfig, List<EncryptedDataDetail> encryptionDetails);
}