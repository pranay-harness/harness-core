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
