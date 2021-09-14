/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.api;

import static io.harness.annotations.dev.HarnessModule._957_CG_BEANS;
import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.context.ContextElementType;

import software.wings.api.AwsLambdaContextElement.FunctionMeta;
import software.wings.beans.AwsConfig;
import software.wings.sm.ContextElement;
import software.wings.sm.ExecutionContext;

import java.util.Map;
import lombok.Builder;
import lombok.Value;

/**
 * The type Aws lambda context element.
 */
@Value
@Builder
@OwnedBy(CDP)
@TargetModule(_957_CG_BEANS)
public class AwsLambdaFunctionElement implements ContextElement {
  private String uuid;
  private String name;

  private AwsConfig awsConfig;
  private String region;
  private FunctionMeta functionArn;

  @Override
  public ContextElementType getElementType() {
    return ContextElementType.AWS_LAMBDA_FUNCTION;
  }

  @Override
  public Map<String, Object> paramMap(ExecutionContext context) {
    return null;
  }

  @Override
  public ContextElement cloneMin() {
    return this;
  }
}
