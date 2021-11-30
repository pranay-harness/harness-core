package io.harness.plancreator.steps;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.plancreator.steps.http.HttpStepNode;

import java.util.HashSet;
import java.util.Set;
import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(HarnessTeam.PIPELINE)
public class StepSchemaUtils {
  public Set<Class<?>> getStepsMovedToNewSchema() {
    Set<Class<?>> steps = new HashSet<>();
    steps.add(HttpStepNode.class);
    return steps;
  }
}
