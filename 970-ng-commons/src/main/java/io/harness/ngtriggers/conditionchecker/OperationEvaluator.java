package io.harness.ngtriggers.conditionchecker;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(PIPELINE)
public interface OperationEvaluator {
  String EQUALS_OPERATOR = "Equals";
  String NOT_EQUALS_OPERATOR = "NotEquals";
  String STARTS_WITH_OPERATOR = "StartsWith";
  String ENDS_WITH_OPERATOR = "EndsWith";
  String CONTAINS_OPERATOR = "Contains";
  String REGEX_OPERATOR = "Regex";
  String IN_OPERATOR = "In";
  String NOT_IN_OPERATOR = "NotIn";

  boolean evaluate(String input, String standard);
}
