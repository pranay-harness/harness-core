package io.harness.steps.http;

import io.harness.expression.EngineExpressionEvaluator;

public class HttpExpressionEvaluator extends EngineExpressionEvaluator {
  private final int httpResponseCode;

  public HttpExpressionEvaluator(int httpResponseCode) {
    super(null);
    this.httpResponseCode = httpResponseCode;
  }

  @Override
  protected void initialize() {
    super.initialize();
    this.addToContext("httpResponseCode", httpResponseCode);
  }
}
