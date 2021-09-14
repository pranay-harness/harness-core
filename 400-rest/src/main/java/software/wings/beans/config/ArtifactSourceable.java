/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans.config;

import static io.harness.data.structure.MapUtils.putIfNotEmpty;

import java.util.HashMap;
import java.util.Map;

public interface ArtifactSourceable {
  String ARTIFACT_SOURCE_USER_NAME_KEY = "username";
  String ARTIFACT_SOURCE_REGISTRY_URL_KEY = "registryUrl";
  String ARTIFACT_SOURCE_REPOSITORY_NAME_KEY = "repositoryName";
  String ARTIFACT_SOURCE_DOCKER_CONFIG_NAME_KEY = "dockerconfig";
  String ARTIFACT_SOURCE_DOCKER_CONFIG_PLACEHOLDER = "${dockerconfig}";

  default Map<String, String> fetchArtifactSourceProperties() {
    Map<String, String> attributes = new HashMap<>();
    putIfNotEmpty(ARTIFACT_SOURCE_USER_NAME_KEY, fetchUserName(), attributes);
    putIfNotEmpty(ARTIFACT_SOURCE_REGISTRY_URL_KEY, fetchRegistryUrl(), attributes);
    putIfNotEmpty(ARTIFACT_SOURCE_REPOSITORY_NAME_KEY, fetchRepositoryName(), attributes);
    putIfNotEmpty(ARTIFACT_SOURCE_DOCKER_CONFIG_NAME_KEY, ARTIFACT_SOURCE_DOCKER_CONFIG_PLACEHOLDER, attributes);
    return attributes;
  }

  default String fetchUserName() {
    return null;
  }

  default String fetchRegistryUrl() {
    return null;
  }

  default String fetchRepositoryName() {
    return null;
  }
}
