package io.harness.cdng.visitor.helpers.pipelineinfrastructure;

import io.harness.EntityType;
import io.harness.beans.IdentifierRef;
import io.harness.cdng.infra.yaml.K8SDirectInfrastructure;
import io.harness.ng.core.EntityDetail;
import io.harness.pms.yaml.ParameterField;
import io.harness.utils.IdentifierRefHelper;
import io.harness.walktree.visitor.entityreference.EntityReferenceExtractor;
import io.harness.walktree.visitor.validation.ConfigValidator;
import io.harness.walktree.visitor.validation.ValidationVisitor;

import java.util.HashSet;
import java.util.Set;

public class K8SDirectInfrastructureVisitorHelper implements ConfigValidator, EntityReferenceExtractor {
  @Override
  public void validate(Object object, ValidationVisitor visitor) {
    // Nothing to validate.
  }

  @Override
  public Object createDummyVisitableElement(Object originalElement) {
    return K8SDirectInfrastructure.builder().build();
  }

  @Override
  public Set<EntityDetail> addReference(
      Object object, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    K8SDirectInfrastructure k8SDirectInfrastructure = (K8SDirectInfrastructure) object;
    Set<EntityDetail> result = new HashSet<>();
    if (ParameterField.isNull(k8SDirectInfrastructure.getConnectorRef())) {
      return result;
    }
    if (!k8SDirectInfrastructure.getConnectorRef().isExpression()) {
      String connectorRefString = k8SDirectInfrastructure.getConnectorRef().getValue();
      IdentifierRef identifierRef =
          IdentifierRefHelper.getIdentifierRef(connectorRefString, accountIdentifier, orgIdentifier, projectIdentifier);
      EntityDetail entityDetail = EntityDetail.builder().entityRef(identifierRef).type(EntityType.CONNECTORS).build();
      result.add(entityDetail);
    }

    return result;
  }
}
