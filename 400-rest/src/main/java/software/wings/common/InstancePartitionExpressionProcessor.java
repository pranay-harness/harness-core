/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.common;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

import io.harness.context.ContextElementType;

import software.wings.sm.ContextElement;
import software.wings.sm.ExecutionContext;

import java.util.List;

public class InstancePartitionExpressionProcessor extends InstanceExpressionProcessor implements PartitionProcessor {
  /**
   * The constant EXPRESSION_PARTITIONS_SUFFIX.
   */
  public static final String EXPRESSION_PARTITIONS_SUFFIX = ".partitions()";
  /**
   * The constant DEFAULT_EXPRESSION_FOR_PARTITION.
   */
  public static final String DEFAULT_EXPRESSION_FOR_PARTITION = "${phases}";

  private static final List<String> EXPRESSION_START_PATTERNS = asList("phases", "phases()", "phases().instances");
  private static final List<String> EXPRESSION_EQUAL_PATTERNS = asList("phases");

  private static final String INSTANCE_PHASE_EXPR_PROCESSOR = "phaseExpressionProcessor";

  private List<String> breakdowns;
  private List<String> percentages;
  private List<String> counts;

  /**
   * Instantiates a new instance expression processor.
   *
   * @param context the context
   */
  public InstancePartitionExpressionProcessor(ExecutionContext context) {
    super(context);
  }

  /**
   * Phases instance partition expression processor.
   *
   * @return the instance partition expression processor
   */
  public InstancePartitionExpressionProcessor phases() {
    return this;
  }

  @Override
  public List<String> getCounts() {
    return counts;
  }

  @Override
  public void setCounts(List<String> counts) {
    this.counts = counts;
  }

  @Override
  public List<String> getPercentages() {
    return percentages;
  }

  @Override
  public void setPercentages(List<String> percentages) {
    this.percentages = percentages;
  }

  @Override
  public List<String> getBreakdowns() {
    return breakdowns;
  }

  @Override
  public void setBreakdowns(List<String> breakdowns) {
    this.breakdowns = breakdowns;
  }

  @Override
  public List<ContextElement> elements() {
    return list().stream().map(instanceElement -> (ContextElement) instanceElement).collect(toList());
  }

  @Override
  public String normalizeExpression(String expression) {
    if (!matches(expression)) {
      return null;
    }
    expression = getPrefixObjectName() + "." + expression;
    if (!expression.endsWith(EXPRESSION_PARTITIONS_SUFFIX)) {
      expression = expression + EXPRESSION_PARTITIONS_SUFFIX;
    }
    return expression;
  }

  @Override
  public List<String> getExpressionEqualPatterns() {
    return EXPRESSION_EQUAL_PATTERNS;
  }

  @Override
  public List<String> getExpressionStartPatterns() {
    return EXPRESSION_START_PATTERNS;
  }

  @Override
  public String getPrefixObjectName() {
    return INSTANCE_PHASE_EXPR_PROCESSOR;
  }

  /**
   * Gets phases.
   *
   * @return the phases
   */
  public InstancePartitionExpressionProcessor getPhases() {
    return this;
  }

  @Override
  public ContextElementType getContextElementType() {
    return ContextElementType.PARTITION;
  }
}
