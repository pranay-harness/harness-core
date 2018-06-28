package software.wings.beans.config;

import static io.harness.data.structure.MapUtils.putIfNotEmpty;
import static software.wings.common.Constants.ARTIFACT_SOURCE_REGISTRY_URL_KEY;
import static software.wings.common.Constants.ARTIFACT_SOURCE_REPOSITORY_NAME_KEY;
import static software.wings.common.Constants.ARTIFACT_SOURCE_USER_NAME_KEY;

import java.util.HashMap;
import java.util.Map;

public interface ArtifactSourceable {
  default Map
    <String, String> fetchArtifactSourceProperties() {
      Map<String, String> attributes = new HashMap<>();
      putIfNotEmpty(ARTIFACT_SOURCE_USER_NAME_KEY, fetchUserName(), attributes);
      putIfNotEmpty(ARTIFACT_SOURCE_REGISTRY_URL_KEY, fetchRegistryUrl(), attributes);
      putIfNotEmpty(ARTIFACT_SOURCE_REPOSITORY_NAME_KEY, fetchRepositoryName(), attributes);
      return attributes;
    }

  default String
    fetchUserName() {
      return null;
    }

  default String
    fetchRegistryUrl() {
      return null;
    }

  default String
    fetchRepositoryName() {
      return null;
    }
}
