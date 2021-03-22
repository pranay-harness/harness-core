package io.harness.steps.shellScript.manifest;

import io.harness.IdentifierRefProtoUtils;
import io.harness.beans.IdentifierRef;
import io.harness.steps.shellScript.manifest.yaml.HttpStoreConfig;
import io.harness.eventsframework.schemas.entity.EntityDetailProtoDTO;
import io.harness.eventsframework.schemas.entity.EntityTypeProtoEnum;
import io.harness.pms.yaml.ParameterField;
import io.harness.utils.IdentifierRefHelper;
import io.harness.walktree.visitor.entityreference.EntityReferenceExtractor;
import io.harness.walktree.visitor.validation.ConfigValidator;
import io.harness.walktree.visitor.validation.ValidationVisitor;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class HttpStoreVisitorHelper implements ConfigValidator, EntityReferenceExtractor {
  @Override
  public void validate(Object object, ValidationVisitor visitor) {}

  @Override
  public Object createDummyVisitableElement(Object originalElement) {
    return HttpStoreConfig.builder().build();
  }

  @Override
  public Set<EntityDetailProtoDTO> addReference(Object object, String accountIdentifier, String orgIdentifier,
      String projectIdentifier, Map<String, Object> contextMap) {
    HttpStoreConfig helmHttpStore = (HttpStoreConfig) object;
    Set<EntityDetailProtoDTO> result = new HashSet<>();
    if (ParameterField.isNull(helmHttpStore.getConnectorRef())) {
      return result;
    }

    if (!helmHttpStore.getConnectorRef().isExpression()) {
      String connectorRefString = helmHttpStore.getConnectorRef().getValue();
      IdentifierRef identifierRef =
          IdentifierRefHelper.getIdentifierRef(connectorRefString, accountIdentifier, orgIdentifier, projectIdentifier);
      EntityDetailProtoDTO entityDetail =
          EntityDetailProtoDTO.newBuilder()
              .setIdentifierRef(IdentifierRefProtoUtils.createIdentifierRefProtoFromIdentifierRef(identifierRef))
              .setType(EntityTypeProtoEnum.CONNECTORS)
              .build();
      result.add(entityDetail);
    }
    return result;
  }
}
