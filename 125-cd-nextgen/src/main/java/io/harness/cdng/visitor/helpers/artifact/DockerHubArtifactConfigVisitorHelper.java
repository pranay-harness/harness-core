package io.harness.cdng.visitor.helpers.artifact;

import static io.harness.yaml.core.LevelNodeQualifierName.PATH_CONNECTOR;

import io.harness.IdentifierRefProtoUtils;
import io.harness.beans.IdentifierRef;
import io.harness.cdng.artifact.bean.yaml.DockerHubArtifactConfig;
import io.harness.cdng.visitor.YamlTypes;
import io.harness.eventsframework.protohelper.IdentifierRefProtoDTOHelper;
import io.harness.eventsframework.schemas.entity.EntityDetailProtoDTO;
import io.harness.eventsframework.schemas.entity.EntityTypeProtoEnum;
import io.harness.pms.yaml.ParameterField;
import io.harness.utils.IdentifierRefHelper;
import io.harness.walktree.visitor.entityreference.EntityReferenceExtractor;
import io.harness.walktree.visitor.utilities.VisitorParentPathUtils;
import io.harness.walktree.visitor.validation.ConfigValidator;
import io.harness.walktree.visitor.validation.ValidationVisitor;

import com.google.inject.Inject;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DockerHubArtifactConfigVisitorHelper implements ConfigValidator, EntityReferenceExtractor {
  @Inject private IdentifierRefProtoDTOHelper identifierRefProtoDTOHelper;
  @Override
  public void validate(Object object, ValidationVisitor visitor) {
    // Nothing to validate.
  }

  @Override
  public Set<EntityDetailProtoDTO> addReference(Object object, String accountIdentifier, String orgIdentifier,
      String projectIdentifier, Map<String, Object> contextMap) {
    DockerHubArtifactConfig dockerHubArtifactConfig = (DockerHubArtifactConfig) object;
    Set<EntityDetailProtoDTO> result = new HashSet<>();
    if (ParameterField.isNull(dockerHubArtifactConfig.getConnectorRef())) {
      return result;
    }
    String fullQualifiedDomainName =
        VisitorParentPathUtils.getFullQualifiedDomainName(contextMap) + PATH_CONNECTOR + YamlTypes.CONNECTOR_REF;
    Map<String, String> metadata = new HashMap<>(Collections.singletonMap("fqn", fullQualifiedDomainName));
    if (!dockerHubArtifactConfig.getConnectorRef().isExpression()) {
      String connectorRefString = dockerHubArtifactConfig.getConnectorRef().getValue();
      IdentifierRef identifierRef = IdentifierRefHelper.getIdentifierRef(
          connectorRefString, accountIdentifier, orgIdentifier, projectIdentifier, metadata);
      EntityDetailProtoDTO entityDetail =
          EntityDetailProtoDTO.newBuilder()
              .setIdentifierRef(IdentifierRefProtoUtils.createIdentifierRefProtoFromIdentifierRef(identifierRef))
              .setType(EntityTypeProtoEnum.CONNECTORS)
              .build();
      result.add(entityDetail);
    } else {
      metadata.put("expression", dockerHubArtifactConfig.getConnectorRef().getExpressionValue());
      IdentifierRef identifierRef = IdentifierRefHelper.createIdentifierRefWithUnknownScope(accountIdentifier,
          orgIdentifier, projectIdentifier, dockerHubArtifactConfig.getConnectorRef().getExpressionValue(), metadata);
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
  public Object createDummyVisitableElement(Object originalElement) {
    DockerHubArtifactConfig dockerHubArtifactConfig = (DockerHubArtifactConfig) originalElement;
    return DockerHubArtifactConfig.builder()
        .identifier(dockerHubArtifactConfig.getIdentifier())
        .primaryArtifact(dockerHubArtifactConfig.isPrimaryArtifact())
        .build();
  }
}
