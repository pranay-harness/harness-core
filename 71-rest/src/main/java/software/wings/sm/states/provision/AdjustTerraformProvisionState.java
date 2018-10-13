package software.wings.sm.states.provision;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.delegation.TerraformProvisionParameters.TerraformCommand;
import software.wings.beans.delegation.TerraformProvisionParameters.TerraformCommandUnit;
import software.wings.sm.StateType;

public class AdjustTerraformProvisionState extends TerraformProvisionState {
  private static final Logger logger = LoggerFactory.getLogger(AdjustTerraformProvisionState.class);

  public AdjustTerraformProvisionState(String name) {
    super(name, StateType.TERRAFORM_PROVISION.name());
  }

  protected TerraformCommandUnit commandUnit() {
    return TerraformCommandUnit.Adjust;
  }

  @Override
  protected TerraformCommand command() {
    return null;
  }
}
