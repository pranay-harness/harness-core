package io.harness.ngtriggers.conditionchecker;

import static io.harness.ngtriggers.conditionchecker.OperationEvaluator.CONTAINS_OPERATOR;
import static io.harness.ngtriggers.conditionchecker.OperationEvaluator.ENDS_WITH_OPERATOR;
import static io.harness.ngtriggers.conditionchecker.OperationEvaluator.EQUALS_OPERATOR;
import static io.harness.ngtriggers.conditionchecker.OperationEvaluator.IN_OPERATOR;
import static io.harness.ngtriggers.conditionchecker.OperationEvaluator.NOT_EQUALS_OPERATOR;
import static io.harness.ngtriggers.conditionchecker.OperationEvaluator.NOT_IN_OPERATOR;
import static io.harness.ngtriggers.conditionchecker.OperationEvaluator.REGEX_OPERATOR;
import static io.harness.ngtriggers.conditionchecker.OperationEvaluator.STARTS_WITH_OPERATOR;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.annotation.TypeAlias;

@TypeAlias("operator")
public enum ConditionOperator {
  @JsonProperty(IN_OPERATOR) IN(IN_OPERATOR),
  @JsonProperty(EQUALS_OPERATOR) EQUALS(EQUALS_OPERATOR),
  @JsonProperty(NOT_EQUALS_OPERATOR) NOT_EQUALS(NOT_EQUALS_OPERATOR),
  @JsonProperty(NOT_IN_OPERATOR) NOT_IN(NOT_IN_OPERATOR),
  @JsonProperty(REGEX_OPERATOR) REGEX(REGEX_OPERATOR),
  @JsonProperty(ENDS_WITH_OPERATOR) ENDS_WITH(ENDS_WITH_OPERATOR),
  @JsonProperty(STARTS_WITH_OPERATOR) STARTS_WITH(STARTS_WITH_OPERATOR),
  @JsonProperty(CONTAINS_OPERATOR) CONTAINS(CONTAINS_OPERATOR);

  private String value;

  ConditionOperator(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}
