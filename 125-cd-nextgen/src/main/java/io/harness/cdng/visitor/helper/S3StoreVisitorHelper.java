package io.harness.cdng.visitor.helper;

import static io.harness.yaml.core.LevelNodeQualifierName.PATH_CONNECTOR;

import io.harness.IdentifierRefProtoUtils;
import io.harness.beans.IdentifierRef;
import io.harness.cdng.manifest.yaml.S3StoreConfig;
import io.harness.cdng.visitor.YamlTypes;
import io.harness.eventsframework.schemas.entity.EntityDetailProtoDTO;
import io.harness.eventsframework.schemas.entity.EntityTypeProtoEnum;
import io.harness.pms.yaml.ParameterField;
import io.harness.utils.IdentifierRefHelper;
import io.harness.walktree.visitor.entityreference.EntityReferenceExtractor;
import io.harness.walktree.visitor.utilities.VisitorParentPathUtils;
import io.harness.walktree.visitor.validation.ConfigValidator;
import io.harness.walktree.visitor.validation.ValidationVisitor;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class S3StoreVisitorHelper implements ConfigValidator, EntityReferenceExtractor {
  @Override
  public void validate(Object object, ValidationVisitor visitor) {
    // Nothing to validate.
  }

  @Override
  public Object createDummyVisitableElement(Object originalElement) {
    return S3StoreConfig.builder().build();
  }

  @Override
  public Set<EntityDetailProtoDTO> addReference(Object object, String accountIdentifier, String orgIdentifier,
      String projectIdentifier, Map<String, Object> contextMap) {
    S3StoreConfig s3StoreConfig = (S3StoreConfig) object;

    Set<EntityDetailProtoDTO> result = new HashSet<>();
    if (ParameterField.isNull(s3StoreConfig.getConnectorRef())) {
      return result;
    }

    if (!s3StoreConfig.getConnectorRef().isExpression()) {
      String connectorRefString = s3StoreConfig.getConnectorRef().getValue();
      String fullQualifiedDomainName =
          VisitorParentPathUtils.getFullQualifiedDomainName(contextMap) + PATH_CONNECTOR + YamlTypes.CONNECTOR_REF;
      Map<String, String> metadata = Collections.singletonMap("fqn", fullQualifiedDomainName);
      IdentifierRef identifierRef = IdentifierRefHelper.getIdentifierRef(
          connectorRefString, accountIdentifier, orgIdentifier, projectIdentifier, metadata);
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
