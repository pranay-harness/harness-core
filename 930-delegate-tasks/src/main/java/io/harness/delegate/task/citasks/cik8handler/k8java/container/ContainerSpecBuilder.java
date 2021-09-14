/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.task.citasks.cik8handler.k8java.container;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.ci.pod.ContainerParams;

import com.google.inject.Singleton;

@Singleton
@OwnedBy(HarnessTeam.CI)
public class ContainerSpecBuilder extends BaseContainerSpecBuilder {
  protected void decorateSpec(
      ContainerParams containerParams, ContainerSpecBuilderResponse containerSpecBuilderResponse) {
    // Nothing to decorate.
  }
}
