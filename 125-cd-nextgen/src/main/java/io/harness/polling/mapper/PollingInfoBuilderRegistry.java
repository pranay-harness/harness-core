/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.polling.mapper;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.polling.contracts.Type;
import io.harness.polling.mapper.artifact.DockerHubArtifactInfoBuilder;
import io.harness.polling.mapper.artifact.EcrArtifactInfoBuilder;
import io.harness.polling.mapper.artifact.GcrArtifactInfoBuilder;
import io.harness.polling.mapper.manifest.GcsHelmChartManifestInfoBuilder;
import io.harness.polling.mapper.manifest.HttpHelmChartManifestInfoBuilder;
import io.harness.polling.mapper.manifest.S3HelmChartManifestInfoBuilder;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

@OwnedBy(HarnessTeam.CDC)
@Singleton
public class PollingInfoBuilderRegistry {
  @Inject private Injector injector;

  private final Map<Type, Class<? extends PollingInfoBuilder>> registeredPollingInfoBuilders =
      new EnumMap<>(Type.class);

  public PollingInfoBuilderRegistry() {
    registeredPollingInfoBuilders.put(Type.HTTP_HELM, HttpHelmChartManifestInfoBuilder.class);
    registeredPollingInfoBuilders.put(Type.S3_HELM, S3HelmChartManifestInfoBuilder.class);
    registeredPollingInfoBuilders.put(Type.GCS_HELM, GcsHelmChartManifestInfoBuilder.class);
    registeredPollingInfoBuilders.put(Type.ECR, EcrArtifactInfoBuilder.class);
    registeredPollingInfoBuilders.put(Type.DOCKER_HUB, DockerHubArtifactInfoBuilder.class);
    registeredPollingInfoBuilders.put(Type.GCR, GcrArtifactInfoBuilder.class);
  }

  public Optional<PollingInfoBuilder> getPollingInfoBuilder(Type type) {
    if (!registeredPollingInfoBuilders.containsKey(type)) {
      return Optional.empty();
    }
    return Optional.of(injector.getInstance(registeredPollingInfoBuilders.get(type)));
  }
}
