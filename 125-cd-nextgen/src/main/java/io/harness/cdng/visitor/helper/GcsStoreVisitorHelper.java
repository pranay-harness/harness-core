package io.harness.cdng.visitor.helper;

import static io.harness.yaml.core.LevelNodeQualifierName.PATH_CONNECTOR;

import io.harness.IdentifierRefProtoUtils;
import io.harness.beans.IdentifierRef;
import io.harness.cdng.manifest.yaml.GcsStoreConfig;
import io.harness.cdng.visitor.YamlTypes;
import io.harness.eventsframework.schemas.entity.EntityDetailProtoDTO;
import io.harness.eventsframework.schemas.entity.EntityTypeProtoEnum;
import io.harness.pms.sdk.preflight.PreFlightCheckMetadata;
import io.harness.pms.yaml.ParameterField;
import io.harness.utils.IdentifierRefHelper;
import io.harness.walktree.visitor.entityreference.EntityReferenceExtractor;
import io.harness.walktree.visitor.utilities.VisitorParentPathUtils;
import io.harness.walktree.visitor.validation.ConfigValidator;
import io.harness.walktree.visitor.validation.ValidationVisitor;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class GcsStoreVisitorHelper implements ConfigValidator, EntityReferenceExtractor {
  @Override
  public void validate(Object object, ValidationVisitor visitor) {
    // Nothing to validate.
  }

  @Override
  public Object createDummyVisitableElement(Object originalElement) {
    return GcsStoreConfig.builder().build();
  }

  @Override
  public Set<EntityDetailProtoDTO> addReference(Object object, String accountIdentifier, String orgIdentifier,
      String projectIdentifier, Map<String, Object> contextMap) {
    GcsStoreConfig gcsStoreConfig = (GcsStoreConfig) object;

    Set<EntityDetailProtoDTO> result = new HashSet<>();
    if (ParameterField.isNull(gcsStoreConfig.getConnectorRef())) {
      return result;
    }

    String fullQualifiedDomainName =
        VisitorParentPathUtils.getFullQualifiedDomainName(contextMap) + PATH_CONNECTOR + YamlTypes.CONNECTOR_REF;
    Map<String, String> metadata =
        new HashMap<>(Collections.singletonMap(PreFlightCheckMetadata.FQN, fullQualifiedDomainName));

    if (!gcsStoreConfig.getConnectorRef().isExpression()) {
      String connectorRefString = gcsStoreConfig.getConnectorRef().getValue();
      IdentifierRef identifierRef = IdentifierRefHelper.getIdentifierRef(
          connectorRefString, accountIdentifier, orgIdentifier, projectIdentifier, metadata);
      EntityDetailProtoDTO entityDetail =
          EntityDetailProtoDTO.newBuilder()
              .setIdentifierRef(IdentifierRefProtoUtils.createIdentifierRefProtoFromIdentifierRef(identifierRef))
              .setType(EntityTypeProtoEnum.CONNECTORS)
              .build();
      result.add(entityDetail);
    } else {
      metadata.put(PreFlightCheckMetadata.EXPRESSION, gcsStoreConfig.getConnectorRef().getExpressionValue());
      IdentifierRef identifierRef = IdentifierRefHelper.createIdentifierRefWithUnknownScope(accountIdentifier,
          orgIdentifier, projectIdentifier, gcsStoreConfig.getConnectorRef().getExpressionValue(), metadata);
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
