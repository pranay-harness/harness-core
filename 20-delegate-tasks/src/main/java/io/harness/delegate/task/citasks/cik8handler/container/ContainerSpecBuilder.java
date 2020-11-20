package io.harness.delegate.task.citasks.cik8handler.container;

/**
 *  Generates minimal K8 container spec based on container parameters provided to it.
 */

import com.google.inject.Singleton;

import io.harness.delegate.beans.ci.pod.ContainerParams;

@Singleton
public class ContainerSpecBuilder extends BaseContainerSpecBuilder {
  protected void decorateSpec(
      ContainerParams containerParams, ContainerSpecBuilderResponse containerSpecBuilderResponse) {
    // Nothing to decorate.
  }
}
