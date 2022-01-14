package software.wings.expression;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.expression.ExpressionEvaluator;
import io.harness.expression.JsonFunctor;
import io.harness.expression.RegexFunctor;
import io.harness.expression.XmlFunctor;
import io.harness.shell.ScriptType;

import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

/**
 * The Class ManagerExpressionEvaluator.
 */
@OwnedBy(CDC)
@Singleton
@Slf4j
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
public class ManagerExpressionEvaluator extends ExpressionEvaluator {
  public ManagerExpressionEvaluator() {
    addFunctor("regex", new RegexFunctor());
    addFunctor("json", new JsonFunctor());
    addFunctor("xml", new XmlFunctor());
    addFunctor("aws", new AwsFunctor());
    addFunctor("shell", new ShellScriptFunctor(ScriptType.BASH));
  }
}
