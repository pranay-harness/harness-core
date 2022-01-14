package io.harness.delegate.beans.ci.vm.steps;

import io.harness.expression.ExpressionReflectionUtils.NestedAnnotationResolver;

public interface VmStepInfo extends NestedAnnotationResolver {
  enum Type { RUN, PLUGIN, RUN_TEST }
  Type getType();
}
