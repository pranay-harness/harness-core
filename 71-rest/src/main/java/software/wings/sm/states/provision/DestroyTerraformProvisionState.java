package software.wings.sm.states.provision;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.delegation.TerraformProvisionParameters.TerraformCommand;
import software.wings.beans.delegation.TerraformProvisionParameters.TerraformCommandUnit;
import software.wings.sm.StateType;

public class DestroyTerraformProvisionState extends TerraformProvisionState {
  private static final Logger logger = LoggerFactory.getLogger(DestroyTerraformProvisionState.class);

  public DestroyTerraformProvisionState(String name) {
    super(name, StateType.TERRAFORM_DESTROY.name());
  }

  protected TerraformCommandUnit commandUnit() {
    return TerraformCommandUnit.Destroy;
  }

  @Override
  protected TerraformCommand command() {
    return TerraformCommand.DESTROY;
  }
}
