package io.harness.ngtriggers.conditionchecker;

public interface OperationEvaluator {
  String EQUALS_OPERATOR = "equals";
  String NOT_EQUALS_OPERATOR = "not equals";
  String STARTS_WITH_OPERATOR = "starts with";
  String ENDS_WITH_OPERATOR = "ends with";
  String CONTAINS_OPERATOR = "contains";
  String REGEX_OPERATOR = "regex";
  String IN_OPERATOR = "in";
  String NOT_IN_OPERATOR = "not in";

  boolean evaluate(String input, String standard);
}
