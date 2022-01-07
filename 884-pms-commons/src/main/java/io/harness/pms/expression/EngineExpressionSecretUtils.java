/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.expression;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.expression.EngineExpressionEvaluator;
import io.harness.expression.ExpressionEvaluatorUtils;
import io.harness.expression.ExpressionResolveFunctor;
import io.harness.text.StringReplacer;
import io.harness.text.resolver.ExpressionResolver;

import lombok.experimental.UtilityClass;
import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlExpression;
import org.apache.commons.jexl3.MapContext;
import org.apache.commons.logging.impl.NoOpLog;

@OwnedBy(HarnessTeam.PIPELINE)
@UtilityClass
public class EngineExpressionSecretUtils {
  public Object revertSecrets(Object o) {
    return ExpressionEvaluatorUtils.updateExpressions(o, new SecretRevertResolveFunctor());
  }

  private static class SecretRevertResolveFunctor implements ExpressionResolveFunctor {
    private final StringReplacer replacer;

    private SecretRevertResolveFunctor() {
      JexlEngine engine = new JexlBuilder().logger(new NoOpLog()).create();
      this.replacer = new StringReplacer(new SecretRevertResolver(engine), "${", "}");
    }

    @Override
    public String processString(String expression) {
      return replacer.replace(expression);
    }

    @Override
    public boolean supportsNotExpression() {
      return false;
    }
  }

  private static class SecretRevertResolver implements ExpressionResolver {
    private final JexlEngine engine;
    private final JexlContext ctx;

    private SecretRevertResolver(JexlEngine engine) {
      this.engine = engine;
      this.ctx = new MapContext();
      this.ctx.set("ngSecretManager", new SecretRevertFunctor());
    }

    @Override
    public String resolve(String expression) {
      JexlExpression jexlExpression = engine.createExpression(expression);
      try {
        Object value = jexlExpression.evaluate(ctx);
        return String.valueOf(value);
      } catch (Exception ex) {
        return createExpression(expression);
      }
    }

    private String createExpression(String expression) {
      return String.format("${%s}", expression);
    }
  }

  public static class SecretRevertFunctor {
    public String obtain(String secretIdentifier, int token) {
      return EngineExpressionEvaluator.createExpression(String.format("secrets.getValue(\"%s\")", secretIdentifier));
    }
  }
}
