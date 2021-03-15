package io.harness.cdng.visitor.helper;

import static io.harness.yaml.core.LevelNodeQualifierName.PATH_CONNECTOR;

import io.harness.IdentifierRefProtoUtils;
import io.harness.beans.IdentifierRef;
import io.harness.cdng.manifest.yaml.GithubStore;
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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class GithubStoreVisitorHelper implements ConfigValidator, EntityReferenceExtractor {
  @Inject private IdentifierRefProtoDTOHelper identifierRefProtoDTOHelper;

  @Override
  public void validate(Object object, ValidationVisitor visitor) {
    // Nothing to validate.
  }

  @Override
  public Object createDummyVisitableElement(Object originalElement) {
    return GithubStore.builder().build();
  }

  @Override
  public Set<EntityDetailProtoDTO> addReference(Object object, String accountIdentifier, String orgIdentifier,
      String projectIdentifier, Map<String, Object> contextMap) {
    GithubStore githubStore = (GithubStore) object;
    Set<EntityDetailProtoDTO> result = new HashSet<>();
    if (ParameterField.isNull(githubStore.getConnectorRef())) {
      return result;
    }
    if (!githubStore.getConnectorRef().isExpression()) {
      String connectorRefString = githubStore.getConnectorRef().getValue();
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
