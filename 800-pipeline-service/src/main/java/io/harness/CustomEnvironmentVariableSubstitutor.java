package io.harness;

import org.apache.commons.text.StrSubstitutor;

public class CustomEnvironmentVariableSubstitutor extends StrSubstitutor {
  public CustomEnvironmentVariableSubstitutor() {
    this(true, false);
  }

  public CustomEnvironmentVariableSubstitutor(boolean strict) {
    this(strict, false);
  }

  public CustomEnvironmentVariableSubstitutor(boolean strict, boolean substitutionInVariables) {
    super(new CustomEnvironmentVariableLookup(strict));
    this.setEnableSubstitutionInVariables(substitutionInVariables);
  }
}
