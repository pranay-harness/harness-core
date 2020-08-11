package software.wings.service.mappers.artifact;

import io.harness.artifacts.docker.beans.DockerInternalConfig;
import io.harness.data.structure.EmptyPredicate;
import lombok.experimental.UtilityClass;
import software.wings.beans.DockerConfig;

@UtilityClass
public class DockerConfigToInternalMapper {
  public DockerInternalConfig toDockerInternalConfig(DockerConfig dockerConfig) {
    String password =
        EmptyPredicate.isNotEmpty(dockerConfig.getPassword()) ? new String(dockerConfig.getPassword()) : null;
    return DockerInternalConfig.builder()
        .dockerRegistryUrl(dockerConfig.getDockerRegistryUrl())
        .username(dockerConfig.getUsername())
        .password(password)
        .build();
  }
}
