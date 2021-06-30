package io.harness.delegate.task.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(CDP)
public class K8sExceptionConstants {
  public final String PROVIDE_MASTER_URL_HINT = "Please provide master URL of the kubernetes cluster";
  public final String PROVIDE_MASTER_URL_EXPLANATION = "Master URL not provided";
  public final String INCORRECT_MASTER_URL_HINT =
      "Please provide the correct master URL of the kubernetes cluster. It can be obtained using \"kubectl cluster-info\" cli command";
  public final String INCORRECT_MASTER_URL_EXPLANATION = "Master URL provided is not reachable";
}
