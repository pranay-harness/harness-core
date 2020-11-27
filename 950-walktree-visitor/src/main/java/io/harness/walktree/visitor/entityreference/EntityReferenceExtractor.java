package io.harness.walktree.visitor.entityreference;

import io.harness.ng.core.EntityDetail;
import io.harness.walktree.visitor.DummyVisitableElement;

import java.util.Collections;
import java.util.Set;

public interface EntityReferenceExtractor extends DummyVisitableElement {
  default Set<EntityDetail> addReference(
      Object object, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    return Collections.emptySet();
  }
}
