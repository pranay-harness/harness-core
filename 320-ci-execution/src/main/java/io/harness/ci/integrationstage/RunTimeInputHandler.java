package io.harness.ci.integrationstage;

import io.harness.pms.yaml.ParameterField;

import lombok.experimental.UtilityClass;

@UtilityClass
public class RunTimeInputHandler {
  public boolean resolveGitClone(ParameterField<Boolean> cloneRepository) {
    if (cloneRepository == null || cloneRepository.isExpression() || cloneRepository.getValue() == null) {
      return false;
    } else {
      return (boolean) cloneRepository.fetchFinalValue();
    }
  }
}
