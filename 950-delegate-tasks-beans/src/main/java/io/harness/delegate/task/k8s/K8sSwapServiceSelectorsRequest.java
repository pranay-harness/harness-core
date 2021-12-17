package io.harness.delegate.task.k8s;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.logstreaming.CommandUnitsProgress;

import java.util.Collections;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(HarnessTeam.CDP)
public class K8sSwapServiceSelectorsRequest implements K8sDeployRequest {
  String commandName;
  K8sTaskType taskType;
  K8sInfraDelegateConfig k8sInfraDelegateConfig;
  ManifestDelegateConfig manifestDelegateConfig;
  Integer timeoutIntervalInMin;
  String accountId;
  String service1;
  String service2;
  CommandUnitsProgress commandUnitsProgress;
  boolean useVarSupportForKustomize;
  boolean useNewKubectlVersion;

  @Override
  public List<String> getValuesYamlList() {
    return Collections.emptyList();
  }

  @Override
  public List<String> getKustomizePatchesList() {
    return Collections.emptyList();
  }

  @Override
  public List<String> getOpenshiftParamList() {
    return Collections.emptyList();
  }
}
