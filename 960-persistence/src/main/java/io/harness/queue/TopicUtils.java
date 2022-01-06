/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.queue;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import java.util.ArrayList;
import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TopicUtils {
  public static final String DELIMITER = ";";

  public static String combineElements(List<String> topicElements) {
    if (isEmpty(topicElements)) {
      return null;
    }
    return String.join(DELIMITER, topicElements);
  }

  public static String appendElements(String prefix, List<String> topicElements) {
    final String combineElements = combineElements(topicElements);
    if (isEmpty(prefix)) {
      return combineElements;
    }
    if (isEmpty(combineElements)) {
      return prefix;
    }

    return prefix + ";" + combineElements;
  }

  public static String appendElements(String prefix, String suffix) {
    if (isEmpty(prefix)) {
      return suffix;
    }
    if (isEmpty(suffix)) {
      return prefix;
    }

    return prefix + ";" + suffix;
  }

  public static List<String> resolveExpressionIntoListOfTopics(List<List<String>> topicExpression) {
    if (isEmpty(topicExpression)) {
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
