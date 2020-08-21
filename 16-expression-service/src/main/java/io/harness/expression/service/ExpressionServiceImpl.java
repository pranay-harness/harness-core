package io.harness.expression.service;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static java.lang.String.format;

import com.google.inject.Inject;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.stub.StreamObserver;
import io.harness.expression.evaluator.ExpressionServiceEvaluator;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
public class ExpressionServiceImpl extends ExpressionEvaulatorServiceGrpc.ExpressionEvaulatorServiceImplBase {
  private final ExpressionServiceEvaluator expressionEvaluator;
  private final ObjectMapper objectMapper;

  @Inject
  public ExpressionServiceImpl(ExpressionServiceEvaluator expressionEvaluator) {
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
      logger.error("Expression queries should not be empty");
    }

    for (ExpressionQuery expressionQuery : expressionQueries) {
      try {
        String expression = expressionQuery.getJexl();
        Map<String, Object> context = getContextMap(expressionQuery.getJsonContext());
        Object value = expressionEvaluator.substitute(expression, context);
        responseBuilder.addValues(ExpressionValue.newBuilder()
                                      .setValue(value.toString())
                                      .setJexl(expression)
                                      .setStatusCode(ExpressionValue.EvaluationStatus.SUCCESS)
                                      .build());

      } catch (Exception e) {
        responseBuilder.addValues(ExpressionValue.newBuilder()
                                      .setStatusCode(ExpressionValue.EvaluationStatus.ERROR)
                                      .setErrorMessage(format("Error evaluating expression: %s", e.getMessage()))
                                      .build());
        logger.error("Error evaluation expression", e);
      }
    }
    responseObserver.onNext(responseBuilder.build());
    responseObserver.onCompleted();
  }

  private Map<String, Object> getContextMap(String jsonContext) throws IOException {
    return objectMapper.readValue(jsonContext, new TypeReference<Map<String, Object>>() {});
  }
}
