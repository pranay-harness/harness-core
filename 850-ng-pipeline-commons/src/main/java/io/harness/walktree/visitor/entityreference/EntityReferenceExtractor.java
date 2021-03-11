package io.harness.walktree.visitor.entityreference;

import io.harness.eventsframework.schemas.entity.EntityDetailProtoDTO;
import io.harness.walktree.visitor.DummyVisitableElement;

import java.util.Collections;
import java.util.Set;

public interface EntityReferenceExtractor extends DummyVisitableElement {
  default Set<EntityDetailProtoDTO> addReference(
      Object object, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    return Collections.emptySet();
  }
}
