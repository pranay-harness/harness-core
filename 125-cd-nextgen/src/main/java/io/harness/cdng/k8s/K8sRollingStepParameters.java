package io.harness.cdng.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.k8s.K8sCommandUnitConstants;
import io.harness.plancreator.steps.TaskSelectorYaml;
import io.harness.pms.yaml.ParameterField;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(CDP)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TypeAlias("k8sRollingStepParameters")
@RecasterAlias("io.harness.cdng.k8s.K8sRollingStepParameters")
public class K8sRollingStepParameters extends K8sRollingBaseStepInfo implements K8sSpecParameters {
  @Builder(builderMethodName = "infoBuilder")
  public K8sRollingStepParameters(ParameterField<Boolean> skipDryRun,
      ParameterField<List<TaskSelectorYaml>> delegateSelectors, String canaryStepFqn) {
    super(skipDryRun, delegateSelectors, canaryStepFqn);
  }

  @NotNull
  @Override
  public List<String> getCommandUnits() {
    return new ArrayList<>(Arrays.asList(K8sCommandUnitConstants.FetchFiles, K8sCommandUnitConstants.Init,
            K8sCommandUnitConstants.Prepare, K8sCommandUnitConstants.Apply, K8sCommandUnitConstants.WaitForSteadyState,
            K8sCommandUnitConstants.WrapUp, K8sCommandUnitConstants.Prune));
  }
}
