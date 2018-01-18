package software.wings.common;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static software.wings.api.AwsLambdaFunctionElement.Builder.anAwsLambdaFunctionElement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.AwsLambdaContextElement;
import software.wings.api.AwsLambdaFunctionElement;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExpressionProcessor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AwsLambdaFunctionProcessor implements ExpressionProcessor {
  private static final Logger logger = LoggerFactory.getLogger(AwsLambdaFunctionProcessor.class);

  /**
   * The Expression start pattern.
   */
  public static final String DEFAULT_EXPRESSION = "${functions}";

  private static final String EXPRESSION_START_PATTERN = "functions()";
  private static final String EXPRESSION_EQUAL_PATTERN = "functions";

  private static final String FUNCTION_EXPR_PROCESSOR = "awsLambdaFunctionProcessor";

  private ExecutionContext context;

  /**
   * Instantiates a new AwsLambdaFunctionProcessor.
   *
   * @param context the context
   */
  public AwsLambdaFunctionProcessor(ExecutionContext context) {
    this.context = context;
  }

  @Override
  public String getPrefixObjectName() {
    return FUNCTION_EXPR_PROCESSOR;
  }

  @Override
  public List<String> getExpressionStartPatterns() {
    return Collections.singletonList(EXPRESSION_START_PATTERN);
  }

  @Override
  public List<String> getExpressionEqualPatterns() {
    return Collections.singletonList(EXPRESSION_EQUAL_PATTERN);
  }

  @Override
  public ContextElementType getContextElementType() {
    return ContextElementType.AWS_LAMBDA_FUNCTION;
  }

  /**
   * Functions.
   *
   * @return the function expression processor
   */
  public AwsLambdaFunctionProcessor getFunctions() {
    return this;
  }

  /**
   * Lists.
   *
   * @return the list
   */
  public List<AwsLambdaFunctionElement> list() {
    AwsLambdaContextElement awsLambdaContextElement =
        context.getContextElement(ContextElementType.PARAM, Constants.AWS_LAMBDA_REQUEST_PARAM);
    if (awsLambdaContextElement == null) {
      logger.error("AwsLambdaContextElement is null in the context");
      return null;
    }

    if (isEmpty(awsLambdaContextElement.getFunctionArns())) {
      logger.error("awsLambdaContextElement.getFunctionArns() is null or empty in the context");
    }
    List<AwsLambdaFunctionElement> awsLambdaFunctionElementList = new ArrayList<>();
    awsLambdaContextElement.getFunctionArns().forEach(functionMeta -> {
      awsLambdaFunctionElementList.add(anAwsLambdaFunctionElement()
                                           .withUuid(functionMeta.getFunctionArn())
                                           .withName(functionMeta.getFunctionName())
                                           .withAwsConfig(awsLambdaContextElement.getAwsConfig())
                                           .withFunctionArn(functionMeta)
                                           .withRegion(awsLambdaContextElement.getRegion())
                                           .build());
    });
    return awsLambdaFunctionElementList;
  }
}
