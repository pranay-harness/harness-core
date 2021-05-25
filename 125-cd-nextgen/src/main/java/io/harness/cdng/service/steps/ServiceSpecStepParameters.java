package io.harness.cdng.service.steps;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.artifact.bean.yaml.ArtifactOverrideSets.ArtifactOverrideSetsStepParametersWrapper;
import io.harness.cdng.manifest.yaml.ManifestOverrideSets.ManifestOverrideSetsStepParametersWrapper;
import io.harness.cdng.variables.beans.NGVariableOverrideSetWrapper;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.yaml.ParameterField;
import io.harness.yaml.core.variables.NGVariable;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(HarnessTeam.CDC)
@Value
@Builder
@TypeAlias("serviceSpecStepParameters")
public class ServiceSpecStepParameters implements StepParameters {
  List<NGVariable> originalVariables;
  List<NGVariableOverrideSetWrapper> originalVariableOverrideSets;
  List<NGVariable> stageOverrideVariables;
  ParameterField<List<String>> stageOverridesUseVariableOverrideSets;

  Map<String, ArtifactOverrideSetsStepParametersWrapper> artifactOverrideSets;
  Map<String, ManifestOverrideSetsStepParametersWrapper> manifestOverrideSets;
  List<String> childrenNodeIds;
}
