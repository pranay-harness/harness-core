/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

/**
 *
 */

package software.wings.sm;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import static org.apache.commons.lang3.StringUtils.startsWith;

import io.harness.annotations.dev.OwnedBy;
import io.harness.context.ContextElementType;

import java.util.List;
import org.apache.commons.lang3.StringUtils;

/**
 * The Interface ExpressionProcessor.
 *
 * @author Rishi
 */
@OwnedBy(CDC)
public interface ExpressionProcessor {
  /**
   * The constant WILD_CHAR.
   */
  char WILD_CHAR = '*';
  /**
   * The constant EXPRESSION_LIST_SUFFIX.
   */
  String EXPRESSION_LIST_SUFFIX = ".list()";

  /**
   * The constant EXPRESSION_NAME_DELIMITER.
   */
  String EXPRESSION_NAME_DELIMITER = ",";

  /**
   * The constant EXPRESSION_PREFIX.
   */
  String EXPRESSION_PREFIX = "${";

  /**
   * The constant EXPRESSION_SUFFIX.
   */
  String EXPRESSION_SUFFIX = "}";

  /*
   * The constant DOT
   */
  String SUBFIELD_ACCESS = ".";

  /**
   * Gets prefix object name.
   *
   * @return the prefix object name
   */
  String getPrefixObjectName();

  /**
   * Gets expression start pattern.
   *
   * @return the expression start pattern
   */
  List<String> getExpressionStartPatterns();

  /**
   * Gets expression equal pattern.
   *
   * @return the expression equal pattern
   */
  List<String> getExpressionEqualPatterns();

  /**
   * Gets context element type.
   *
   * @return the context element type
   */
  ContextElementType getContextElementType();

  /**
   * Matches boolean.
   *
   * @param expression the expression
   * @return the boolean
   */
  default boolean matches(String expression) {
    if (getExpressionStartPatterns()
            .stream()
            .filter(pattern -> startsWith(expression, pattern) || startsWith(expression, EXPRESSION_PREFIX + pattern))
            .findFirst()
            .isPresent()
        || getExpressionEqualPatterns()
               .stream()
               .filter(pattern
                   -> StringUtils.equals(expression, pattern)
                       || StringUtils.equals(expression, EXPRESSION_PREFIX + pattern + EXPRESSION_SUFFIX))
               .findFirst()
               .isPresent()) {
      return true;
    }
    return false;
  }

  /**
   * Normalize expression string.
   *
   * @param expression the expression
   * @return the string
   */
  default String normalizeExpression(String expression) {
    if (!matches(expression)) {
      return null;
    }
    expression = getPrefixObjectName() + "." + expression;
    if (!expression.endsWith(EXPRESSION_LIST_SUFFIX)) {
      expression = expression + EXPRESSION_LIST_SUFFIX;
    }
    return expression;
  }
}
