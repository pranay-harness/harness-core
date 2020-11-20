package software.wings.expression;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.google.inject.Singleton;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.task.shell.ScriptType;
import io.harness.expression.ExpressionEvaluator;
import io.harness.expression.JsonFunctor;
import io.harness.expression.RegexFunctor;
import io.harness.expression.XmlFunctor;
import lombok.extern.slf4j.Slf4j;

/**
 * The Class ManagerExpressionEvaluator.
 */
@OwnedBy(CDC)
@Singleton
@Slf4j
public class ManagerExpressionEvaluator extends ExpressionEvaluator {
  public ManagerExpressionEvaluator() {
    addFunctor("regex", new RegexFunctor());
    addFunctor("json", new JsonFunctor());
    addFunctor("xml", new XmlFunctor());
    addFunctor("aws", new AwsFunctor());
    addFunctor("shell", new ShellScriptFunctor(ScriptType.BASH));
  }
}
