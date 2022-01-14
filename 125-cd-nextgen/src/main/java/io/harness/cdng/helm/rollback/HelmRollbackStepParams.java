package io.harness.cdng.helm.rollback;

import io.harness.cdng.helm.HelmSpecParameters;
import io.harness.plancreator.steps.TaskSelectorYaml;
import io.harness.pms.yaml.ParameterField;

import software.wings.beans.command.HelmDummyCommandUnit;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nonnull;
import lombok.Builder;

public class HelmRollbackStepParams extends HelmRollbackBaseStepInfo implements HelmSpecParameters {
  @Builder(builderMethodName = "infoBuilder")
  public HelmRollbackStepParams(ParameterField<List<TaskSelectorYaml>> delegateSelectors, String helmRollbackFqn) {
    super(delegateSelectors, helmRollbackFqn);
  }

  @Nonnull
  @JsonIgnore
  public List<String> getCommandUnits() {
    return Arrays.asList(
        HelmDummyCommandUnit.Init, HelmDummyCommandUnit.Rollback, HelmDummyCommandUnit.WaitForSteadyState);
  }
}
