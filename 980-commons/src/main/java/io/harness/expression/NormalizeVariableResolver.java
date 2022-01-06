/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.expression;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import java.util.List;
import lombok.Builder;
import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlException;
import org.apache.commons.jexl3.JexlExpression;
import org.apache.commons.logging.impl.NoOpLog;
import org.apache.commons.text.StrLookup;

@Builder
public class NormalizeVariableResolver extends StrLookup {
  private static JexlEngine engine = new JexlBuilder().logger(new NoOpLog()).create();
  private JexlContext context;
  private List<String> objectPrefixes;

  public static String expand(String variable, JexlContext context, String objectPrefix) {
    int index = variable.indexOf('.');
    String topObjectName = index < 0 ? variable : variable.substring(0, index);

    if (context.has(topObjectName)) {
      return variable;
    }

    if (!context.has(objectPrefix)) {
      return variable;
    }

    String normalized = objectPrefix + "." + variable;

    try {
      JexlExpression jexlExpression = engine.createExpression(normalized);
      if (jexlExpression.evaluate(context) == null) {
        return variable;
      }
    } catch (JexlException exception) {
      return variable;
    }

    return normalized;
  }

  public static String expand(String variable, JexlContext context, List<String> objectPrefixes) {
    if (isNotEmpty(objectPrefixes)) {
      for (String objectPrefix : objectPrefixes) {
        if (isNotEmpty(objectPrefix)) {
          variable = expand(variable, context, objectPrefix);
        }
      }
    }
    return variable;
  }

  @Override
  public String lookup(String variable) {
    return expand(variable, context, objectPrefixes);
  }
}
