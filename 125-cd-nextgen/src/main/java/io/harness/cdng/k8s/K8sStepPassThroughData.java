package io.harness.cdng.k8s;

import io.harness.cdng.infra.beans.InfrastructureOutcome;
import io.harness.cdng.manifest.yaml.K8sManifestOutcome;
import io.harness.cdng.manifest.yaml.ValuesManifestOutcome;
import io.harness.pms.sdk.core.steps.io.PassThroughData;

import java.util.List;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("k8sStepPassThroughData")
public class K8sStepPassThroughData implements PassThroughData {
  K8sManifestOutcome k8sManifestOutcome;
  List<ValuesManifestOutcome> valuesManifestOutcomes;
  InfrastructureOutcome infrastructure;
}
