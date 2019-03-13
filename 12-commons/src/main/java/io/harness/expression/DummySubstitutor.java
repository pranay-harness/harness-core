package io.harness.expression;

import org.apache.commons.text.StrLookup;
import org.apache.commons.text.StrSubstitutor;

public class DummySubstitutor {
  static class DummyStrLookup extends StrLookup {
    @Override
    public String lookup(String s) {
      return "dummy";
    }
  }

  public static String substitute(String expression) {
    final DummyStrLookup variableResolver = new DummyStrLookup();

    StrSubstitutor substitutor = new StrSubstitutor();
    substitutor.setEnableSubstitutionInVariables(true);
    substitutor.setVariableResolver(variableResolver);

    return substitutor.replace(expression);
  }
}
