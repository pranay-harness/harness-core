package io.harness.filters;

import io.harness.IdentifierRefProtoUtils;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.IdentifierRef;
import io.harness.eventsframework.schemas.entity.EntityDetailProtoDTO;
import io.harness.eventsframework.schemas.entity.EntityTypeProtoEnum;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.sdk.preflight.PreFlightCheckMetadata;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;
import io.harness.pms.yaml.YamlUtils;
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
        String fqn = YamlUtils.getFullyQualifiedName(uuidNode.getNode());
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
      String projectIdentifier, String fullQualifiedDomainName, ParameterField<String> connectorRef) {
    Map<String, String> metadata =
        new HashMap<>(Collections.singletonMap(PreFlightCheckMetadata.FQN, fullQualifiedDomainName));
    if (!connectorRef.isExpression()) {
      String connectorRefString = connectorRef.getValue();
      IdentifierRef identifierRef = IdentifierRefHelper.getIdentifierRef(
          connectorRefString, accountIdentifier, orgIdentifier, projectIdentifier, metadata);
      return EntityDetailProtoDTO.newBuilder()
          .setIdentifierRef(IdentifierRefProtoUtils.createIdentifierRefProtoFromIdentifierRef(identifierRef))
          .setType(EntityTypeProtoEnum.CONNECTORS)
          .build();
    } else {
      metadata.put(PreFlightCheckMetadata.EXPRESSION, connectorRef.getExpressionValue());
      IdentifierRef identifierRef = IdentifierRefHelper.createIdentifierRefWithUnknownScope(
          accountIdentifier, orgIdentifier, projectIdentifier, connectorRef.getExpressionValue(), metadata);
      return EntityDetailProtoDTO.newBuilder()
          .setIdentifierRef(IdentifierRefProtoUtils.createIdentifierRefProtoFromIdentifierRef(identifierRef))
          .setType(EntityTypeProtoEnum.CONNECTORS)
          .build();
    }
  }
}
