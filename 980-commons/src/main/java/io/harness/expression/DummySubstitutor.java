/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.expression;

import lombok.experimental.UtilityClass;
import org.apache.commons.text.StrLookup;
import org.apache.commons.text.StrSubstitutor;

@UtilityClass
public class DummySubstitutor {
  public static final String DUMMY_UUID = "CD36671D4E034D3E8732217BD43F9AFA";

  static class DummyStrLookup extends StrLookup {
    @Override
    public String lookup(String expression) {
      return DUMMY_UUID;
    }
  }

  public static String substitute(String expression) {
    DummyStrLookup variableResolver = new DummyStrLookup();

    StrSubstitutor substitutor = new StrSubstitutor();
    substitutor.setEnableSubstitutionInVariables(true);
    substitutor.setVariableResolver(variableResolver);

    return substitutor.replace(expression);
  }
}
