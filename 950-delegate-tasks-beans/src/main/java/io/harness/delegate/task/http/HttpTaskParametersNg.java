package io.harness.delegate.task.http;

import static io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator.HttpCapabilityDetailsLevel.QUERY;
import static io.harness.expression.Expression.ALLOW_SECRETS;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.expression.Expression;
import io.harness.expression.ExpressionEvaluator;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class HttpTaskParametersNg implements TaskParameters {
  String method;
  String url;
  Map<String, String> requestHeader;
  String body;
  int socketTimeoutMillis;
  boolean useProxy;
  boolean isCertValidationRequired;
}
