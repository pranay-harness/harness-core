package io.harness.steps;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.walktree.visitor.DummyVisitableElement;

@OwnedBy(PIPELINE)
public class VisitorTestParentVisitorHelper implements DummyVisitableElement {
  @Override
  public Object createDummyVisitableElement(Object originalElement) {
    return VisitorTestParent.builder().build();
  }
}
