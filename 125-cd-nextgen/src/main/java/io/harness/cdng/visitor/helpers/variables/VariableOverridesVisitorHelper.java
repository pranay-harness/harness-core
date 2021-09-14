/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cdng.visitor.helpers.variables;

import io.harness.cdng.variables.beans.NGVariableOverrideSets;
import io.harness.walktree.visitor.DummyVisitableElement;

public class VariableOverridesVisitorHelper implements DummyVisitableElement {
  @Override
  public Object createDummyVisitableElement(Object originalElement) {
    NGVariableOverrideSets element = (NGVariableOverrideSets) originalElement;
    return NGVariableOverrideSets.builder().identifier(element.getIdentifier()).build();
  }
}
