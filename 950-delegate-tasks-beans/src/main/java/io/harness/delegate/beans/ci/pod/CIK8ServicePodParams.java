package io.harness.delegate.beans.ci.pod;

import static io.harness.expression.Expression.ALLOW_SECRETS;

import io.harness.expression.Expression;
import io.harness.expression.ExpressionReflectionUtils.NestedAnnotationResolver;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

@Data
@Builder
public class CIK8ServicePodParams implements NestedAnnotationResolver {
  @Expression(ALLOW_SECRETS) @NonNull private String serviceName;
  @Expression(ALLOW_SECRETS) @NonNull private Map<String, String> selectorMap;
  @NonNull private List<Integer> ports;
  @Expression(ALLOW_SECRETS) @NonNull private CIK8PodParams<CIK8ContainerParams> cik8PodParams;
}
