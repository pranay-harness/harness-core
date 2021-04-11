package io.harness.cdng.visitor.helpers.artifact;

import static io.harness.yaml.core.LevelNodeQualifierName.PATH_CONNECTOR;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.artifact.bean.yaml.DockerHubArtifactConfig;
import io.harness.cdng.visitor.YamlTypes;
import io.harness.eventsframework.protohelper.IdentifierRefProtoDTOHelper;
import io.harness.eventsframework.schemas.entity.EntityDetailProtoDTO;
import io.harness.filters.FilterCreatorHelper;
import io.harness.pms.yaml.ParameterField;
import io.harness.walktree.visitor.entityreference.EntityReferenceExtractor;
import io.harness.walktree.visitor.utilities.VisitorParentPathUtils;
import io.harness.walktree.visitor.validation.ConfigValidator;
import io.harness.walktree.visitor.validation.ValidationVisitor;

import com.google.inject.Inject;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@OwnedBy(HarnessTeam.CDC)
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
    result.add(FilterCreatorHelper.convertToEntityDetailProtoDTO(accountIdentifier, orgIdentifier, projectIdentifier,
        fullQualifiedDomainName, dockerHubArtifactConfig.getConnectorRef()));
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
