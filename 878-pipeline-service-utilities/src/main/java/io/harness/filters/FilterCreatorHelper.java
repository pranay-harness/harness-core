package io.harness.filters;

import io.harness.IdentifierRefProtoUtils;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.IdentifierRef;
import io.harness.encryption.SecretRefData;
import io.harness.eventsframework.schemas.entity.EntityDetailProtoDTO;
import io.harness.eventsframework.schemas.entity.EntityTypeProtoEnum;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.sdk.preflight.PreFlightCheckMetadata;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.PmsYamlUtils;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;
import io.harness.utils.IdentifierRefHelper;

import com.google.common.collect.ImmutableList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(HarnessTeam.PIPELINE)
public class FilterCreatorHelper {
  private final List<String> jexlKeywords = ImmutableList.of("or", "and", "eq", "ne", "lt", "gt", "le", "ge", "div",
      "mod", "not", "null", "true", "false", "new", "var", "return");

  private void checkIfNameIsJexlKeyword(YamlNode variable) {
    String variableName = Objects.requireNonNull(variable.getField(YAMLFieldNameConstants.NAME)).getNode().asText();
    if (jexlKeywords.contains(variableName)) {
      String errorMsg = "Variable name " + variableName + " is a jexl reserved keyword";
      YamlField uuidNode = variable.getField(YAMLFieldNameConstants.UUID);
      if (uuidNode != null) {
        String fqn = PmsYamlUtils.getFullyQualifiedName(uuidNode.getNode());
        errorMsg = errorMsg + ". FQN: " + fqn;
      }
      throw new InvalidRequestException(errorMsg);
    }
  }

  public void checkIfVariableNamesAreValid(YamlField variables) {
    List<YamlNode> variableNodes = variables.getNode().asArray();
    variableNodes.forEach(FilterCreatorHelper::checkIfNameIsJexlKeyword);
  }

  public EntityDetailProtoDTO convertToEntityDetailProtoDTO(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String fullQualifiedDomainName, ParameterField<String> connectorRef,
      EntityTypeProtoEnum entityTypeProtoEnum) {
    Map<String, String> metadata =
        new HashMap<>(Collections.singletonMap(PreFlightCheckMetadata.FQN, fullQualifiedDomainName));
    if (!connectorRef.isExpression()) {
      String connectorRefString = connectorRef.getValue();
      IdentifierRef identifierRef = IdentifierRefHelper.getIdentifierRef(
          connectorRefString, accountIdentifier, orgIdentifier, projectIdentifier, metadata);
      return EntityDetailProtoDTO.newBuilder()
          .setIdentifierRef(IdentifierRefProtoUtils.createIdentifierRefProtoFromIdentifierRef(identifierRef))
          .setType(entityTypeProtoEnum)
          .build();
    } else {
      metadata.put(PreFlightCheckMetadata.EXPRESSION, connectorRef.getExpressionValue());
      IdentifierRef identifierRef = IdentifierRefHelper.createIdentifierRefWithUnknownScope(
          accountIdentifier, orgIdentifier, projectIdentifier, connectorRef.getExpressionValue(), metadata);
      return EntityDetailProtoDTO.newBuilder()
          .setIdentifierRef(IdentifierRefProtoUtils.createIdentifierRefProtoFromIdentifierRef(identifierRef))
          .setType(entityTypeProtoEnum)
          .build();
    }
  }

  public EntityDetailProtoDTO convertSecretToEntityDetailProtoDTO(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String fullQualifiedDomainName, ParameterField<SecretRefData> secretRef) {
    Map<String, String> metadata =
        new HashMap<>(Collections.singletonMap(PreFlightCheckMetadata.FQN, fullQualifiedDomainName));
    if (!secretRef.isExpression()) {
      SecretRefData secretRefData = secretRef.getValue();
      IdentifierRef identifierRef = IdentifierRefHelper.getIdentifierRef(secretRefData.getScope(),
          secretRefData.getIdentifier(), accountIdentifier, orgIdentifier, projectIdentifier, metadata);
      return EntityDetailProtoDTO.newBuilder()
          .setIdentifierRef(IdentifierRefProtoUtils.createIdentifierRefProtoFromIdentifierRef(identifierRef))
          .setType(EntityTypeProtoEnum.SECRETS)
          .build();
    } else {
      metadata.put(PreFlightCheckMetadata.EXPRESSION, secretRef.getExpressionValue());
      IdentifierRef identifierRef = IdentifierRefHelper.createIdentifierRefWithUnknownScope(
          accountIdentifier, orgIdentifier, projectIdentifier, secretRef.getExpressionValue(), metadata);
      return EntityDetailProtoDTO.newBuilder()
          .setIdentifierRef(IdentifierRefProtoUtils.createIdentifierRefProtoFromIdentifierRef(identifierRef))
          .setType(EntityTypeProtoEnum.SECRETS)
          .build();
    }
  }
}
