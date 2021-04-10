package io.harness.ng.core.entitydetail;

import static io.harness.data.structure.HarnessStringUtils.nullIfEmpty;

import io.harness.beans.IdentifierRef;
import io.harness.beans.InputSetReference;
import io.harness.encryption.Scope;
import io.harness.eventsframework.schemas.entity.EntityDetailProtoDTO;
import io.harness.eventsframework.schemas.entity.EntityTypeProtoEnum;
import io.harness.eventsframework.schemas.entity.IdentifierRefProtoDTO;
import io.harness.eventsframework.schemas.entity.InputSetReferenceProtoDTO;
import io.harness.eventsframework.schemas.entity.ScopeProtoEnum;
import io.harness.exception.UnknownEnumTypeException;
import io.harness.ng.core.EntityDetail;
import io.harness.ng.core.event.EntityToEntityProtoHelper;

import com.google.inject.Singleton;
import com.google.protobuf.StringValue;

@Singleton
public class EntityDetailRestToProtoMapper {
  public EntityDetailProtoDTO createEntityDetailDTO(EntityDetail entityDetail) {
    final EntityTypeProtoEnum entityTypeProto =
        EntityToEntityProtoHelper.getEntityTypeFromProto(entityDetail.getType());
    final EntityDetailProtoDTO.Builder entityDetailProtoBuilder =
        EntityDetailProtoDTO.newBuilder().setName(entityDetail.getName()).setType(entityTypeProto);
    if (entityDetail.getEntityRef() instanceof IdentifierRef) {
      return entityDetailProtoBuilder.setIdentifierRef(createIdentifierRef((IdentifierRef) entityDetail.getEntityRef()))
          .build();
    } else {
      return entityDetailProtoBuilder.setInputSetRef(createInputSetRef((InputSetReference) entityDetail.getEntityRef()))
          .build();
    }
  }

  private IdentifierRefProtoDTO createIdentifierRef(IdentifierRef identifierRef) {
    return IdentifierRefProtoDTO.newBuilder()
        .setAccountIdentifier(StringValue.newBuilder().setValue(identifierRef.getAccountIdentifier()).build())
        .setOrgIdentifier(StringValue.of(nullIfEmpty(identifierRef.getOrgIdentifier())))
        .setProjectIdentifier(StringValue.of(nullIfEmpty(identifierRef.getProjectIdentifier())))
        .setScope(mapToScopeProtoEnum(identifierRef.getScope()))
        .setIdentifier(StringValue.of(identifierRef.getIdentifier()))
        .build();
  }

  private ScopeProtoEnum mapToScopeProtoEnum(Scope scope) {
    switch (scope) {
      case ACCOUNT:
        return ScopeProtoEnum.ACCOUNT;
      case ORG:
        return ScopeProtoEnum.ORG;
      case PROJECT:
        return ScopeProtoEnum.PROJECT;
      case UNKNOWN:
        return ScopeProtoEnum.UNKNOWN;
      default:
        throw new UnknownEnumTypeException("scope", String.valueOf(scope));
    }
  }

  private InputSetReferenceProtoDTO createInputSetRef(InputSetReference inputSetReference) {
    return InputSetReferenceProtoDTO.newBuilder()
        .setAccountIdentifier(StringValue.of(inputSetReference.getAccountIdentifier()))
        .setIdentifier(StringValue.of(inputSetReference.getIdentifier()))
        .setOrgIdentifier(StringValue.of(nullIfEmpty(inputSetReference.getOrgIdentifier())))
        .setProjectIdentifier(StringValue.of(nullIfEmpty(inputSetReference.getProjectIdentifier())))
        .setPipelineIdentifier(StringValue.of(inputSetReference.getPipelineIdentifier()))
        .build();
  }
}
