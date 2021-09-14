/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.artifacts.docker.client;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.artifacts.docker.DockerRegistryRestClient;
import io.harness.artifacts.docker.beans.DockerInternalConfig;

@OwnedBy(CDC)
public interface DockerRestClientFactory {
  DockerRegistryRestClient getDockerRegistryRestClient(DockerInternalConfig dockerConfig);
}
