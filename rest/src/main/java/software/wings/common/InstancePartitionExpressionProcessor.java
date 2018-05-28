package software.wings.common;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import software.wings.sm.ContextElement;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;

import java.util.List;

/**
 * Created by peeyushaggarwal on 7/1/16.
 */
public class InstancePartitionExpressionProcessor extends InstanceExpressionProcessor implements PartitionProcessor {
  /**
   * The constant DEFAULT_EXPRESSION_FOR_PARTITION.
   */
  public static final String DEFAULT_EXPRESSION_FOR_PARTITION = "${phases}";

  private static final List<String> EXPRESSION_START_PATTERNS = asList("phases", "phases()", "phases().instances");
  private static final List<String> EXPRESSION_EQUAL_PATTERNS = asList("phases");

  private static final String INSTANCE_PHASE_EXPR_PROCESSOR = "phaseExpressionProcessor";

  private String[] breakdowns;
  private String[] percentages;
  private String[] counts;

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

  @SuppressFBWarnings("EI_EXPOSE_REP")
  @Override
  public String[] getCounts() {
    return counts;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  @Override
  public void setCounts(String[] counts) {
    this.counts = counts;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP")
  @Override
  public String[] getPercentages() {
    return percentages;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  @Override
  public void setPercentages(String[] percentages) {
    this.percentages = percentages;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP")
  @Override
  public String[] getBreakdowns() {
    return breakdowns;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  @Override
  public void setBreakdowns(String[] breakdowns) {
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
    if (!expression.endsWith(Constants.EXPRESSION_PARTITIONS_SUFFIX)) {
      expression = expression + Constants.EXPRESSION_PARTITIONS_SUFFIX;
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
