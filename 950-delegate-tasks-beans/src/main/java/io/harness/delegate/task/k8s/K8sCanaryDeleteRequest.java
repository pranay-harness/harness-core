package io.harness.delegate.task.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.logstreaming.CommandUnitsProgress;

import java.util.Collections;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(CDP)
public class K8sCanaryDeleteRequest implements K8sDeployRequest {
  String releaseName;
  String canaryWorkloads;
  K8sInfraDelegateConfig k8sInfraDelegateConfig;

  String commandName;
  Integer timeoutIntervalInMin;
  CommandUnitsProgress commandUnitsProgress;
  boolean useLatestKustomizeVersion;
  boolean useNewKubectlVersion;

  @Override
  public ManifestDelegateConfig getManifestDelegateConfig() {
    return null;
  }

  @Override
  public K8sTaskType getTaskType() {
    return K8sTaskType.CANARY_DELETE;
  }

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
