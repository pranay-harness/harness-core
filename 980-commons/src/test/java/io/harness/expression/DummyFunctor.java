/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.expression;

import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DummyFunctor implements ExpressionResolveFunctor {
  Map<String, Object> context;

  @Override
  public String processString(String str) {
    return str.replaceAll("original", "updated");
  }

  public Object evaluateExpression(String str) {
    return context != null && context.containsKey(str) ? context.get(str) : str;
  }

  public boolean hasVariables(String str) {
    if ("random".equals(str)) {
      return true;
    }
    if (context == null) {
      return false;
    }

    for (Map.Entry<String, Object> entry : context.entrySet()) {
      if (str.equals(entry.getKey())) {
        return true;
      }
    }
    return false;
  }

  @Override
  public ResolveObjectResponse processObject(Object o) {
    if (!(o instanceof DummyField)) {
      return new ResolveObjectResponse(false, null);
    }

    DummyField field = (DummyField) o;
    field.process(this);
    return new ResolveObjectResponse(true, o);
  }
}
