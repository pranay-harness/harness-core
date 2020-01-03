package software.wings.expression;

import static io.harness.rule.OwnerRule.SRINIVAS;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.delegate.task.shell.ScriptType;
import io.harness.expression.SecretString;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;

public class ShellScriptFunctorTest extends WingsBaseTest {
  private static final String ESCAPE_CHAR = "a'b\"c`d$e~f!g@h#i%j^k&l*m(n)o-p_r{s}t[]|;:u,v.w/x?y";
  public static final String ESCAPED_CHARS = "a\\'b\\\"c\\`d\\$e~f!g@h#i%j^k\\&l*m\\(n\\)o-p_r{s}t[]\\|\\;:u,v.w/x?y";
  public static final String POWERSHELL_ESCAPED_CHARS = "\"a'b`\"c`d$e~f!g@h#i%j^k&l*m(n)o-p_r{s}t[]|;:u,v.w/x?y\"";
  public static final String DON_T = "don't";

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void testEscapifyBash() {
    ShellScriptFunctor shellScriptFunctor = new ShellScriptFunctor(ScriptType.BASH);
    assertThat(shellScriptFunctor.escape(ESCAPE_CHAR)).isEqualTo(ESCAPED_CHARS);
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void testEscapifyPowershell() {
    ShellScriptFunctor shellScriptFunctor = new ShellScriptFunctor(ScriptType.POWERSHELL);
    assertThat(shellScriptFunctor.escape(ESCAPE_CHAR)).isEqualTo(POWERSHELL_ESCAPED_CHARS);
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void testEscapifyQuote() {
    ShellScriptFunctor shellScriptFunctor = new ShellScriptFunctor(ScriptType.BASH);
    final String s = shellScriptFunctor.escape("don't");
    assertThat(s).isEqualTo("don\\'t");
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void testEscapifyQuotePowerShell() {
    ShellScriptFunctor shellScriptFunctor = new ShellScriptFunctor(ScriptType.POWERSHELL);
    final String s = shellScriptFunctor.escape(DON_T);
    assertThat(s).isEqualTo("\"don't\"");
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void testEscapifySecret() {
    ShellScriptFunctor shellScriptFunctor = new ShellScriptFunctor(ScriptType.BASH);
    final String s = shellScriptFunctor.escape(SecretString.builder().value(DON_T).build());
    assertThat(s).isEqualTo("don\\'t");
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void testEscapifyLateBindingSecret() {
    ShellScriptFunctor shellScriptFunctor = new ShellScriptFunctor(ScriptType.BASH);
    final String s = shellScriptFunctor.escape(() -> DON_T);
    assertThat(s).isEqualTo("don\\'t");
  }
}