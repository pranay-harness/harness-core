/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.common;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import static software.wings.api.AwsLambdaContextElement.AWS_LAMBDA_REQUEST_PARAM;

import io.harness.annotations.dev.OwnedBy;
import io.harness.context.ContextElementType;

import software.wings.api.AwsLambdaContextElement;
import software.wings.api.AwsLambdaFunctionElement;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExpressionProcessor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(CDP)
public class AwsLambdaFunctionProcessor implements ExpressionProcessor {
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
        context.getContextElement(ContextElementType.PARAM, AWS_LAMBDA_REQUEST_PARAM);
    if (awsLambdaContextElement == null) {
      log.error("AwsLambdaContextElement is null in the context");
      return null;
    }

    List<AwsLambdaFunctionElement> awsLambdaFunctionElementList = new ArrayList<>();
    if (isEmpty(awsLambdaContextElement.getFunctionArns())) {
      log.error("awsLambdaContextElement.getFunctionArns() is null or empty in the context");
      return awsLambdaFunctionElementList;
    }
    awsLambdaContextElement.getFunctionArns().forEach(functionMeta -> {
      awsLambdaFunctionElementList.add(AwsLambdaFunctionElement.builder()
                                           .uuid(functionMeta.getFunctionArn())
                                           .name(functionMeta.getFunctionName())
                                           .awsConfig(awsLambdaContextElement.getAwsConfig())
                                           .functionArn(functionMeta)
                                           .region(awsLambdaContextElement.getRegion())
                                           .build());
    });
    return awsLambdaFunctionElementList;
  }
}
