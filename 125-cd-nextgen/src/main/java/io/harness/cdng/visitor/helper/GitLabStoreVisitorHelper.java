package io.harness.cdng.visitor.helper;

import io.harness.IdentifierRefProtoUtils;
import io.harness.beans.IdentifierRef;
import io.harness.cdng.manifest.yaml.GitLabStore;
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

public class GitLabStoreVisitorHelper implements ConfigValidator, EntityReferenceExtractor {
  @Inject private IdentifierRefProtoDTOHelper identifierRefProtoDTOHelper;

  @Override
  public void validate(Object object, ValidationVisitor visitor) {
    // Nothing to validate.
  }

  @Override
  public Object createDummyVisitableElement(Object originalElement) {
    return GitLabStore.builder().build();
  }

  @Override
  public Set<EntityDetailProtoDTO> addReference(
      Object object, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    GitLabStore gitLabStore = (GitLabStore) object;
    Set<EntityDetailProtoDTO> result = new HashSet<>();
    if (ParameterField.isNull(gitLabStore.getConnectorRef())) {
      return result;
    }
    if (!gitLabStore.getConnectorRef().isExpression()) {
      String connectorRefString = gitLabStore.getConnectorRef().getValue();
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
