/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.sm.states.provision;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.delegation.TerragruntProvisionParameters.TerragruntCommand;
import software.wings.beans.delegation.TerragruntProvisionParameters.TerragruntCommandUnit;
import software.wings.sm.StateType;

@OwnedBy(CDP)
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
public class TerragruntApplyState extends TerragruntProvisionState {
  public TerragruntApplyState(String name) {
    super(name, StateType.TERRAGRUNT_PROVISION.name());
  }

  @Override
  protected TerragruntCommandUnit commandUnit() {
    return TerragruntCommandUnit.Apply;
  }

  @Override
  protected TerragruntCommand command() {
    return TerragruntCommand.APPLY;
  }
}
