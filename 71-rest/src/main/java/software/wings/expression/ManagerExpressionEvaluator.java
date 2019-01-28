package software.wings.expression;

import com.google.inject.Singleton;

import io.harness.expression.ExpressionEvaluator;
import io.harness.expression.JsonFunctor;
import io.harness.expression.RegexFunctor;
import io.harness.expression.XmlFunctor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class ManagerExpressionEvaluator.
 */
@Singleton
public class ManagerExpressionEvaluator extends ExpressionEvaluator {
  private static final Logger logger = LoggerFactory.getLogger(ManagerExpressionEvaluator.class);

  public ManagerExpressionEvaluator() {
    addFunctor("regex", new RegexFunctor());
    addFunctor("json", new JsonFunctor());
    addFunctor("xml", new XmlFunctor());
    addFunctor("aws", new AwsFunctor());
  }
}
