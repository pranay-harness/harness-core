package software.wings.sm.states.provision;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.delegation.TerraformProvisionParameters.TerraformCommand;
import software.wings.beans.delegation.TerraformProvisionParameters.TerraformCommandUnit;
import software.wings.sm.StateType;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(CDP)
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
