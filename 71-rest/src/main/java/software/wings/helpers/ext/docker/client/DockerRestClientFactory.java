package software.wings.helpers.ext.docker.client;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.artifacts.docker.beans.DockerInternalConfig;
import software.wings.helpers.ext.docker.DockerRegistryRestClient;

@OwnedBy(CDC)
public interface DockerRestClientFactory {
  DockerRegistryRestClient getDockerRegistryRestClient(DockerInternalConfig dockerConfig);
}