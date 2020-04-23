package software.wings.beans.ci.pod;

public enum CIContainerType {
  STEP_EXECUTOR(CIContainerSource.BUILD_JOB);
  CIContainerSource ciContainerSource;

  CIContainerType(CIContainerSource ciContainerSource) {
    this.ciContainerSource = ciContainerSource;
  }
}

enum CIContainerSource { BUILD_JOB, HARNESS_WORKER }