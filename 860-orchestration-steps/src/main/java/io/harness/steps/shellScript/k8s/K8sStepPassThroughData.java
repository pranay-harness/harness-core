package io.harness.steps.shellScript.k8s;

import io.harness.steps.shellScript.beans.InfrastructureOutcome;
import io.harness.steps.shellScript.manifest.yaml.ManifestOutcome;
import io.harness.steps.shellScript.manifest.yaml.ValuesManifestOutcome;
import io.harness.pms.sdk.core.steps.io.PassThroughData;

import java.util.List;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("k8sStepPassThroughData")
public class K8sStepPassThroughData implements PassThroughData {
  ManifestOutcome k8sManifestOutcome;
  List<ValuesManifestOutcome> valuesManifestOutcomes;
  InfrastructureOutcome infrastructure;
}
