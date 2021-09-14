/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.walktree.visitor.entityreference;

import io.harness.eventsframework.schemas.entity.EntityDetailProtoDTO;
import io.harness.walktree.visitor.DummyVisitableElement;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public interface EntityReferenceExtractor extends DummyVisitableElement {
  default Set<EntityDetailProtoDTO> addReference(Object object, String accountIdentifier, String orgIdentifier,
      String projectIdentifier, Map<String, Object> contextMap) {
    return Collections.emptySet();
  }
}
