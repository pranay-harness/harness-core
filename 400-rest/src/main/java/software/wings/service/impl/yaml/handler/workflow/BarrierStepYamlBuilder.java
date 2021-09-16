package software.wings.service.impl.yaml.handler.workflow;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;

import software.wings.beans.yaml.ChangeContext;
import software.wings.yaml.workflow.StepYaml;

@OwnedBy(CDC)
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
public class BarrierStepYamlBuilder extends StepYamlBuilder {
  private static final String IDENTIFIER = "identifier";

  @Override
  public void validate(ChangeContext<StepYaml> changeContext) {
    StepYaml stepYaml = changeContext.getYaml();
    if (stepYaml.getProperties() != null) {
      String barrierStepIdentifier = (String) stepYaml.getProperties().get(IDENTIFIER);
      if (EmptyPredicate.isEmpty(barrierStepIdentifier)) {
        throw new InvalidRequestException("Barrier step Identifier cannot be empty");
      }
    }
  }
}
