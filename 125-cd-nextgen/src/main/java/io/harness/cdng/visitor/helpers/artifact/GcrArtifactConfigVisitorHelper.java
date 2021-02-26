package io.harness.cdng.visitor.helpers.artifact;

import io.harness.IdentifierRefProtoUtils;
import io.harness.beans.IdentifierRef;
import io.harness.cdng.artifact.bean.yaml.GcrArtifactConfig;
import io.harness.eventsframework.protohelper.IdentifierRefProtoDTOHelper;
import io.harness.eventsframework.schemas.entity.EntityDetailProtoDTO;
import io.harness.eventsframework.schemas.entity.EntityTypeProtoEnum;
import io.harness.pms.yaml.ParameterField;
import io.harness.utils.IdentifierRefHelper;
import io.harness.walktree.visitor.entityreference.EntityReferenceExtractor;
import io.harness.walktree.visitor.validation.ConfigValidator;
import io.harness.walktree.visitor.validation.ValidationVisitor;

import com.google.inject.Inject;
import java.util.HashSet;
import java.util.Set;

public class GcrArtifactConfigVisitorHelper implements ConfigValidator, EntityReferenceExtractor {
  @Inject private IdentifierRefProtoDTOHelper identifierRefProtoDTOHelper;

  @Override
  public Set<EntityDetailProtoDTO> addReference(
      Object object, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    GcrArtifactConfig gcrArtifactConfig = (GcrArtifactConfig) object;
    Set<EntityDetailProtoDTO> result = new HashSet<>();
    if (ParameterField.isNull(gcrArtifactConfig.getConnectorRef())) {
      return result;
    }
    if (!gcrArtifactConfig.getConnectorRef().isExpression()) {
      String connectorRefString = gcrArtifactConfig.getConnectorRef().getValue();
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

  @Override
  public void validate(Object object, ValidationVisitor visitor) {
    // Nothing to validate.
  }

  @Override
  public Object createDummyVisitableElement(Object originalElement) {
    GcrArtifactConfig gcrArtifactConfig = (GcrArtifactConfig) originalElement;
    return GcrArtifactConfig.builder()
        .identifier(gcrArtifactConfig.getIdentifier())
        .isPrimaryArtifact(gcrArtifactConfig.isPrimaryArtifact())
        .build();
  }
}
