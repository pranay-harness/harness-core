package io.harness.queue;

import static io.harness.data.structure.HasPredicate.hasNone;

import java.util.ArrayList;
import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TopicUtils {
  public static final String DELIMITER = ";";

  public static String combineElements(List<String> topicElements) {
    if (hasNone(topicElements)) {
      return null;
    }
    return String.join(DELIMITER, topicElements);
  }

  public static String appendElements(String prefix, List<String> topicElements) {
    final String combineElements = combineElements(topicElements);
    if (hasNone(prefix)) {
      return combineElements;
    }
    if (hasNone(combineElements)) {
      return prefix;
    }

    return prefix + ";" + combineElements;
  }

  public static String appendElements(String prefix, String suffix) {
    if (hasNone(prefix)) {
      return suffix;
    }
    if (hasNone(suffix)) {
      return prefix;
    }

    return prefix + ";" + suffix;
  }

  public static List<String> resolveExpressionIntoListOfTopics(List<List<String>> topicExpression) {
    if (hasNone(topicExpression)) {
      return null;
    }

    List<String> result = null;
    for (List<String> ors : topicExpression) {
      if (result == null) {
        result = ors;
        continue;
      }

      List<String> newResult = new ArrayList();
      for (String prefix : result) {
        for (String suffix : ors) {
          newResult.add(appendElements(prefix, suffix));
        }
      }

      result = newResult;
    }

    return result;
  }
}
