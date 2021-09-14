/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.steps;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.schemas.entity.EntityDetailProtoDTO;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.walktree.visitor.SimpleVisitorFactory;
import io.harness.walktree.visitor.entityreference.EntityReferenceExtractorVisitor;

import com.google.inject.Inject;
import java.util.LinkedList;
import java.util.Set;

/**
 * A helper util to extract out all referred entities in a given object
 */
@OwnedBy(HarnessTeam.PIPELINE)
public class EntityReferenceExtractorUtils {
  @Inject SimpleVisitorFactory simpleVisitorFactory;

  public Set<EntityDetailProtoDTO> extractReferredEntities(Ambiance ambiance, Object object) {
    String accountIdentifier = AmbianceUtils.getAccountId(ambiance);
    String orgIdentifier = AmbianceUtils.getOrgIdentifier(ambiance);
    String projectIdentifier = AmbianceUtils.getProjectIdentifier(ambiance);
    EntityReferenceExtractorVisitor visitor = simpleVisitorFactory.obtainEntityReferenceExtractorVisitor(
        accountIdentifier, orgIdentifier, projectIdentifier, new LinkedList<>());
    visitor.walkElementTree(object);
    return visitor.getEntityReferenceSet();
  }
}
