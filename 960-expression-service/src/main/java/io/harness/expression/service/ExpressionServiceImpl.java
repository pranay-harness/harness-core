/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.expression.service;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import static java.lang.String.format;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.expression.EngineExpressionEvaluator;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.CI)
@Slf4j
public class ExpressionServiceImpl extends ExpressionEvaulatorServiceGrpc.ExpressionEvaulatorServiceImplBase {
  private final EngineExpressionEvaluator expressionEvaluator;
  private final ObjectMapper objectMapper;

  @Inject
  public ExpressionServiceImpl(EngineExpressionEvaluator expressionEvaluator) {
    this.expressionEvaluator = expressionEvaluator;
    this.objectMapper = new ObjectMapper();
  }

  @Override
  public void evaluateExpression(ExpressionRequest request, StreamObserver<ExpressionResponse> responseObserver) {
    ExpressionResponse.Builder responseBuilder = ExpressionResponse.newBuilder();
    List<ExpressionQuery> expressionQueries = request.getQueriesList();

    if (isEmpty(request.getQueriesList())) {
      responseBuilder.addValues(ExpressionValue.newBuilder()
                                    .setStatusCode(ExpressionValue.EvaluationStatus.ERROR)
                                    .setErrorMessage("Expression queries should not be empty")
                                    .build());
      log.error("Expression queries should not be empty");
    }

    for (ExpressionQuery expressionQuery : expressionQueries) {
      try {
        String originalExpr = expressionQuery.getJexl();
        Map<String, Object> context = getContextMap(expressionQuery.getJsonContext());
        String evaluatedExpr = null;
        if (expressionQuery.getIsSkipCondition()) {
          evaluatedExpr = expressionEvaluator.evaluateExpression(originalExpr, context).toString();
        } else {
          evaluatedExpr = expressionEvaluator.renderExpression(originalExpr, context, true);
        }

        responseBuilder.addValues(ExpressionValue.newBuilder()
                                      .setValue(evaluatedExpr)
                                      .setJexl(originalExpr)
                                      .setStatusCode(ExpressionValue.EvaluationStatus.SUCCESS)
                                      .build());

      } catch (Exception e) {
        responseBuilder.addValues(ExpressionValue.newBuilder()
                                      .setStatusCode(ExpressionValue.EvaluationStatus.ERROR)
                                      .setErrorMessage(format("Error evaluating expression: %s", e.getMessage()))
                                      .build());
        log.error("Error evaluation expression", e);
      }
    }
    responseObserver.onNext(responseBuilder.build());
    responseObserver.onCompleted();
  }

  private Map<String, Object> getContextMap(String jsonContext) throws IOException {
    return objectMapper.readValue(jsonContext, new TypeReference<Map<String, Object>>() {});
  }
}
