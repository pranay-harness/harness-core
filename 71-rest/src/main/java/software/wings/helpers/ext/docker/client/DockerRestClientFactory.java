package software.wings.helpers.ext.docker.client;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import software.wings.beans.DockerConfig;
import software.wings.helpers.ext.docker.DockerRegistryRestClient;

@OwnedBy(CDC)
public interface DockerRestClientFactory {
  DockerRegistryRestClient getDockerRegistryRestClient(DockerConfig dockerConfig);
}