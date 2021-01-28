package io.harness.cdng.visitor.helpers.pipelineinfrastructure;

import io.harness.IdentifierRefProtoUtils;
import io.harness.beans.IdentifierRef;
import io.harness.cdng.infra.yaml.K8SDirectInfrastructure;
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

public class K8SDirectInfrastructureVisitorHelper implements ConfigValidator, EntityReferenceExtractor {
  @Inject private IdentifierRefProtoDTOHelper identifierRefProtoDTOHelper;

  @Override
  public void validate(Object object, ValidationVisitor visitor) {
    // Nothing to validate.
  }

  @Override
  public Object createDummyVisitableElement(Object originalElement) {
    return K8SDirectInfrastructure.builder().build();
  }

  @Override
  public Set<EntityDetailProtoDTO> addReference(
      Object object, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    K8SDirectInfrastructure k8SDirectInfrastructure = (K8SDirectInfrastructure) object;
    Set<EntityDetailProtoDTO> result = new HashSet<>();
    if (ParameterField.isNull(k8SDirectInfrastructure.getConnectorRef())) {
      return result;
    }
    if (!k8SDirectInfrastructure.getConnectorRef().isExpression()) {
      String connectorRefString = k8SDirectInfrastructure.getConnectorRef().getValue();
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
