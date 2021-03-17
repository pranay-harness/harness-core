package io.harness.pms.sdk.core.pipeline.filters;

import io.harness.exception.InvalidRequestException;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;
import io.harness.pms.yaml.YamlUtils;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Objects;
import lombok.experimental.UtilityClass;

@UtilityClass
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
}
