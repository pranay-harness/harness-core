package harness.callgraph.util;

import static io.harness.rule.OwnerRule.SHIVAKUMAR;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import harness.callgraph.MockitoRule;
import java.security.ProtectionDomain;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.utility.JavaModule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TestRule;
import org.mockito.Mock;

public class SourceClassesMatcherTest {
  @Rule public TestRule mockitoRule = new MockitoRule(this);

  @Mock private TypeDescription typeDescription;

  @Mock private ClassLoader classLoader;

  @Mock private JavaModule module;

  @Mock private ProtectionDomain protectionDomain;

  @Test
  @Owner(developers = SHIVAKUMAR)
  @Category(UnitTests.class)
  public void testMatches() throws Exception {
    SourceClassesMatcher rawMatcher = new SourceClassesMatcher(false);
    assertThat(rawMatcher.matches(typeDescription, classLoader, module, Foo.class, protectionDomain)).isTrue();
  }

  private static class Foo { /* empty */
  }
}
