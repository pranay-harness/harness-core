package io.harness.ng.core.entitysetupusage.mapper;

import static io.harness.eventsframework.schemas.entity.EntityTypeProtoEnum.CONNECTORS;
import static io.harness.eventsframework.schemas.entity.EntityTypeProtoEnum.INPUT_SETS;
import static io.harness.eventsframework.schemas.entity.EntityTypeProtoEnum.PIPELINES;
import static io.harness.eventsframework.schemas.entity.EntityTypeProtoEnum.PROJECTS;
import static io.harness.eventsframework.schemas.entity.EntityTypeProtoEnum.SECRETS;
import static io.harness.eventsframework.schemas.entity.EntityTypeProtoEnum.SERVICE;

import io.harness.EntityType;
import io.harness.common.EntityReference;
import io.harness.encryption.Scope;
import io.harness.eventsframework.schemas.entity.EntityDetailProtoDTO;
import io.harness.eventsframework.schemas.entity.EntityTypeProtoEnum;
import io.harness.eventsframework.schemas.entity.IdentifierRefProtoDTO;
import io.harness.eventsframework.schemas.entity.InputSetReferenceProtoDTO;
import io.harness.eventsframework.schemas.entity.ScopeProtoEnum;
import io.harness.eventsframework.schemas.entitysetupusage.EntitySetupUsageCreateDTO;
import io.harness.exception.UnknownEnumTypeException;
import io.harness.ng.core.EntityDetail;
import io.harness.ng.core.entitysetupusage.dto.EntitySetupUsageDTO;

import com.google.inject.Singleton;
import java.util.EnumMap;
import java.util.Map;

@Singleton
public class EntityEventDTOToRestDTOMapper {
  public EntitySetupUsageDTO toRestDTO(EntitySetupUsageCreateDTO setupUsageEventsDTO) {
    EntityDetail referredEntity = createEntityDetailDTO(setupUsageEventsDTO.getReferredEntity());
    EntityDetail referredByEntity = createEntityDetailDTO(setupUsageEventsDTO.getReferredByEntity());

    return EntitySetupUsageDTO.builder()
        .accountIdentifier(setupUsageEventsDTO.getAccountIdentifier())
        .createdAt(setupUsageEventsDTO.getCreatedAt())
        .referredByEntity(referredByEntity)
        .referredEntity(referredEntity)
        .build();
  }

  private EntityDetail createEntityDetailDTO(EntityDetailProtoDTO entityDetail) {
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
    return io.harness.beans.IdentifierRef.builder()
        .accountIdentifier(identifierRef.getAccountIdentifier().getValue())
        .orgIdentifier(identifierRef.getOrgIdentifier().getValue())
        .projectIdentifier(identifierRef.getProjectIdentifier().getValue())
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
    return io.harness.beans.InputSetReference.builder()
        .accountIdentifier(inputSetReference.getAccountIdentifier().getValue())
        .identifier(inputSetReference.getIdentifier().getValue())
        .orgIdentifier(inputSetReference.getOrgIdentifier().getValue())
        .projectIdentifier(inputSetReference.getProjectIdentifier().getValue())
        .pipelineIdentifier(inputSetReference.getPipelineIdentifier().getValue())
        .build();
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

  private EntityType mapEventToRestEntityType(EntityTypeProtoEnum type) {
    Map<EntityTypeProtoEnum, EntityType> mappingBetweenProtoAndActualEnum = getEntityTypeProtoEnumToRestEnumMap();
    if (mappingBetweenProtoAndActualEnum.containsKey(type)) {
      return mappingBetweenProtoAndActualEnum.get(type);
    } else {
      throw new UnknownEnumTypeException("entityType", String.valueOf(type));
    }
  }
}
