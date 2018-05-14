package software.wings.sm.states.provision;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.sm.StateType;

public class AdjustTerraformProvisionState extends TerraformProvisionState {
  private static final Logger logger = LoggerFactory.getLogger(AdjustTerraformProvisionState.class);

  public static final String COMMAND_UNIT = "Adjust";

  public AdjustTerraformProvisionState(String name) {
    super(name, StateType.TERRAFORM_PROVISION.name());
  }

  protected String commandUnit() {
    return COMMAND_UNIT;
  }
}
