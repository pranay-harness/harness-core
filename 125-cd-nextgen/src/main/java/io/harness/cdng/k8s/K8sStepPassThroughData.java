package io.harness.cdng.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.infra.beans.InfrastructureOutcome;
import io.harness.cdng.manifest.yaml.ManifestOutcome;
import io.harness.cdng.manifest.yaml.OpenshiftParamManifestOutcome;
import io.harness.cdng.manifest.yaml.ValuesManifestOutcome;
import io.harness.pms.sdk.core.steps.io.PassThroughData;

import java.util.List;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(CDP)
@Value
@Builder
@TypeAlias("k8sStepPassThroughData")
public class K8sStepPassThroughData implements PassThroughData {
  ManifestOutcome k8sManifestOutcome;
  List<ValuesManifestOutcome> valuesManifestOutcomes;
  List<OpenshiftParamManifestOutcome> openshiftParamManifestOutcomes;
  InfrastructureOutcome infrastructure;
  String helmValuesFileContent;
}
