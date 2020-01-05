package software.wings.sm.states.provision;

import lombok.extern.slf4j.Slf4j;
import software.wings.beans.delegation.TerraformProvisionParameters.TerraformCommand;
import software.wings.beans.delegation.TerraformProvisionParameters.TerraformCommandUnit;
import software.wings.sm.StateType;

@Slf4j
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
