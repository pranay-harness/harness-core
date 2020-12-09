package io.harness.cdng.visitor.helpers.artifact;

import io.harness.EntityType;
import io.harness.beans.IdentifierRef;
import io.harness.cdng.artifact.bean.yaml.DockerHubArtifactConfig;
import io.harness.ng.core.EntityDetail;
import io.harness.pms.yaml.ParameterField;
import io.harness.utils.IdentifierRefHelper;
import io.harness.walktree.visitor.entityreference.EntityReferenceExtractor;
import io.harness.walktree.visitor.validation.ConfigValidator;
import io.harness.walktree.visitor.validation.ValidationVisitor;

import java.util.HashSet;
import java.util.Set;

public class DockerHubArtifactConfigVisitorHelper implements ConfigValidator, EntityReferenceExtractor {
  @Override
  public void validate(Object object, ValidationVisitor visitor) {
    // Nothing to validate.
  }

  @Override
  public Set<EntityDetail> addReference(
      Object object, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    DockerHubArtifactConfig dockerHubArtifactConfig = (DockerHubArtifactConfig) object;
    Set<EntityDetail> result = new HashSet<>();
    if (ParameterField.isNull(dockerHubArtifactConfig.getConnectorRef())) {
      return result;
    }
    if (!dockerHubArtifactConfig.getConnectorRef().isExpression()) {
      String connectorRefString = dockerHubArtifactConfig.getConnectorRef().getValue();
      IdentifierRef identifierRef =
          IdentifierRefHelper.getIdentifierRef(connectorRefString, accountIdentifier, orgIdentifier, projectIdentifier);
      EntityDetail entityDetail = EntityDetail.builder().entityRef(identifierRef).type(EntityType.CONNECTORS).build();
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
