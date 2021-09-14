/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.perpetualtask.manifest;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.task.manifests.request.ManifestCollectionParams;

import software.wings.beans.appmanifest.HelmChart;

import java.util.List;
import javax.validation.constraints.NotNull;

@TargetModule(HarnessModule._420_DELEGATE_AGENT)
public interface ManifestRepositoryService {
  List<HelmChart> collectManifests(@NotNull ManifestCollectionParams params) throws Exception;

  void cleanup(@NotNull ManifestCollectionParams params) throws Exception;
}
