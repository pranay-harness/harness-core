/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans.config;

import static io.harness.data.structure.MapUtils.putIfNotEmpty;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import java.util.HashMap;
import java.util.Map;

@OwnedBy(HarnessTeam.CDC)
@TargetModule(HarnessModule._957_CG_BEANS)
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
