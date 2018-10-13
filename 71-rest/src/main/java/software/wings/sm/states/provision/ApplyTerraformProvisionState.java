package software.wings.sm.states.provision;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.sm.StateType;

public class ApplyTerraformProvisionState extends TerraformProvisionState {
  private static final Logger logger = LoggerFactory.getLogger(ApplyTerraformProvisionState.class);

  public static final String COMMAND_UNIT = "Apply";

  public ApplyTerraformProvisionState(String name) {
    super(name, StateType.TERRAFORM_PROVISION.name());
  }

  protected String commandUnit() {
    return COMMAND_UNIT;
  }
}
