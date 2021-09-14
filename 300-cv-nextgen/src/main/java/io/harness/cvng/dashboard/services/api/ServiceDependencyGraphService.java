/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.dashboard.services.api;

import static io.harness.annotations.dev.HarnessTeam.CV;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.core.beans.params.ProjectParams;
import io.harness.cvng.dashboard.beans.ServiceDependencyGraphDTO;

import javax.annotation.Nullable;
import lombok.NonNull;

@OwnedBy(CV)
public interface ServiceDependencyGraphService {
  ServiceDependencyGraphDTO getDependencyGraph(
      @NonNull ProjectParams projectParams, @Nullable String serviceIdentifier, @Nullable String envIdentifier);
}
