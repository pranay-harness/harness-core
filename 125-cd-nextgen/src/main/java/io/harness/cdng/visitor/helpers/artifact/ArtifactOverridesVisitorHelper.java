package io.harness.cdng.visitor.helpers.artifact;

import io.harness.cdng.artifact.bean.yaml.ArtifactOverrideSets;
import io.harness.walktree.visitor.DummyVisitableElement;

public class ArtifactOverridesVisitorHelper implements DummyVisitableElement {
  @Override
  public Object createDummyVisitableElement(Object originalElement) {
    ArtifactOverrideSets artifactOverrideSets = (ArtifactOverrideSets) originalElement;
    return ArtifactOverrideSets.builder().identifier(artifactOverrideSets.getIdentifier()).build();
  }
}
