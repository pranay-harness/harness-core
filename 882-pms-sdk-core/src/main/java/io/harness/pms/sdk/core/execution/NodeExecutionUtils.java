package io.harness.pms.sdk.core.execution;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.expression.ExpressionEvaluatorUtils;
import io.harness.expression.ExpressionResolveFunctor;
import io.harness.expression.ResolveObjectResponse;
import io.harness.pms.contracts.execution.ExecutableResponse;
import io.harness.pms.contracts.execution.NodeExecutionProto;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;
import io.harness.pms.yaml.ParameterDocumentField;
import io.harness.pms.yaml.ParameterDocumentFieldMapper;

import com.google.common.annotations.VisibleForTesting;
import java.util.List;
import java.util.Optional;
import lombok.experimental.UtilityClass;
import org.bson.Document;

@OwnedBy(CDC)
@UtilityClass
public class NodeExecutionUtils {
  public static int retryCount(NodeExecutionProto nodeExecutionProto) {
    if (isRetry(nodeExecutionProto)) {
      return nodeExecutionProto.getRetryIdsList().size();
    }
    return 0;
  }

  private static boolean isRetry(NodeExecutionProto nodeExecutionProto) {
    return !isEmpty(nodeExecutionProto.getRetryIdsList());
  }

  public ExecutableResponse obtainLatestExecutableResponse(NodeExecutionProto proto) {
    List<ExecutableResponse> executableResponses = proto.getExecutableResponsesList();
    if (isEmpty(executableResponses)) {
      return null;
    }
    return executableResponses.get(executableResponses.size() - 1);
  }

  public Document extractStepParameters(String json) {
    if (EmptyPredicate.isEmpty(json)) {
      return null;
    }
    return RecastOrchestrationUtils.toDocumentFromJson(json);
  }

  public Document extractAndProcessStepParameters(String json) {
    if (EmptyPredicate.isEmpty(json)) {
      return null;
    }
    return (Document) resolveObject(RecastOrchestrationUtils.toDocumentFromJson(json));
  }

  @VisibleForTesting
  public Object resolveObject(Object o) {
    if (o == null) {
      return null;
    }
    return ExpressionEvaluatorUtils.updateExpressions(o, new ExtractResolveFunctorImpl());
  }

  public static class ExtractResolveFunctorImpl implements ExpressionResolveFunctor {
    @Override
    public String processString(String expression) {
      return expression;
    }

    @Override
    public ResolveObjectResponse processObject(Object o) {
      Optional<ParameterDocumentField> docFieldOptional = ParameterDocumentFieldMapper.fromParameterFieldDocument(o);
      if (!docFieldOptional.isPresent()) {
        return new ResolveObjectResponse(false, null);
      }

      ParameterDocumentField docField = docFieldOptional.get();
      return new ResolveObjectResponse(true, resolveObject(docField.fetchFinalValue()));
    }
  }
}
