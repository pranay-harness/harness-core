package io.harness.cdng.k8s;

import io.harness.cdng.infra.beans.InfrastructureOutcome;
import io.harness.cdng.manifest.yaml.kinds.K8sManifest;
import io.harness.cdng.manifest.yaml.kinds.ValuesManifest;
import io.harness.facilitator.PassThroughData;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class K8sRollingStepPassThroughData implements PassThroughData {
  K8sManifest k8sManifest;
  List<ValuesManifest> valuesManifests;
  InfrastructureOutcome infrastructure;
}
