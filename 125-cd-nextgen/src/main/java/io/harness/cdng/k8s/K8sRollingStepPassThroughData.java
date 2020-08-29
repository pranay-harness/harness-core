package io.harness.cdng.k8s;

import io.harness.cdng.infra.yaml.Infrastructure;
import io.harness.cdng.manifest.yaml.kinds.K8sManifest;
import io.harness.cdng.manifest.yaml.kinds.ValuesManifest;
import io.harness.facilitator.PassThroughData;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class K8sRollingStepPassThroughData implements PassThroughData {
  private K8sManifest k8sManifest;
  private List<ValuesManifest> valuesManifests;
  private Infrastructure infrastructure;
}
