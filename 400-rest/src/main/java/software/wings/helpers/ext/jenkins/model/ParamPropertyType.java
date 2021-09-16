package software.wings.helpers.ext.jenkins.model;

import static io.harness.annotations.dev.HarnessModule._960_API_SERVICES;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

@OwnedBy(HarnessTeam.CDC)
@TargetModule(_960_API_SERVICES)
public enum ParamPropertyType {
  StringParameterDefinition,
  BooleanParameterDefinition,
  CredentialsParameterDefinition,
  ChoiceParameterDefinition,
  TextParameterDefinition
}
