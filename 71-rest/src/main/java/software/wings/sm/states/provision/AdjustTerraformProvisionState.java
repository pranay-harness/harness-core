package software.wings.sm.states.provision;

import lombok.extern.slf4j.Slf4j;
import software.wings.beans.delegation.TerraformProvisionParameters.TerraformCommand;
import software.wings.beans.delegation.TerraformProvisionParameters.TerraformCommandUnit;
import software.wings.sm.StateType;

@Slf4j
public class AdjustTerraformProvisionState extends TerraformProvisionState {
  public AdjustTerraformProvisionState(String name) {
    super(name, StateType.TERRAFORM_PROVISION.name());
  }

  @Override
  protected TerraformCommandUnit commandUnit() {
    return TerraformCommandUnit.Adjust;
  }

  @Override
  protected TerraformCommand command() {
    return null;
  }
}
