package io.harness.steps;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.IdentifierRef;
import io.harness.eventsframework.schemas.entity.EntityDetailProtoDTO;
import io.harness.eventsframework.schemas.entity.EntityTypeProtoEnum;
import io.harness.preflight.PreFlightCheckMetadata;
import io.harness.utils.IdentifierRefHelper;
import io.harness.utils.IdentifierRefProtoUtils;
import io.harness.walktree.visitor.entityreference.EntityReferenceExtractor;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

@OwnedBy(PIPELINE)
public class VisitorTestChildVisitorHelper implements EntityReferenceExtractor {
  @Override
  public Object createDummyVisitableElement(Object originalElement) {
    return VisitorTestChild.builder().build();
  }

  @Override
  public Set<EntityDetailProtoDTO> addReference(Object object, String accountIdentifier, String orgIdentifier,
      String projectIdentifier, Map<String, Object> contextMap) {
    IdentifierRef identifierRef = IdentifierRefHelper.getIdentifierRef("reference", accountIdentifier, orgIdentifier,
        projectIdentifier, Collections.singletonMap(PreFlightCheckMetadata.FQN, "fqnValue"));
    EntityDetailProtoDTO entityDetail =
        EntityDetailProtoDTO.newBuilder()
            .setIdentifierRef(IdentifierRefProtoUtils.createIdentifierRefProtoFromIdentifierRef(identifierRef))
            .setType(EntityTypeProtoEnum.CONNECTORS)
            .build();
    return Collections.singleton(entityDetail);
  }
}
