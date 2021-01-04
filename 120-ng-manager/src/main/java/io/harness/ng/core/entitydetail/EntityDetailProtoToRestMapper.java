package io.harness.ng.core.entitydetail;

import static io.harness.data.structure.HarnessStringUtils.nullIfEmpty;
import static io.harness.eventsframework.schemas.entity.EntityTypeProtoEnum.CONNECTORS;
import static io.harness.eventsframework.schemas.entity.EntityTypeProtoEnum.INPUT_SETS;
import static io.harness.eventsframework.schemas.entity.EntityTypeProtoEnum.PIPELINES;
import static io.harness.eventsframework.schemas.entity.EntityTypeProtoEnum.PROJECTS;
import static io.harness.eventsframework.schemas.entity.EntityTypeProtoEnum.SECRETS;
import static io.harness.eventsframework.schemas.entity.EntityTypeProtoEnum.SERVICE;

import io.harness.EntityType;
import io.harness.beans.IdentifierRef;
import io.harness.beans.InputSetReference;
import io.harness.common.EntityReference;
import io.harness.encryption.Scope;
import io.harness.eventsframework.schemas.entity.EntityDetailProtoDTO;
import io.harness.eventsframework.schemas.entity.EntityTypeProtoEnum;
import io.harness.eventsframework.schemas.entity.IdentifierRefProtoDTO;
import io.harness.eventsframework.schemas.entity.InputSetReferenceProtoDTO;
import io.harness.eventsframework.schemas.entity.ScopeProtoEnum;
import io.harness.exception.UnknownEnumTypeException;
import io.harness.ng.core.EntityDetail;

import com.google.inject.Singleton;
import java.util.EnumMap;
import java.util.Map;

@Singleton
public class EntityDetailProtoToRestMapper {
  public EntityDetail createEntityDetailDTO(EntityDetailProtoDTO entityDetail) {
    return EntityDetail.builder()
        .name(entityDetail.getName())
        .entityRef(createEntityReference(entityDetail))
        .type(mapEventToRestEntityType(entityDetail.getType()))
        .build();
  }

  private EntityReference createEntityReference(EntityDetailProtoDTO entityDetail) {
    if (entityDetail.getType() == INPUT_SETS) {
      return createInputSetRef(entityDetail.getInputSetRef());
    } else {
      return createIdentifierRef(entityDetail.getIdentifierRef());
    }
  }

  private EntityReference createIdentifierRef(IdentifierRefProtoDTO identifierRef) {
    return IdentifierRef.builder()
        .accountIdentifier(identifierRef.getAccountIdentifier().getValue())
        .orgIdentifier(nullIfEmpty(identifierRef.getOrgIdentifier().getValue()))
        .projectIdentifier(nullIfEmpty(identifierRef.getProjectIdentifier().getValue()))
        .scope(mapEventToRestScopeEnum(identifierRef.getScope()))
        .identifier(identifierRef.getIdentifier().getValue())
        .build();
  }

  private Scope mapEventToRestScopeEnum(ScopeProtoEnum scope) {
    switch (scope) {
      case ACCOUNT:
        return Scope.ACCOUNT;
      case ORG:
        return Scope.ORG;
      case PROJECT:
        return Scope.PROJECT;
      case UNRECOGNIZED:
      default:
        throw new UnknownEnumTypeException("scope", String.valueOf(scope));
    }
  }

  private EntityReference createInputSetRef(InputSetReferenceProtoDTO inputSetReference) {
    return InputSetReference.builder()
        .accountIdentifier(inputSetReference.getAccountIdentifier().getValue())
        .identifier(inputSetReference.getIdentifier().getValue())
        .orgIdentifier(nullIfEmpty(inputSetReference.getOrgIdentifier().getValue()))
        .projectIdentifier(nullIfEmpty(inputSetReference.getProjectIdentifier().getValue()))
        .pipelineIdentifier(inputSetReference.getPipelineIdentifier().getValue())
        .build();
  }

  private EntityType mapEventToRestEntityType(EntityTypeProtoEnum type) {
    Map<EntityTypeProtoEnum, EntityType> mappingBetweenProtoAndActualEnum = getEntityTypeProtoEnumToRestEnumMap();
    if (mappingBetweenProtoAndActualEnum.containsKey(type)) {
      return mappingBetweenProtoAndActualEnum.get(type);
    } else {
      throw new UnknownEnumTypeException("entityType", String.valueOf(type));
    }
  }

  private Map<EntityTypeProtoEnum, EntityType> getEntityTypeProtoEnumToRestEnumMap() {
    Map<EntityTypeProtoEnum, EntityType> mappingBetweenProtoAndActualEnum = new EnumMap<>(EntityTypeProtoEnum.class);
    mappingBetweenProtoAndActualEnum.put(SECRETS, EntityType.SECRETS);
    mappingBetweenProtoAndActualEnum.put(PROJECTS, EntityType.PROJECTS);
    mappingBetweenProtoAndActualEnum.put(PIPELINES, EntityType.PIPELINES);
    mappingBetweenProtoAndActualEnum.put(SERVICE, EntityType.SERVICE);
    mappingBetweenProtoAndActualEnum.put(CONNECTORS, EntityType.CONNECTORS);
    return mappingBetweenProtoAndActualEnum;
  }
}
