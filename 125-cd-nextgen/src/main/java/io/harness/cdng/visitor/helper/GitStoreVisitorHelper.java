package io.harness.cdng.visitor.helper;

import io.harness.EntityType;
import io.harness.beans.IdentifierRef;
import io.harness.cdng.manifest.yaml.GitStore;
import io.harness.ng.core.EntityDetail;
import io.harness.pms.yaml.ParameterField;
import io.harness.utils.IdentifierRefHelper;
import io.harness.walktree.visitor.entityreference.EntityReferenceExtractor;
import io.harness.walktree.visitor.validation.ConfigValidator;
import io.harness.walktree.visitor.validation.ValidationVisitor;

import java.util.HashSet;
import java.util.Set;

public class GitStoreVisitorHelper implements ConfigValidator, EntityReferenceExtractor {
  @Override
  public void validate(Object object, ValidationVisitor visitor) {
    // Nothing to validate.
  }

  @Override
  public Object createDummyVisitableElement(Object originalElement) {
    return GitStore.builder().build();
  }

  @Override
  public Set<EntityDetail> addReference(
      Object object, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    GitStore gitStore = (GitStore) object;
    Set<EntityDetail> result = new HashSet<>();
    if (ParameterField.isNull(gitStore.getConnectorRef())) {
      return result;
    }
    if (!gitStore.getConnectorRef().isExpression()) {
      String connectorRefString = gitStore.getConnectorRef().getValue();
      IdentifierRef identifierRef =
          IdentifierRefHelper.getIdentifierRef(connectorRefString, accountIdentifier, orgIdentifier, projectIdentifier);
      EntityDetail entityDetail = EntityDetail.builder().entityRef(identifierRef).type(EntityType.CONNECTORS).build();
      result.add(entityDetail);
    }
    return result;
  }
}
