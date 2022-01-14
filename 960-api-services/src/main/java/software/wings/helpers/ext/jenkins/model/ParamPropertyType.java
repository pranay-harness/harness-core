package software.wings.helpers.ext.jenkins.model;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.CDC)
public enum ParamPropertyType {
  StringParameterDefinition,
  BooleanParameterDefinition,
  CredentialsParameterDefinition,
  ChoiceParameterDefinition,
  TextParameterDefinition
}
