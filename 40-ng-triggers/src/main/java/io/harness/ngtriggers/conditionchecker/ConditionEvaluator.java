package io.harness.ngtriggers.conditionchecker;

import static io.harness.exception.WingsException.USER_SRE;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.common.base.Splitter;

import io.harness.common.NGExpressionUtils;
import io.harness.exception.InvalidArgumentsException;
import lombok.experimental.UtilityClass;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@UtilityClass
public class ConditionEvaluator {
  public boolean evaluate(String input, String standard, String operator) {
    OperationEvaluator operationEvaluator = evaluatorMap.get(operator);
    if (operationEvaluator == null) {
      throw new InvalidArgumentsException(
          String.format("Operator [%s] is not supported. Please user valid one", operator), USER_SRE);
    }

    return operationEvaluator.evaluate(input, standard);
  }

  static Map<String, OperationEvaluator> evaluatorMap = new HashMap<>();
  static {
    evaluatorMap.put("equals", new EqualsOperatorEvaluator());
    evaluatorMap.put("not equals", new NotEqualsOperatorEvaluator());
    evaluatorMap.put("in", new INOperatorEvaluator());
    evaluatorMap.put("not in", new NotINOperatorEvaluator());
    evaluatorMap.put("contains", new ContainsOperatorEvaluator());
    evaluatorMap.put("starts with", new StartsWithOperatorEvaluator());
    evaluatorMap.put("ends with", new EndsWithOperatorEvaluator());
    evaluatorMap.put("regex", new RegexOperatorEvaluator());
  }

  static class EqualsOperatorEvaluator implements OperationEvaluator {
    @Override
    public boolean evaluate(String input, String standard) {
      if (isBlank(standard)) {
        return isBlank(input);
      }

      return standard.equals(input);
    }
  }

  static class NotEqualsOperatorEvaluator implements OperationEvaluator {
    @Override
    public boolean evaluate(String input, String standard) {
      // standard is missing, evaluation cant happen
      if (isBlank(standard)) {
        return isNotBlank(input);
      }

      return !standard.equals(input);
    }
  }

  static class StartsWithOperatorEvaluator implements OperationEvaluator {
    @Override
    public boolean evaluate(String input, String standard) {
      // standard is missing, skipping evaluation
      if (isBlank(standard)) {
        return true;
      }

      if (isBlank(input)) {
        return false;
      }

      return input.startsWith(standard);
    }
  }

  static class EndsWithOperatorEvaluator implements OperationEvaluator {
    @Override
    public boolean evaluate(String input, String standard) {
      // standard is missing, skipping evaluation
      if (isBlank(standard)) {
        return true;
      }

      if (isBlank(input)) {
        return false;
      }

      return input.endsWith(standard);
    }
  }

  static class ContainsOperatorEvaluator implements OperationEvaluator {
    @Override
    public boolean evaluate(String input, String standard) {
      // standard is missing, skipping evaluation
      if (isBlank(standard)) {
        return true;
      }

      if (isBlank(input)) {
        return false;
      }

      return input.contains(standard);
    }
  }

  static class INOperatorEvaluator implements OperationEvaluator {
    Set<String> generateAllowedValuesSet(String standard) {
      Set<String> allowedValues = new HashSet<>();
      for (String s : Splitter.on(Pattern.compile(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)")).trimResults().split(standard)) {
        if (s.startsWith("\"") && s.endsWith("\"") && s.length() > 1) {
          String str = s.substring(1, s.length() - 1);
          allowedValues.add(str.replaceAll("\"\"", "\""));
        } else {
          allowedValues.add(s);
        }
      }

      return allowedValues;
    }

    @Override
    public boolean evaluate(String input, String standard) {
      // standard is missing, skipping evaluation
      if (isBlank(standard)) {
        return true;
      }

      Set<String> allowedValuesSet = generateAllowedValuesSet(standard);
      return allowedValuesSet.contains(input);
    }
  }

  static class NotINOperatorEvaluator extends INOperatorEvaluator {
    @Override
    public boolean evaluate(String input, String standard) {
      // standard is missing, skipping evaluation
      if (isBlank(standard)) {
        return true;
      }

      return !super.evaluate(input, standard);
    }
  }

  static class RegexOperatorEvaluator extends INOperatorEvaluator {
    @Override
    public boolean evaluate(String input, String standard) {
      // standard is missing, skipping evaluation
      if (isBlank(standard)) {
        return true;
      }

      return NGExpressionUtils.matchesPattern(Pattern.compile(standard), input);
    }
  }
}
