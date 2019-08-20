package software.wings.helpers.ext.docker.client;

import io.harness.security.encryption.EncryptedDataDetail;
import software.wings.beans.DockerConfig;
import software.wings.helpers.ext.docker.DockerRegistryRestClient;

import java.util.List;

public interface DockerRestClientFactory {
  DockerRegistryRestClient getDockerRegistryRestClient(
      DockerConfig dockerConfig, List<EncryptedDataDetail> encryptionDetails);
}