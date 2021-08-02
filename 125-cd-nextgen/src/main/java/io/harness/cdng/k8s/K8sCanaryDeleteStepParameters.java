package io.harness.cdng.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.ParameterField;
import io.harness.k8s.K8sCommandUnitConstants;
import io.harness.plancreator.steps.TaskSelectorYaml;

import java.util.Arrays;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(CDP)
@Data
@NoArgsConstructor
@EqualsAndHashCode
@TypeAlias("k8sCanaryDeleteParameters")
public class K8sCanaryDeleteStepParameters extends K8sCanaryDeleteStepInfo implements K8sSpecParameters {
  @Builder(builderMethodName = "infoBuilder")
  public K8sCanaryDeleteStepParameters(ParameterField<Boolean> skipDryRun,
      ParameterField<List<TaskSelectorYaml>> delegateSelectors, String canaryStepFqn, String canaryDeleteStepFqn) {
    this.skipDryRun = skipDryRun;
    this.delegateSelectors = delegateSelectors;
    this.canaryStepFqn = canaryStepFqn;
    this.canaryDeleteStepFqn = canaryDeleteStepFqn;
  }

  @Override
  public List<String> getCommandUnits() {
    return Arrays.asList(K8sCommandUnitConstants.Init, K8sCommandUnitConstants.Delete);
  }
}
