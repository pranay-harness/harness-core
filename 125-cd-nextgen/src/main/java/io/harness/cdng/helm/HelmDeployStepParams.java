package io.harness.cdng.helm;

import io.harness.plancreator.steps.TaskSelectorYaml;
import io.harness.pms.yaml.ParameterField;

import software.wings.beans.command.HelmDummyCommandUnit;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nonnull;
import lombok.Builder;

public class HelmDeployStepParams extends HelmDeployBaseStepInfo implements HelmSpecParameters {
  @Builder(builderMethodName = "infoBuilder")
  public HelmDeployStepParams(ParameterField<List<TaskSelectorYaml>> delegateSelectors, String helmDeployFqn) {
    super(delegateSelectors, helmDeployFqn);
  }

  @Nonnull
  @Override
  @JsonIgnore
  public List<String> getCommandUnits() {
    return Arrays.asList(HelmDummyCommandUnit.FetchFiles, HelmDummyCommandUnit.Init, HelmDummyCommandUnit.Prepare,
        HelmDummyCommandUnit.InstallUpgrade, HelmDummyCommandUnit.WaitForSteadyState, HelmDummyCommandUnit.WrapUp);
  }
}
