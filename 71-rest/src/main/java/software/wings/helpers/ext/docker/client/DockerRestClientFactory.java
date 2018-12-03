package software.wings.helpers.ext.docker.client;

import software.wings.beans.DockerConfig;
import software.wings.helpers.ext.docker.DockerRegistryRestClient;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;

public interface DockerRestClientFactory {
  DockerRegistryRestClient getDockerRegistryRestClient(
      DockerConfig dockerConfig, List<EncryptedDataDetail> encryptionDetails);
}