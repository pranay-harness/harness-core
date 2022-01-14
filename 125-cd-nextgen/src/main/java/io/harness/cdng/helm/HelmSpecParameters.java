package io.harness.cdng.helm;

import io.harness.plancreator.steps.TaskSelectorYaml;
import io.harness.plancreator.steps.common.SpecParameters;
import io.harness.pms.yaml.ParameterField;

import software.wings.beans.command.HelmDummyCommandUnit;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nonnull;

public interface HelmSpecParameters extends SpecParameters {
  @JsonIgnore ParameterField<List<TaskSelectorYaml>> getDelegateSelectors();

  @Nonnull
  @JsonIgnore
  default List<String> getCommandUnits() {
    return Arrays.asList(HelmDummyCommandUnit.FetchFiles, HelmDummyCommandUnit.Init, HelmDummyCommandUnit.Prepare,
        HelmDummyCommandUnit.InstallUpgrade, HelmDummyCommandUnit.WaitForSteadyState, HelmDummyCommandUnit.WrapUp);
  }
}
