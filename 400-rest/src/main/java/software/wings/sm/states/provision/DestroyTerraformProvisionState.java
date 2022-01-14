package software.wings.sm.states.provision;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.task.terraform.TerraformCommand;
import io.harness.delegate.task.terraform.TerraformCommandUnit;

import software.wings.sm.StateType;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(CDP)
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
public class DestroyTerraformProvisionState extends TerraformProvisionState {
  public DestroyTerraformProvisionState(String name) {
    super(name, StateType.TERRAFORM_DESTROY.name());
  }

  @Override
  protected TerraformCommandUnit commandUnit() {
    return TerraformCommandUnit.Destroy;
  }

  @Override
  protected TerraformCommand command() {
    return TerraformCommand.DESTROY;
  }
}
