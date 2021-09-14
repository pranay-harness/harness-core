/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.steps.http;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.task.http.HttpStepResponse;
import io.harness.expression.EngineExpressionEvaluator;

@OwnedBy(HarnessTeam.PIPELINE)
public class HttpExpressionEvaluator extends EngineExpressionEvaluator {
  private final HttpStepResponse httpStepResponse;

  public HttpExpressionEvaluator(HttpStepResponse httpStepResponse) {
    super(null);
    this.httpStepResponse = httpStepResponse;
  }

  @Override
  protected void initialize() {
    super.initialize();
    this.addToContext("httpResponseCode", httpStepResponse.getHttpResponseCode());
    this.addToContext("httpResponseBody", httpStepResponse.getHttpResponseBody());
  }
}
