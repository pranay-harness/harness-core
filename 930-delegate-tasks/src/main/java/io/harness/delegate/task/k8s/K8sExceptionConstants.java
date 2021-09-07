package io.harness.delegate.task.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDP)
public final class K8sExceptionConstants {
  public static final String PROVIDE_MASTER_URL_HINT = "Please provide master URL of the kubernetes cluster";
  public static final String PROVIDE_MASTER_URL_EXPLANATION = "Master URL not provided";
  public static final String INCORRECT_MASTER_URL_HINT =
      "Please provide the correct master URL of the kubernetes cluster. It can be obtained using \"kubectl cluster-info\" cli command";
  public static final String INCORRECT_MASTER_URL_EXPLANATION = "Master URL provided is not reachable";

  private K8sExceptionConstants() {
    throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
  }
}
