package io.harness.text;

import io.harness.text.resolver.ExpressionResolver;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class DummyExpressionResolver implements ExpressionResolver {
  private final List<String> expressions = new ArrayList<>();
  private int index = 0;

  @Override
  public String resolve(String expression) {
    expressions.add(expression);
    index++;
    return String.valueOf(index);
  }
}
