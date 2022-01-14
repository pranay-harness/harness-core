package io.harness.expression.evaluator;

import io.harness.expression.ExpressionEvaluator;
import io.harness.expression.JsonFunctor;
import io.harness.expression.RegexFunctor;
import io.harness.expression.XmlFunctor;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
public class ExpressionServiceEvaluator extends ExpressionEvaluator {
  public ExpressionServiceEvaluator() {
    addFunctor("regex", new RegexFunctor());
    addFunctor("json", new JsonFunctor());
    addFunctor("xml", new XmlFunctor());
  }
}
