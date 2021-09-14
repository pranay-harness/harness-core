/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
