/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.generator;

import static io.harness.govern.IgnoreThrowable.ignoredOnPurpose;

import io.harness.exception.InvalidRequestException;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.Randomizer.Seed;

import software.wings.beans.Application;
import software.wings.beans.Service;
import software.wings.beans.artifact.ArtifactStreamBinding;
import software.wings.beans.artifact.ArtifactStreamSummary;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.ArtifactStreamServiceBindingService;
import software.wings.service.intfc.ServiceVariableService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class ArtifactStreamBindingGenerator {
  @Inject ArtifactStreamServiceBindingService artifactStreamServiceBindingService;
  @Inject ServiceVariableService serviceVariableService;
  @Inject WingsPersistence wingsPersistence;

  public ArtifactStreamBinding ensurePredefined(Seed seed, Owners owners, String name, String... artifactStreamIds) {
    Service service = owners.obtainService();
    Application application = owners.obtainApplication();
    List<ArtifactStreamSummary> artifactStreamSummaries = new ArrayList<>();
    for (String artifactStreamId : artifactStreamIds) {
      artifactStreamSummaries.add(ArtifactStreamSummary.builder().artifactStreamId(artifactStreamId).build());
    }
    ArtifactStreamBinding artifactStreamBinding =
        ArtifactStreamBinding.builder().name(name).artifactStreams(artifactStreamSummaries).build();
    ArtifactStreamBinding existing = exists(application.getUuid(), service.getUuid(), name);
    if (existing != null) {
      return existing;
    }

    return artifactStreamServiceBindingService.create(application.getUuid(), service.getUuid(), artifactStreamBinding);
  }

  public ArtifactStreamBinding exists(String appId, String serviceId, String serviceVariableName) {
    try {
      return artifactStreamServiceBindingService.get(appId, serviceId, serviceVariableName);
    } catch (InvalidRequestException e) {
      ignoredOnPurpose(e);
    }
    return null;
  }
}
