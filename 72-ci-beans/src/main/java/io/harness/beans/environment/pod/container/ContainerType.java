package io.harness.beans.environment.pod.container;

public enum ContainerType {
  STEP_EXECUTOR(ContainerSource.BUILD_JOB);
  ContainerSource containerSource;

  ContainerType(ContainerSource containerSource) {
    this.containerSource = containerSource;
  }
}

enum ContainerSource { BUILD_JOB, HARNESS_WORKER }